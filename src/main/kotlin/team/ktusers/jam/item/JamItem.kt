package team.ktusers.jam.item

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.minestom.server.event.player.PlayerUseItemEvent

@Serializable
@Polymorphic
sealed interface JamItem {
    fun onUse(event: PlayerUseItemEvent)
}