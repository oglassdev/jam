package team.ktusers.jam.command

import kotlinx.coroutines.Dispatchers
import net.bladehunt.kotstom.dsl.kommand.buildSyntax
import net.bladehunt.kotstom.dsl.kommand.kommand
import net.bladehunt.kotstom.extension.adventure.asComponent
import net.bladehunt.kotstom.extension.adventure.asMini
import net.minestom.server.coordinate.Pos
import team.ktusers.jam.cutscene.Cutscene
import team.ktusers.jam.cutscene.CutscenePosition
import team.ktusers.jam.cutscene.CutsceneText
import java.time.Duration

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
                ),
                listOf(
                    CutsceneText("Scientists were experimenting with a form of dark matter!", Duration.ofMillis(3634)),
                    CutsceneText("when suddenly the experiment went wrong!", Duration.ofMillis(5991 - 3634)),
                    CutsceneText("There was a LEAK!", Duration.ofMillis(7233 - 5991)),
                    CutsceneText("The dark matter started to spread!", Duration.ofMillis(8970 - 7233)),
                    CutsceneText("And only you, can stop it!", Duration.ofMillis(10671 - 8970))
                )
            )
            cutscene.addViewer(player)
            cutscene.start()
            cutscene.removeViewer(player)
        }
    }
}
