// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.gatekeeper

import arc.Core
import arc.func.Cons2
import arc.struct.ObjectMap
import arc.util.Strings
import com.xpdustry.foundation.collection.MindustryCollections
import com.xpdustry.foundation.player.MUUID
import com.xpdustry.foundation.plugin.PluginListener
import com.xpdustry.foundation.util.Priority
import com.xpdustry.nucleus.config.ConfigManager
import com.xpdustry.nucleus.config.ConfigPropertyKey
import com.xpdustry.nucleus.network.InetAddressInfoProvider
import com.xpdustry.nucleus.network.InetAddressWhitelist
import com.xpdustry.nucleus.text.BadWordCategory
import com.xpdustry.nucleus.text.BadWordFinder
import java.net.InetAddress
import java.util.regex.Pattern
import kotlin.enums.enumEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mindustry.Vars
import mindustry.net.Net
import mindustry.net.NetConnection
import mindustry.net.Packets.ConnectPacket

class GatekeeperController(
    private val pipeline: GatekeeperPipeline,
    private val configManager: ConfigManager,
    private val badWords: BadWordFinder,
    private val addressInfoProvider: InetAddressInfoProvider,
    private val addressWhitelist: InetAddressWhitelist,
    private val scope: CoroutineScope,
) : PluginListener {
    override fun onInit() {
        val previous = accessPacketHandlers()[ConnectPacket::class.java]!!

        Vars.net.handleServer(ConnectPacket::class.java) { connection, packet ->
            if (connection.kicked) {
                return@handleServer
            }
            if (!MUUID.isUuid(packet.uuid) || !MUUID.isUsid(packet.usid)) {
                connection.kick("Invalid UUID or USID", 5000L)
                return@handleServer
            }
            val address = InetAddress.ofLiteral(connection.address)
            val muuid = MUUID.of(packet.uuid, packet.usid)

            scope.launch {
                val context = GatekeeperContext(Strings.stripColors(packet.name), muuid, address)
                when (val decision = this@GatekeeperController.pipeline.pump(context)) {
                    is GatekeeperDecision.Allow -> Core.app.post { previous.get(connection, packet) }
                    is GatekeeperDecision.Kick ->
                        connection.kick(decision.reason, decision.duration.inWholeMilliseconds)
                }
            }
        }

        this.pipeline.register("cracked-client-name", Priority.HIGH) { context ->
            if (!CRACKED_CLIENT_USERNAMES.contains(context.name.lowercase())) {
                GatekeeperDecision.Allow
            } else {
                GatekeeperDecision.Kick(
                    """
                    [green]Mindustry is a free and open source game.
                    [white]It is available on [royal]https://anuke.itch.io/mindustry[].
                    [red]Please, get a legit copy of the game.
                    """
                        .trimIndent()
                )
            }
        }

        this.pipeline.register("link-in-name", Priority.HIGH) { context ->
            if (LINK_PATTERN.matcher(context.name.lowercase()).find()) {
                GatekeeperDecision.Kick("Your name cannot contain a link.")
            } else {
                GatekeeperDecision.Allow
            }
        }

        this.pipeline.register("bad-word-name", Priority.HIGH) { context ->
            val words = this.badWords.findBadWords(context.name, enumEntries<BadWordCategory>())
            if (words.isEmpty()) {
                return@register GatekeeperDecision.Allow
            } else {
                GatekeeperDecision.Kick("Your name contains prohibited words, $words. Please change it.")
            }
        }

        this.pipeline.register("safe-ip", Priority.LOW) { context ->
            if (this.addressWhitelist.contains(context.address)) {
                return@register GatekeeperDecision.Allow
            }
            val result = this.addressInfoProvider.get(context.address)
            if (result != null) {
                if (result.safe) {
                    return@register GatekeeperDecision.Allow
                } else {
                    return@register GatekeeperDecision.Kick(
                        """
                        [red]VPN detected.[]
                        [lightgray]If you think this is a false positive or using a VPN is necessary to you,
                        join our discord server at [accent]%s[].
                        Then ask for an IP unblock in the [accent]#appeals[] channel.
                        [red]Warning: During the process, only share you IP address to an admin [orange](%s).[].[]
                        """
                            .trimIndent()
                            .format(
                                this.configManager.get(ConfigPropertyKey.SERVER_DISCORD),
                                context.address.hostAddress,
                            )
                    )
                }
            } else {
                return@register when (this.configManager.get(ConfigPropertyKey.GATEKEEPER_FAILURE_POLICY)) {
                    GatekeeperFailurePolicy.ALLOW_ALL -> GatekeeperDecision.Allow
                    GatekeeperFailurePolicy.ALLOW_KNOWN_PLAYERS -> {
                        // TODO Implement using the MindustryUserRepository
                        GatekeeperDecision.Allow
                    }
                }
            }
        }
    }

    companion object {
        private val LINK_PATTERN: Pattern = Pattern.compile("(https?://|discord\\.gg)")
        private val CRACKED_CLIENT_USERNAMES =
            setOf(
                "valve",
                "tuttop",
                "codex",
                "igggames",
                "igg-games.com",
                "igruhaorg",
                "freetp.org",
                "goldberg",
                "rog",
            )

        @Suppress("UNCHECKED_CAST")
        private fun accessPacketHandlers(): MutableMap<Class<*>, Cons2<NetConnection, Any>> {
            try {
                val serverListenersField = Net::class.java.getDeclaredField("serverListeners")
                serverListenersField.isAccessible = true
                return MindustryCollections.asMap(
                    serverListenersField.get(Vars.net) as ObjectMap<Class<*>, Cons2<NetConnection, Any>>
                )
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException("Failed to access Net#serverListeners", e)
            }
        }
    }
}
