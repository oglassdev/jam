package team.ktusers.jam.item

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.dsl.item.item
import net.bladehunt.kotstom.dsl.item.itemName
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.kotstom.extension.asVec
import net.bladehunt.kotstom.extension.roundToBlock
import net.bladehunt.minigamelib.ext.game
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import team.ktusers.jam.AdventureNbt
import team.ktusers.jam.event.PlayerCleanseBlockEvent
import team.ktusers.jam.event.PlayerPreCleanseBlockEvent
import team.ktusers.jam.game.JamGame
import team.ktusers.jam.generated.Palette

@Serializable
@SerialName("clentaminator")
data class Clentaminator(
    val selectedColor: Int = Palette.RED.red
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

        val point = event.player.getTargetBlockPosition(10)?.asVec()?.roundToBlock() ?: return
        val block = event.instance.getBlock(point).takeIf { !it.isAir } ?: return
        val preCleanse = PlayerPreCleanseBlockEvent(game, event.player, block, point)
        GlobalEventHandler.call(preCleanse)
        if (!preCleanse.isCancelled) GlobalEventHandler.call(PlayerCleanseBlockEvent(game, event.player, block, point))
    }
}