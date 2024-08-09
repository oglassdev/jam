package team.ktusers.jam.command

import kotlinx.coroutines.Dispatchers
import net.bladehunt.kotstom.dsl.kommand.buildSyntax
import net.bladehunt.kotstom.dsl.kommand.kommand
import net.minestom.server.coordinate.Pos
import team.ktusers.jam.cutscene.Cutscene
import team.ktusers.jam.cutscene.CutscenePosition

val CutsceneCommand = kommand {
    name = "cutscene"

    buildSyntax {
        onlyPlayers()

        executorAsync(Dispatchers.Default) {
            val cutscene = Cutscene(
                player.instance, true, listOf(
                    CutscenePosition(player.position.withY { it + player.eyeHeight }, 0),
                    CutscenePosition(Pos(8.5, -30.0, 113.5, 150f, 40f), 50),
                    CutscenePosition(Pos(-4.5, -30.0, 110.5, -150f, 40f), 50),
                    CutscenePosition(player.position.withY { it + player.eyeHeight }, 50)
                )
            )
            cutscene.addViewer(player)
            cutscene.start()
            cutscene.removeViewer(player)
        }
    }
}
