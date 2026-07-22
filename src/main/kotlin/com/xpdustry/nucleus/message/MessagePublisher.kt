// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.message

import com.google.gson.Gson
import com.xpdustry.foundation.plugin.PluginListener
import com.xpdustry.nucleus.config.ConfigManager
import com.xpdustry.nucleus.config.ConfigPropertyKey
import com.xpdustry.nucleus.database.PostgresDatabase
import java.util.*
import java.util.concurrent.*
import kotlin.Exception
import kotlin.String
import kotlin.reflect.jvm.jvmName
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MessagePublisher(
    private val configManager: ConfigManager,
    private val database: PostgresDatabase,
) : PluginListener {
    private val gson = Gson()
    private val subscribers: MutableMap<String, MutableList<MessageSubscriber<*>>> =
        ConcurrentHashMap<String, MutableList<MessageSubscriber<*>>>()

    private lateinit var polling: Job

    fun <M : Message> subscribe(type: Class<M>, subscriber: MessageSubscriber<M>) {
        this.subscribers.computeIfAbsent(type.name) { CopyOnWriteArrayList() }.add(subscriber)
    }

    fun <M : Message> publish(message: M) {
        val payload = StringBuilder()
        try {
            payload.append(message::class.jvmName)
            payload.append('|')
            payload.append(this.configManager.get(ConfigPropertyKey.SERVER_NAME))
            payload.append('|')
            payload.append(this.gson.toJson(message))
        } catch (e: Exception) {
            log.error("Failed to serialize a message of type {}", message.javaClass.name, e)
            return
        }
        if (payload.length > 2000) {
            log.error(
                "Failed to serialize message of type {}, payload is too large, got {}",
                message.javaClass.name,
                payload.length,
            )
            return
        }
        runBlocking {
            this@MessagePublisher.database.transaction {
                "SELECT pg_notify(?, ?)"
                    .asPreparedStatement()
                    .push(CHANNEL_NAME)
                    .push(payload.toString())
                    .executeSingleSelect { true }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun dispatch(parameter: String) {
        val parts = parameter.split('|', limit = 3)
        if (parts.size != 3) {
            return
        }

        val payloadType = parts[0]
        val sender = parts[1]
        val payload = parts[2]

        val subscribers = this.subscribers[payloadType] ?: return

        val message: Message
        try {
            message = this.gson.fromJson(payload, Class.forName(payloadType)) as Message
        } catch (_: ClassNotFoundException) {
            return
        } catch (e: Exception) {
            log.error("Failed to deserialize message of type {}", payloadType, e)
            return
        }

        for (subscriber in subscribers) {
            try {
                (subscriber as MessageSubscriber<Message>).onMessage(sender, message)
            } catch (e: Exception) {
                log.error("{} failed to handle message {} from {}", subscriber, message, sender)
            }
        }
    }

    override fun onInit() {
        this.polling =
            this.database.listen(CHANNEL_NAME) { payload ->
                this.dispatch(payload)
            }
    }

    override fun onExit() {
        this.polling.cancel()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MessagePublisher::class.java)
        private const val CHANNEL_NAME = "nucleus_message_v1"
    }
}
