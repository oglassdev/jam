package team.ktusers.jam.game

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.bladehunt.blade.dsl.instance.InstanceBuilder
import net.bladehunt.blade.dsl.instance.buildInstance
import net.bladehunt.blade.ext.loadChunks
import net.bladehunt.blade.ext.logger
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
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.network.packet.server.play.BlockChangePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import team.ktusers.jam.Config
import team.ktusers.jam.Lobby
import team.ktusers.jam.cutscene.Cutscene
import team.ktusers.jam.cutscene.CutscenePosition
import team.ktusers.jam.cutscene.CutsceneText
import team.ktusers.jam.event.PlayerCleanseBlockEvent
import team.ktusers.jam.event.PlayerPreCleanseBlockEvent
import team.ktusers.jam.generated.BlockColor
import team.ktusers.jam.item.ColorSelector
import team.ktusers.jam.item.getCustomItemData
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class JamGame : InstancedGame(
    UUID.randomUUID(),
    BLACK_REFERENCE.copy().also { instance ->
        InstanceManager.registerInstance(instance)

        instance.eventNode().listen<PlayerSpawnEvent> {
            it.entity.teleport(Config.game.spawnPos)
        }
    }
) {
    companion object {
        private val logger = logger<JamGame>()

        val DIMENSION = DimensionTypeRegistry.register(
            NamespaceID.from("jam", "dimension"),
            DimensionType.builder()
                .hasSkylight(false)
                .effects("minecraft:the_end")
                .ambientLight(13f).build()
        )

        private val POINT_COLORS = hashMapOf<String, MutableList<BlockVec>>()

        private val REFERENCE = buildInstance {
            polar {
                fromResource("/world.polar")
            }

            instance.loadChunks(0, 0, 8)
        }

        private val BLACK_REFERENCE = InstanceBuilder(InstanceManager.createInstanceContainer(DIMENSION)).apply {
            polar {
                fromResource("/world.polar")
            }

            instance.loadChunks(0, 0, 8).thenAccept {
                val batch = AbsoluteBlockBatch()

                instance.chunks.forEach { chunk ->
                    for (x in 0..15) {
                        for (y in -64..30) {
                            for (z in 0..15) {
                                try {
                                    val current = chunk.getBlock(x, y, z)
                                    if (current.isAir) continue
                                    val block = current.namespace().path()
                                    val newBlock =
                                        if (block.contains("stairs", ignoreCase = true)) JamBlock.BLACK_STAIRS
                                        else if (block.contains("slab", ignoreCase = true)) JamBlock.BLACK_SLAB
                                        else if (block.contains("wall", ignoreCase = true)) JamBlock.BLACK_WALL
                                        else if (!current.properties().contains("waterlogged")) JamBlock.BLACK
                                        else continue

                                    val point = BlockVec(
                                        x + chunk.chunkX * 16,
                                        y,
                                        z + chunk.chunkZ * 16
                                    )
                                    val color = getPaletteColorAsString(BlockColor.getColor(current))
                                    POINT_COLORS.getOrPut(color) { arrayListOf() }.add(point)

                                    batch.setBlock(
                                        point,
                                        newBlock.withNbt(current.nbt()).withProperties(current.properties())
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                batch.apply(instance) {
                    logger.info("Applied batch")
                }
            }
        }.instance

        private val TIMES =
            Title.Times.times(
                50.milliseconds.toJavaDuration(),
                1100.milliseconds.toJavaDuration(),
                50.milliseconds.toJavaDuration()
            )
    }

    override val id: NamespaceID = NamespaceID.from("ktusers", "game")

    var Player.blocksCleansed: Int by store { 0 }

    var Player.currentColor: String by store { "RED" }

    private val _timeElapsed = MutableStateFlow(0)
    val timeElapsed get() = _timeElapsed.asStateFlow()

    val teamInventory = TeamInventory()

    override val descriptor: GameDescriptor = gameDescriptor {
        +element {
            countdown(1, 5, 5, onCountdown = { value ->
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
            val origin = Config.game.spawnPos.withY { it + 2.0 }
            val cutscene = Cutscene(
                instance, true, listOf(
                    CutscenePosition(origin, 0),
                    CutscenePosition(origin.withZ { it - 10.0 }.withLookAt(Config.game.spawnPos), 50),
                    CutscenePosition(origin.withX { it + 10.0 }.withLookAt(Config.game.spawnPos), 50),
                    CutscenePosition(origin, 40)
                ),
                listOf(
                    CutsceneText("Scientists were experimenting with a form of dark matter!", Duration.ofMillis(3634)),
                    CutsceneText("when suddenly the experiment went wrong!", Duration.ofMillis(5991 - 3634)),
                    CutsceneText("There was a LEAK!", Duration.ofMillis(7233 - 5991)),
                    CutsceneText("The dark matter started to spread!", Duration.ofMillis(8970 - 7233)),
                    CutsceneText("And only you, can stop it!", Duration.ofMillis(10671 - 8970))
                )
            )
            players.forEach(cutscene::addViewer)
            cutscene.start()
            players.forEach(cutscene::removeViewer)
        }
        +element {
            withTimeoutOrNull(30.seconds) {
                val eventNode = createElementInstanceEventNode()

                players.forEach {
                    it.inventory.setItemStack(8, ColorSelector().createItemStack())
                }

                eventNode.listen<PlayerBlockInteractEvent> { event ->
                    val usedItem = event.player.getItemInHand(Player.Hand.MAIN).getCustomItemData() ?: return@listen

                    usedItem.onBlockInteract(event)

                    event.isBlockingItemUse = true
                }

                eventNode.listen<PlayerUseItemEvent> { event ->
                    val usedItem = event.itemStack.getCustomItemData() ?: return@listen

                    usedItem.onUse(event)
                }

                suspendCancellableCoroutine {
                    var currentAmount = 0
                    val dnr = setOf(
                        JamBlock.BLACK.namespace(),
                        JamBlock.BLACK_STAIRS.namespace(),
                        JamBlock.BLACK_SLAB.namespace(),
                        JamBlock.BLACK_WALL.namespace(),
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

    fun updateColor(player: Player, from: String, to: String) {
        POINT_COLORS[from]?.let { previous ->
            player.sendPackets(
                previous.map {
                    BlockChangePacket(it, BLACK_REFERENCE.getBlock(it))
                }
            )
        }
        val points = POINT_COLORS[to] ?: return
        player.sendPackets(
            points.map {
                BlockChangePacket(it, REFERENCE.getBlock(it))
            }
        )
        player.currentColor = to
    }

    override fun Player.sendToFallback() {
        TODO("Not yet implemented")
    }
}