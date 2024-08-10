package team.ktusers.jam.command

import net.bladehunt.kotstom.dsl.kommand.buildSyntax
import net.bladehunt.kotstom.dsl.kommand.kommand
import net.bladehunt.kotstom.dsl.scheduleTask
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.ext.game
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor.*
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.timer.TaskSchedule
import team.ktusers.jam.Config
import team.ktusers.jam.Lobby

private val LeaveTag = Tag.Boolean("force_send_lobby")

val LobbyCommand = kommand {
    name = "lobby"

    buildSyntax {
        onlyPlayers()

        executor {
            if (player.instance == Lobby) {
                player.sendMessage(Component.newline() + text("Teleporting to spawn...", DARK_GRAY))
                player.playSound(
                    Sound.sound()
                        .type(SoundEvent.ENTITY_ENDERMAN_TELEPORT)
                        .build()
                )
                player.teleport(Config.lobby.spawnPos)
                return@executor
            }

            if (player.game != null) {
                if (player.hasTag(LeaveTag)) {
                    player.sendMessage(Component.newline() + text("Sending to lobby...", DARK_GRAY))
                    player.setInstance(Lobby)
                    player.removeTag(LeaveTag)

                    return@executor
                }

                player.sendMessage(
                    Component.newline() +
                            text("Are you sure you want to leave?", RED) + Component.newline() +
                            text("ʀᴜɴ ᴛʜᴇ ᴄᴏᴍᴍᴀɴᴅ ᴀɢᴀɪɴ ᴛᴏ ᴄᴏɴꜰɪʀᴍ.", GRAY)
                )
                player.setTag(LeaveTag, true)
                player.scheduler().scheduleTask(delay = TaskSchedule.seconds(6), repeat = TaskSchedule.stop()) {
                    player.removeTag(LeaveTag)
                }
                return@executor
            }

            player.setInstance(Lobby)
            player.sendMessage(Component.newline() + text("Sending to lobby...", DARK_GRAY))
        }
    }
}
