package team.ktusers.jam.command

import net.bladehunt.kotstom.dsl.kommand.buildSyntax
import net.bladehunt.kotstom.dsl.kommand.kommand
import team.ktusers.jam.Lobby

val LobbyCommand = kommand {
    name = "lobby"

    buildSyntax {
        onlyPlayers()

        executor {
            if (player.instance == Lobby) return@executor
            player.setInstance(Lobby)
        }
    }
}
