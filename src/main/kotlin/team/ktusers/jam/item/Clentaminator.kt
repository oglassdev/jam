package team.ktusers.jam.item

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.ext.game
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import team.ktusers.jam.AdventureNbt
import team.ktusers.jam.game.JamColor
import team.ktusers.jam.game.JamGame

@Serializable
@SerialName("clentaminator")
data class Clentaminator(
    val selectedColor: JamColor = JamColor.Red
) : JamItem {
    companion object {
        val ClentaminatorItem = item(Material.BREEZE_ROD) {
            itemName = text("Clentaminator", NamedTextColor.RED)

            this[ItemTag] = AdventureNbt.encodeToCompound<JamItem>(
                Clentaminator()
            )
        }
    }

    override fun onUse(event: PlayerUseItemEvent) {
        val game = event.player.game as? JamGame? ?: return

        CoroutineScope(Dispatchers.Default).launch {
            game.cureChannel.send(Unit)
        }
        event.player.sendMessage("You used the clentaminator!!")
    }
}