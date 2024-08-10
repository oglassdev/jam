package team.ktusers.jam.game

import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.bladehunt.blade.dsl.instance.InstanceBuilder
import net.bladehunt.blade.dsl.instance.buildInstance
import net.bladehunt.blade.ext.loadChunks
import net.bladehunt.kotstom.DimensionTypeRegistry
import net.bladehunt.kotstom.InstanceManager
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.dsl.particle
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.InstancedGame
import net.bladehunt.minigamelib.descriptor.GameDescriptor
import net.bladehunt.minigamelib.dsl.element
import net.bladehunt.minigamelib.dsl.gameDescriptor
import net.bladehunt.minigamelib.element.countdown
import net.bladehunt.minigamelib.util.createElementInstanceEventNode
import net.bladehunt.minigamelib.util.store
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import team.ktusers.jam.Config
import team.ktusers.jam.Lobby
import team.ktusers.jam.event.PlayerCleanseBlockEvent
import team.ktusers.jam.event.PlayerPreCleanseBlockEvent
import team.ktusers.jam.item.Clentaminator
import team.ktusers.jam.item.getCustomItemData
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class JamGame : InstancedGame(
    UUID.randomUUID(),
    InstanceBuilder(InstanceManager.createInstanceContainer(DIMENSION)).apply {
        polar {
            fromResource("/world.polar")
        }

        instance.loadChunks(0, 0, 8).thenAccept {
            val batch = AbsoluteBlockBatch()

            instance.chunks.forEach { chunk ->
                for (x in 0..15) {
                    for (y in -64..30) {
                        for (z in 0..15) {
                            val current = chunk.getBlock(x, y, z)
                            val newBlock = JamBlock.mapped[current.namespace()] ?: continue

                            batch.setBlock(
                                x + chunk.chunkX * 16,
                                y + chunk.chunkZ * 16,
                                z,
                                newBlock.withNbt(current.nbt()).withProperties(current.properties())
                            )
                        }
                    }
                }
            }

            batch.apply(instance) {}
        }

        eventNode.listen<PlayerSpawnEvent> {
            it.entity.teleport(Config.game.spawnPos)
        }
    }.instance
) {
    companion object {
        val DIMENSION = DimensionTypeRegistry.register(
            NamespaceID.from("jam", "dimension"),
            DimensionType.builder()
                .hasSkylight(false)
                .effects("minecraft:the_end")
                .ambientLight(13f).build()
        )

        private val REFERENCE = buildInstance {
            polar {
                fromResource("/world.polar")
            }

            instance.loadChunks(0, 0, 8)
        }

        private val TIMES =
            Title.Times.times(
                50.milliseconds.toJavaDuration(),
                1100.milliseconds.toJavaDuration(),
                50.milliseconds.toJavaDuration()
            )
    }

    override val id: NamespaceID = NamespaceID.from("ktusers", "game")

    var Player.blocksCleansed: Int by store { 0 }

    override val descriptor: GameDescriptor = gameDescriptor {
        +element {
            countdown(3, 5, 5, onCountdown = { value ->
                when (value) {
                    in 0..5 -> {
                        showTitle(
                            Title.title(
                                text(value.toString(), NamedTextColor.RED),
                                Component.empty(),
                                TIMES
                            )
                        )
                        playSound(
                            Sound.sound()
                                .type(SoundEvent.BLOCK_DISPENSER_FAIL)
                                .volume(0.7f)
                                .build()
                        )
                    }

                    else -> {
                        if (value % 5 != 0) return@countdown
                        showTitle(
                            Title.title(
                                text(value.toString(), NamedTextColor.GOLD),
                                Component.empty(),
                                TIMES
                            )
                        )
                        playSound(
                            Sound.sound()
                                .type(SoundEvent.BLOCK_DISPENSER_FAIL)
                                .volume(0.5f)
                                .build()
                        )
                    }
                }
            })

            playSound(
                Sound.sound()
                    .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                    .volume(0.6f)
                    .pitch(0.8f)
                    .build()
            )
        }
        +element {
            withTimeoutOrNull(30.seconds) {
                val eventNode = createElementInstanceEventNode()

                players.forEach {
                    it.inventory.addItemStack(Clentaminator.ClentaminatorItem)
                }

                eventNode.listen<PlayerUseItemEvent> { event ->
                    val usedItem = event.itemStack.getCustomItemData() ?: return@listen

                    usedItem.onUse(event)
                }

                suspendCancellableCoroutine {
                    var currentAmount = 0
                    val dnr = setOf(
                        JamColor.Black.block.namespace(),
                        JamColor.Black.stairs.namespace(),
                        JamColor.Black.slab.namespace()
                    )

                    eventNode.listen<PlayerPreCleanseBlockEvent> { event ->
                        if (!dnr.contains(event.block.namespace())) event.isCancelled = true
                    }

                    eventNode.listen<PlayerCleanseBlockEvent> { event ->
                        sendMessage(text("You've cleaned $currentAmount"))
                        instance.setBlock(event.blockPosition, REFERENCE.getBlock(event.blockPosition))

                        event.player.playSound(
                            Sound.sound()
                                .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                                .volume(0.6f)
                                .pitch(0.8f)
                                .build()
                        )
                        event.player.blocksCleansed++
                        players.forEach { player ->
                            player.sendPackets(
                                particle {
                                    position = event.blockPosition
                                    particle = Particle.POOF
                                },
                                particle {
                                    position = event.blockPosition.add(0, 0, 1)
                                    particle = Particle.POOF
                                },
                                particle {
                                    position = event.blockPosition.add(0, 1, 0)
                                    particle = Particle.POOF
                                },
                                particle {
                                    position = event.blockPosition.add(0, 1, 1)
                                    particle = Particle.POOF
                                },
                                particle {
                                    position = event.blockPosition.add(1, 0, 0)
                                    particle = Particle.POOF
                                },
                                particle {
                                    position = event.blockPosition.add(1, 0, 1)
                                    particle = Particle.POOF
                                },
                                particle {
                                    position = event.blockPosition.add(1, 1, 0)
                                    particle = Particle.POOF
                                },
                                particle {
                                    position = event.blockPosition.add(1, 1, 1)
                                    particle = Particle.POOF
                                }
                            )
                        }

                        currentAmount++
                        if (currentAmount >= 15) it.resume(Unit)
                    }
                }

            }
        }
        +element {
            sendMessage(
                text("Game Overview", TextDecoration.BOLD, color = NamedTextColor.DARK_GREEN) + newline() +
                        text(
                            "Most cured: ",
                            TextDecoration.BOLD,
                            color = NamedTextColor.DARK_GREEN
                        ) + players.maxBy { it.blocksCleansed }.name
            )
            players.forEach {
                it.inventory.clear()
            }
            delay(10000)
            val futures = arrayListOf<CompletableFuture<*>>()
            players.forEach {
                futures.add(it.setInstance(Lobby))
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            InstanceManager.unregisterInstance(instance)
        }
    }

    override fun Player.sendToFallback() {
        TODO("Not yet implemented")
    }
}