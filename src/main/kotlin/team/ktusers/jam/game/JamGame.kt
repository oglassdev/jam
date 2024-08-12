package team.ktusers.jam.game

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
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
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
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
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.play.BlockChangePacket
import net.minestom.server.particle.Particle
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import team.ktusers.jam.Config
import team.ktusers.jam.Lobby
import team.ktusers.jam.cutscene.Cutscene
import team.ktusers.jam.cutscene.CutscenePosition
import team.ktusers.jam.cutscene.CutsceneText
import team.ktusers.jam.event.PlayerChangeColorEvent
import team.ktusers.jam.event.PlayerCollectColorEvent
import team.ktusers.jam.event.PlayerCollectFragmentEvent
import team.ktusers.jam.generated.BlockColor
import team.ktusers.jam.generated.PaletteColor
import team.ktusers.jam.item.ColorSelector
import team.ktusers.jam.item.getCustomItemData
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
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

        private val POINT_COLORS = hashMapOf<PaletteColor, MutableList<BlockVec>>()

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
                                    var newBlock: Block = when {
                                        block.contains("stairs", ignoreCase = true) -> JamBlock.BLACK_STAIRS
                                        block.contains("slab", ignoreCase = true) -> JamBlock.BLACK_SLAB
                                        block.contains("wall", ignoreCase = true) -> JamBlock.BLACK_WALL
                                        block.contains("fence", ignoreCase = true) -> JamBlock.BLACK_FENCE
                                        block.contains("carpet", ignoreCase = true) -> JamBlock.BLACK_CARPET
                                        block.contains("button", ignoreCase = true) -> JamBlock.BLACK_BUTTON
                                        else -> if (!current.properties()
                                                .contains("waterlogged")
                                        ) JamBlock.BLACK else continue
                                    }
                                    if (newBlock != JamBlock.BLACK) {
                                        newBlock = newBlock.withNbt(
                                            current.nbt()
                                        ).withProperties(current.properties())
                                    }

                                    val point = BlockVec(
                                        x + chunk.chunkX * 16,
                                        y,
                                        z + chunk.chunkZ * 16
                                    )
                                    val color = BlockColor.getColor(current)
                                    POINT_COLORS.getOrPut(color) { arrayListOf() }.add(point)

                                    batch.setBlock(
                                        point,
                                        newBlock
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

    val bossbar =
        BossBar.bossBar(text("Colors Collected (0/8)"), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)

    val sidebar: Sidebar = Sidebar(text("cured", TextDecoration.BOLD, color = NamedTextColor.DARK_PURPLE)).apply {
        createLine(
            Sidebar.ScoreboardLine(
                "blank_0",
                empty(),
                0,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "timer",
                text("Time Remaining: 9:00", NamedTextColor.YELLOW),
                -1,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "blank_1",
                empty(),
                -2,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "inv_name",
                text("Team Inventory", NamedTextColor.YELLOW),
                -3,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "colors",
                text("Colors: 0/8", NamedTextColor.GRAY), -4,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "fragments",
                text("Blue Fragments: 0/${5}", NamedTextColor.GRAY), -5,
                Sidebar.NumberFormat.blank()
            )
        )
    }

    override val id: NamespaceID = NamespaceID.from("ktusers", "game")

    var Player.currentColor: PaletteColor by store { PaletteColor.NONE }

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
        element {
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
            val duration = 9.minutes
            withTimeoutOrNull(duration) {
                val start = System.currentTimeMillis()
                launch {
                    while (true) {
                        val elapsed = duration - (System.currentTimeMillis() - start).milliseconds
                        sidebar.updateLineContent(
                            "timer",
                            text(
                                "Time Remaining: ${
                                    String.format(
                                        "%02d:%02d",
                                        elapsed.inWholeMinutes,
                                        elapsed.inWholeSeconds % 60
                                    )
                                }", NamedTextColor.YELLOW
                            )
                        )
                        delay(1000)
                    }
                }
                val eventNode = createElementInstanceEventNode()

                showBossBar(bossbar)
                players.forEach {
                    sidebar.addViewer(it)
                }
                Config.game.puzzles.forEach { it.onElementStart(this@JamGame, eventNode) }

                players.forEach {
                    it.inventory.setItemStack(8, ColorSelector(it.currentColor).createItemStack())
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
                eventNode.listen<PlayerChangeColorEvent> { event ->
                    val updates = hashMapOf<Int, ItemStack>()
                    val (_, player, fromColor, toColor) = event
                    player.inventory.itemStacks.forEachIndexed { slot, itemStack ->
                        val data = itemStack.getCustomItemData() as? ColorSelector? ?: return@forEachIndexed

                        updates[slot] = data.copy(selectedColor = event.toColor).createItemStack()
                    }

                    updates.forEach { (slot, itemStack) ->
                        player.inventory.setItemStack(slot, itemStack)
                    }

                    POINT_COLORS[fromColor]?.let { previous ->
                        if (teamInventory.colors.contains(fromColor)) return@let

                        player.sendPackets(
                            previous.map {
                                BlockChangePacket(it, instance.getBlock(it))
                            }
                        )
                    }
                    event.player.sendPacket(
                        particle {
                            particle = Particle.EXPLOSION_EMITTER
                            count = 2
                            position = event.player.position.add(0.0, event.player.eyeHeight - 0.5, 0.0)
                        }
                    )
                    player.currentColor = toColor
                    val points = POINT_COLORS[toColor] ?: return@listen
                    event.player.sendPackets(
                        points.map {
                            BlockChangePacket(it, REFERENCE.getBlock(it))
                        }
                    )
                }

                eventNode.listen<PlayerCollectColorEvent> { event ->
                    val batch = AbsoluteBlockBatch()
                    POINT_COLORS[event.color]?.forEach {
                        batch.setBlock(it, REFERENCE.getBlock(it))
                    }
                    batch.apply(instance, null)
                    teamInventory.colors += event.color

                    bossbar.name(text("Colors Collected (${teamInventory.colors.size}/8)", event.color.textColor))
                    bossbar.progress(teamInventory.colors.size / 8f)
                    sidebar.updateLineContent(
                        "colors",
                        text("Colors: ${teamInventory.colors.size}/8", NamedTextColor.GRAY)
                    )

                    sendMessage(
                        text("+ ", NamedTextColor.GREEN) + text(
                            event.color.name.lowercase().capitalize(),
                            event.color.textColor
                        )
                    )

                    players.forEach {
                        it.sendPacket(
                            particle {
                                particle = Particle.EXPLOSION_EMITTER
                                count = 2
                                position = it.position.add(0.0, it.eyeHeight - 0.5, 0.0)
                            }
                        )
                    }

                    playSound(
                        Sound.sound()
                            .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                            .volume(0.6f)
                            .pitch(0.8f)
                            .build()
                    )
                }
                eventNode.listen<PlayerCollectFragmentEvent> { event ->
                    val particle = particle {
                        particle = Particle.EXPLOSION
                        count = 2
                        position = event.fragment.position
                    }
                    event.fragment.sendPacketToViewers(particle)

                    event.fragment.viewersAsAudience.playSound(
                        Sound.sound()
                            .type(SoundEvent.ENTITY_ARROW_HIT_PLAYER)
                            .volume(0.6f)
                            .pitch(0.8f)
                            .build()
                    )

                    teamInventory.collectedFragments += 1
                    sidebar.updateLineContent(
                        "fragments",
                        text("Blue Fragments: ${teamInventory.collectedFragments}/5", NamedTextColor.GRAY)
                    )
                }

                delay(100000)

            }
        }
        +element {
            hideBossBar(bossbar)
            players.forEach {
                sidebar.removeViewer(it)
            }
            sendMessage(
                text("Game Overview", TextDecoration.BOLD, color = NamedTextColor.DARK_GREEN) + newline() +
                        text(
                            "Most cured: ",
                            TextDecoration.BOLD,
                            color = NamedTextColor.DARK_GREEN
                        )
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