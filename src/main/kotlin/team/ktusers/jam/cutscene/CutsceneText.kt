package team.ktusers.jam.cutscene

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import java.time.Duration

data class CutsceneText(val text: Component, val duration: Duration) {

    constructor(text: String, duration: Duration) : this(Component.text(text), duration)

    fun show(player: Player) {
        player.sendActionBar(text)
    }

    suspend fun await() {
        delay(duration.toMillis())
    }
}
