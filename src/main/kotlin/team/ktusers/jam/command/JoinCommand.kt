package team.ktusers.jam.command

import kotlinx.coroutines.Dispatchers
import net.bladehunt.kotstom.dsl.kommand.buildSyntax
import net.bladehunt.kotstom.dsl.kommand.kommand
import net.bladehunt.minigamelib.GameManager
import team.ktusers.jam.game.JamGame

val JoinCommand = kommand {
    name = "join"

    buildSyntax {
        onlyPlayers()

        executorAsync(Dispatchers.Default) {
            val game = GameManager.getOrCreateFirstJoinableGame { JamGame() }
            game.addPlayer(player)
        }
    }
}
