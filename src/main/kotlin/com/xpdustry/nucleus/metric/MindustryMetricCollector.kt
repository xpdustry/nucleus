// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.metric

import arc.Core
import com.xpdustry.foundation.annotation.EventHandler
import com.xpdustry.foundation.plugin.PluginListener
import java.util.concurrent.atomic.LongAdder
import mindustry.Vars
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Groups

class MindustryMetricCollector : MetricCollector, PluginListener {
    private val joinCounter = LongAdder()
    private val quitCounter = LongAdder()
    private val chatMessageCounter = LongAdder()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoin) {
        this.joinCounter.increment()
    }

    @EventHandler
    fun onPlayerLeave(event: PlayerLeave) {
        this.quitCounter.increment()
    }

    @EventHandler
    fun onPlayerChatMessage(event: PlayerChatEvent) {
        this.chatMessageCounter.increment()
    }

    override fun flush(sink: MetricSink) {
        sink.sample("mindustry_player_joins_total", MetricType.COUNTER, this.joinCounter.sum())
        sink.sample("mindustry_player_quits_total", MetricType.COUNTER, this.quitCounter.sum())
        sink.sample("mindustry_chat_messages_total", MetricType.COUNTER, this.chatMessageCounter.sum())

        sink.sample("mindustry_players_count", MetricType.GAUGE, Groups.player.size())

        if (Vars.state != null) {
            sink.sample("mindustry_game_waves", MetricType.GAUGE, Vars.state.wave)
            for (team in Team.all) {
                if (!team.active()) {
                    continue
                }
                val labels = mapOf("team" to team.name)
                sink.sample("mindustry_buildings_count", MetricType.GAUGE, team.data().buildings.size, labels)
                sink.sample("mindustry_units_count", MetricType.GAUGE, team.data().units.size, labels)
                sink.sample("mindustry_team_players_count", MetricType.GAUGE, team.data().players.size, labels)
            }
        }

        if (Core.graphics != null) {
            sink.sample("mindustry_server_tps", MetricType.GAUGE, Core.graphics.framesPerSecond)
        }
    }
}
