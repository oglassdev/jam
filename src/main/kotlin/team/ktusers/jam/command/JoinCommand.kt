package team.ktusers.jam.command

import kotlinx.coroutines.Dispatchers
import net.bladehunt.kotstom.dsl.kommand.buildSyntax
import net.bladehunt.kotstom.dsl.kommand.kommand
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.GameManager
import net.kyori.adventure.text.format.NamedTextColor.RED
import team.ktusers.jam.game.JamGame

val JoinCommand = kommand {
    name = "join"

    buildSyntax {
        onlyPlayers()

        executorAsync(Dispatchers.Default) {
            val game = GameManager.getOrCreateFirstJoinableGame { JamGame() }
            if (game.players.contains(player)) {
                player.sendMessage(text("You are already in this game!", RED))
                return@executorAsync
            }
            game.addPlayer(player)
        }
    }
}
