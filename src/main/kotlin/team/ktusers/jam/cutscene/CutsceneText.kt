package team.ktusers.jam.cutscene

import net.bladehunt.kotstom.extension.await
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.timer.TaskSchedule
import java.time.Duration

data class CutsceneText(val text: Component, val duration: Duration) {

    constructor(text: String, duration: Duration) : this(Component.text(text), duration)

    suspend fun show(player: Player) {
        player.sendActionBar(text)
        player.scheduler().await(TaskSchedule.duration(duration))
    }
}
