package team.ktusers.jam.item

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack

@Serializable
@Polymorphic
sealed interface JamItem {
    fun createItemStack(): ItemStack

    fun onUse(event: PlayerUseItemEvent)

    fun onBlockInteract(event: PlayerBlockInteractEvent)
}