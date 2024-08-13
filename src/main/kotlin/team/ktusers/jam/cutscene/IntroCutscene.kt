package team.ktusers.jam.cutscene

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import net.bladehunt.blade.dsl.instance.InstanceBuilder
import net.bladehunt.blade.ext.loadChunks
import net.bladehunt.kotstom.InstanceManager
import net.bladehunt.kotstom.dsl.builder
import net.bladehunt.kotstom.extension.await
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.Instance
import net.minestom.server.timer.TaskSchedule
import team.ktusers.jam.Lobby
import java.time.Duration

val CUTSCENE_REFERENCE = InstanceBuilder(InstanceManager.createInstanceContainer()).apply {
    polar {
        fromPath("./cutscene.polar")
    }

    instance.loadChunks(-9, -8, 10).join()
    instance.loadChunks(12, -19, 16).join()
}.instance

fun cutsceneInstance(): Instance = CUTSCENE_REFERENCE.apply {
    InstanceManager.registerInstance(this)

    eventNode().builder<PlayerSpawnEvent> {
        asyncHandler(Dispatchers.Default) { event ->
            event.player.await(TaskSchedule.nextTick())
            intro(event.player, event.instance)
            event.player.setInstance(Lobby).await()

            InstanceManager.unregisterInstance(this@apply)
        }
    }
}

suspend fun intro(player: Player, instance: Instance) {
    val lab = Cutscene(
        instance, false, listOf(
            CutscenePosition(Pos(-126.5, -42.5, -194.5, 100f, 10f), 0),
            CutscenePosition(Pos(-120.5, -42.5, -193.5, 95f, 4f), 40),
            CutscenePosition(Pos(-101.5, -40.5, -201.5, 70f, 4f), 0),
            CutscenePosition(Pos(-93.5, -34.5, -187.5, 117f, 10f), 40)
        ),
        listOf(
            CutsceneText("Scientists were experimenting with a form of dark matter!", Duration.ofMillis(3634)),
            CutsceneText("when suddenly the experiment went wrong!", Duration.ofMillis(5991 - 3634)),
        )
    )

    lab.addViewer(player)
    player.playSound(
        Sound.sound(
            Key.key("jam:start_cutscene"),
            Sound.Source.VOICE,
            1f, 1f
        ), lab.camera
    )
    lab.start()

    player.teleport(Pos(-15.5, -17.5, -154.5, 90f, 15f)).await()

    val outsideLab = Cutscene(
        instance, false, listOf(
            CutscenePosition(Pos(-15.5, -17.5, -154.5, 90f, 15f), 0),
            CutscenePosition(Pos(-2.5, 21.5, -153.5, 85f, 44f), 40),
        ),
        listOf(
            CutsceneText("There was a LEAK!", Duration.ofMillis(7233 - 5991)),
            CutsceneText("The dark matter started to spread!", Duration.ofMillis(8970 - 7233)),
            CutsceneText("And only you, can stop it!", Duration.ofMillis(10671 - 8970))
        )
    )

    outsideLab.addViewer(player)
    outsideLab.start()

    player.teleport(Pos(92.5, -2.5, -214.5, -140f, 50f)).await()

    val village = Cutscene(
        instance, false, listOf(
            CutscenePosition(Pos(92.5, -2.5, -214.5, -140f, 50f), 0),
            CutscenePosition(Pos(160.0, 16.5, -300.5, -150f, 60f), 40),
        ),
        listOf(
            CutsceneText("There was a LEAK!", Duration.ofMillis(7233 - 5991)),
            CutsceneText("The dark matter started to spread!", Duration.ofMillis(8970 - 7233)),
            CutsceneText("And only you, can stop it!", Duration.ofMillis(10671 - 8970))
        )
    )

    village.addViewer(player)
    village.start()
}