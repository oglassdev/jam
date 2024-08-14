package team.ktusers.jam.game

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.selects.select
import net.bladehunt.blade.dsl.instance.InstanceBuilder
import net.bladehunt.blade.dsl.instance.buildInstance
import net.bladehunt.blade.ext.loadChunks
import net.bladehunt.blade.ext.logger
import net.bladehunt.kotstom.DimensionTypeRegistry
import net.bladehunt.kotstom.GlobalEventHandler
import net.bladehunt.kotstom.InstanceManager
import net.bladehunt.kotstom.dsl.listen
import net.bladehunt.kotstom.dsl.particle
import net.bladehunt.kotstom.dsl.scheduleTask
import net.bladehunt.kotstom.extension.adventure.plus
import net.bladehunt.kotstom.extension.adventure.text
import net.bladehunt.minigamelib.InstancedGame
import net.bladehunt.minigamelib.descriptor.GameDescriptor
import net.bladehunt.minigamelib.dsl.element
import net.bladehunt.minigamelib.dsl.gameDescriptor
import net.bladehunt.minigamelib.element.countdown
import net.bladehunt.minigamelib.event.game.PlayerJoinGameEvent
import net.bladehunt.minigamelib.event.game.PlayerLeaveGameEvent
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
import net.minestom.server.ServerFlag
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.batch.AbsoluteBlockBatch
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.SendablePacket
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket
import net.minestom.server.particle.Particle
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.NamespaceID
import net.minestom.server.utils.chunk.ChunkUtils
import net.minestom.server.world.DimensionType
import team.ktusers.jam.Config
import team.ktusers.jam.Lobby
import team.ktusers.jam.event.PlayerChangeColorEvent
import team.ktusers.jam.event.PlayerCollectColorEvent
import team.ktusers.jam.event.PlayerCollectFragmentEvent
import team.ktusers.jam.event.PlayerPlaceAllRelicsEvent
import team.ktusers.jam.generated.BlockColor
import team.ktusers.jam.generated.PaletteColor
import team.ktusers.jam.item.ColorSelector
import team.ktusers.jam.item.getCustomItemData
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class JamGame : InstancedGame(
    UUID.randomUUID(),
    BLACK_REFERENCE.copy().also { instance ->
        InstanceManager.registerInstance(instance)

        instance.eventNode().listen<PlayerSpawnEvent> {
            it.player.teleport(Config.game.spawnPos)
            it.player.gameMode = GameMode.ADVENTURE
        }
        instance.eventNode().listen<PlayerBlockBreakEvent> { event ->
            event.isCancelled = true
        }
        instance.eventNode().listen<PlayerBlockPlaceEvent> { event ->
            event.isCancelled = true
        }
        instance.eventNode().listen<PlayerTickEvent> { event ->
            if (event.player.position.y > -64) return@listen

            event.player.damage(Damage.fromPosition(DamageType.OUT_OF_WORLD, event.player.position, 100f))
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
                .ambientLight(7f).build()
        )

        lateinit var POINT_COLORS: Map<PaletteColor, List<SendablePacket>>
        lateinit var REVERSED: Map<PaletteColor, List<SendablePacket>>

        lateinit var COMPLETION_BATCH: AbsoluteBlockBatch

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

            instance.loadChunks(0, 0, 11).thenAccept {
                // color -> black
                val fromColors = EnumMap<PaletteColor, MutableList<SendablePacket>>(PaletteColor::class.java)
                // black -> color
                val toColors = EnumMap<PaletteColor, MutableList<SendablePacket>>(PaletteColor::class.java)

                val batch = AbsoluteBlockBatch()
                val completionBatch = AbsoluteBlockBatch()

                logger.info("Beginning to calculate batch & packets...")
                instance.chunks.forEach { chunk ->
                    for (section in chunk.minSection..<chunk.maxSection) {
                        val palette = chunk.getSection(section).blockPalette()
                        val blocks = Array<ArrayList<Long>>(PaletteColor.entries.size) { arrayListOf() }
                        val revert = Array<ArrayList<Long>>(PaletteColor.entries.size) { arrayListOf() }

                        for (x in 0..15) {
                            for (y in 0..15) {
                                for (z in 0..15) {
                                    try {
                                        val current = requireNotNull(Block.fromStateId(palette[x, y, z]))
                                        if (current.isAir) continue
                                        val block = current.namespace().path()
                                        var newBlock: Block = when {
                                            block.contains("stairs", ignoreCase = true) -> JamBlock.BLACK_STAIRS
                                            block.contains("slab", ignoreCase = true) -> JamBlock.BLACK_SLAB
                                            block.contains(
                                                "wall",
                                                ignoreCase = true
                                            ) && !block.contains("sign") && !block.contains("banner") -> JamBlock.BLACK_WALL

                                            block.contains("fence", ignoreCase = true) -> JamBlock.BLACK_FENCE
                                            block.contains("carpet", ignoreCase = true) -> JamBlock.BLACK_CARPET
                                            block.contains("leaves", ignoreCase = true) -> JamBlock.BLACK
                                            block.contains("button", ignoreCase = true) -> JamBlock.BLACK_BUTTON
                                            block.contains("sponge", ignoreCase = true) ||
                                                    block.contains("sculk") ||
                                                    block.contains("glass") ||
                                                    block.contains("water") ||
                                                    block.contains("lava") ||
                                                    current.properties().contains("waterlogged") -> continue

                                            block.contains("fern") ||
                                                    block.endsWith("grass") ||
                                                    block == "azure_bluet" ||
                                                    block == "dandelion" ||
                                                    block == "blue_orchid" ||
                                                    block == "allium" ||
                                                    block == "brown_mushroom" ||
                                                    block == "red_mushroom" ||
                                                    block == "lilac" ||
                                                    block == "poppy" -> Block.AIR

                                            else -> if (!current.properties()
                                                    .contains("waterlogged")
                                            ) JamBlock.BLACK else continue
                                        }
                                        if (newBlock != JamBlock.BLACK && newBlock != Block.AIR) {
                                            newBlock = newBlock.withNbt(
                                                current.nbt()
                                            ).withProperties(current.properties())
                                        }

                                        val color = BlockColor.getColor(current)

                                        batch.setBlock(
                                            x + chunk.chunkX * 16,
                                            y + section * 16,
                                            z + chunk.chunkZ * 16,
                                            newBlock
                                        )

                                        completionBatch.setBlock(
                                            x + chunk.chunkX * 16,
                                            y + section * 16,
                                            z + chunk.chunkZ * 16,
                                            current
                                        )

                                        blocks[color.ordinal].add(
                                            current.stateId().toLong() shl 12 or ((x shl 8 or (z shl 4) or y)).toLong()
                                        )
                                        revert[color.ordinal].add(
                                            newBlock.stateId().toLong() shl 12 or ((x shl 8 or (z shl 4) or y)).toLong()
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }

                        PaletteColor.entries.forEachIndexed { index, paletteColor ->
                            fromColors
                                .getOrPut(paletteColor) { arrayListOf() }
                                .add(
                                    MultiBlockChangePacket(
                                        chunk.chunkX,
                                        section,
                                        chunk.chunkZ,
                                        revert[index].toLongArray()
                                    )
                                )

                            toColors
                                .getOrPut(paletteColor) { arrayListOf() }
                                .add(
                                    MultiBlockChangePacket(
                                        chunk.chunkX,
                                        section,
                                        chunk.chunkZ,
                                        blocks[index].toLongArray()
                                    )
                                )
                        }
                    }
                }
                logger.info("Finished calculating batch & packets!")

                COMPLETION_BATCH = completionBatch
                POINT_COLORS = toColors
                REVERSED = fromColors

                batch.apply(instance) {
                    logger.info("Successfully applied blackout batch!")
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
                "timer",
                text("ʀᴇᴍᴀɪɴɪɴɢ: ", NamedTextColor.WHITE) + text("09:00", NamedTextColor.GRAY),
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
                text("Team", NamedTextColor.LIGHT_PURPLE),
                -3,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "colors",
                text(" ʀᴇʟɪᴄꜱ: ", NamedTextColor.RED) + text("0/7", NamedTextColor.GRAY), -4,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "deaths",
                text(" ᴅᴇᴀᴛʜꜱ: ", NamedTextColor.RED) + text("0", NamedTextColor.GRAY), -5,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "blank_2",
                empty(),
                -6,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "Fragments",
                text("Fragments", NamedTextColor.LIGHT_PURPLE), -7,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "fragment_blue",
                text(" ʙʟᴜᴇ: ", NamedTextColor.BLUE) + text("0/3", NamedTextColor.GRAY), -8,
                Sidebar.NumberFormat.blank()
            )
        )
        createLine(
            Sidebar.ScoreboardLine(
                "fragment_orange",
                text(" ᴏʀᴀɴɢᴇ: ", PaletteColor.ORANGE.textColor) + text("0/3", NamedTextColor.GRAY), -9,
                Sidebar.NumberFormat.blank()
            )
        )
    }

    override val id: NamespaceID = NamespaceID.from("ktusers", "game")

    var Player.currentColor: PaletteColor by store { PaletteColor.NONE }

    var Player.deaths: Int by store { 0 }

    var Player.colorCount: Int by store { 0 }

    private val _timeElapsed = MutableStateFlow(0)
    val timeElapsed get() = _timeElapsed.asStateFlow()

    val teamInventory = TeamInventory()

    init {
        instance.eventNode().listen<PlayerChunkLoadEvent> { event ->
            val points = POINT_COLORS[event.player.currentColor] ?: return@listen
            event.player.sendPackets(points as Collection<SendablePacket>)
        }
    }

    override val descriptor: GameDescriptor = gameDescriptor {
        +element {
            val eventNode = createElementInstanceEventNode()

            showBossBar(bossbar)
            players.forEach {
                sidebar.addViewer(it)
            }

            this@JamGame.eventNode().listen<PlayerJoinGameEvent> {
                it.player.showBossBar(bossbar)
                sidebar.addViewer(it.player)
            }

            this@JamGame.eventNode().listen<PlayerLeaveGameEvent> {
                it.player.hideBossBar(bossbar)
                sidebar.removeViewer(it.player)
            }

            eventNode.listen<EntityDamageEvent> { event ->
                val player = event.entity as? Player ?: return@listen

                event.isCancelled = true

                if (event.damage.type == DamageType.OUT_OF_WORLD) {
                    player.teleport(Config.game.spawnPos)
                }
            }

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
            val start = System.currentTimeMillis()
            val duration = 9.minutes

            val task = instance.scheduler().scheduleTask(
                delay = TaskSchedule.seconds(1),
                repeat = TaskSchedule.seconds(1)
            ) {
                val elapsed = duration - (System.currentTimeMillis() - start).milliseconds
                sidebar.updateLineContent(
                    "timer",
                    text("ʀᴇᴍᴀɪɴɪɴɢ: ", NamedTextColor.WHITE) + text(
                        String.format(
                            "%02d:%02d",
                            elapsed.inWholeMinutes,
                            elapsed.inWholeSeconds % 60
                        ), NamedTextColor.GRAY
                    )
                )
            }

            suspendCoroutine { continuation ->
                teamInventory.colors.add(PaletteColor.RED)
                teamInventory.colors.add(PaletteColor.ORANGE)
                teamInventory.colors.add(PaletteColor.YELLOW)
                teamInventory.colors.add(PaletteColor.GREEN)
                teamInventory.colors.add(PaletteColor.BLUE)
                teamInventory.colors.add(PaletteColor.INDIGO)
                teamInventory.colors.add(PaletteColor.VIOLET)

                players.forEach {
                    it.teleport(Config.game.spawnPos)
                }
                val eventNode = createElementInstanceEventNode()

                Config.game.puzzles.forEach { it.onElementStart(this@JamGame, eventNode) }

                players.forEach {
                    it.inventory.setItemStack(4, ColorSelector(it.currentColor).createItemStack())
                }

                eventNode.listen<PlayerSwapItemEvent> { it.isCancelled = true }
                eventNode.listen<ItemDropEvent> { it.isCancelled = true }
                eventNode.listen<InventoryPreClickEvent> {
                    if (it.inventory != it.player.inventory) return@listen
                    it.isCancelled = true
                }
                eventNode.listen<PlayerBlockInteractEvent> { event ->
                    val usedItem =
                        event.player.getItemInHand(Player.Hand.MAIN).getCustomItemData() ?: return@listen

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

                    REVERSED[fromColor]?.let { previous ->
                        player.sendPackets(
                            previous as Collection<SendablePacket>
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
                    event.player.sendPackets(points as Collection<SendablePacket>)
                }

                eventNode.listen<EntityDamageEvent> { event ->
                    val player = event.entity as? Player ?: return@listen

                    if (player.health - event.damage.amount > 0.0) return@listen
                    event.isCancelled = true

                    player.showTitle(
                        Title.title(text("You Died", NamedTextColor.RED), empty(), TIMES)
                    )
                    player.heal()
                    CoroutineScope(Dispatchers.Default).launch {
                        coroutineScope {
                            ChunkUtils.forChunksInRange(
                                Config.game.spawnPos.chunkX(),
                                Config.game.spawnPos.chunkZ(),
                                ServerFlag.CHUNK_VIEW_DISTANCE
                            ) { x, y ->
                                launch {
                                    player.sendChunk(instance.loadChunk(x, y).await())
                                }
                            }
                        }
                        player.teleport(Config.game.spawnPos)
                    }
                    sendMessage(player.name + text(" died!", NamedTextColor.RED))
                    player.deaths++
                    sidebar.updateLineContent(
                        "deaths",
                        text(" ᴅᴇᴀᴛʜꜱ: ", NamedTextColor.RED) + text(
                            players.sumOf { it.deaths }.toString(),
                            NamedTextColor.GRAY
                        )
                    )
                }

                eventNode.listen<PlayerCollectColorEvent> { event ->
                    teamInventory.colors += event.color
                    event.player.colorCount++

                    bossbar.name(
                        text(
                            "Colors Collected (${teamInventory.colors.size}/8)",
                            event.color.textColor
                        )
                    )
                    bossbar.progress(teamInventory.colors.size / 8f)
                    sidebar.updateLineContent(
                        "colors",
                        text(" ʀᴇʟɪᴄꜱ: ", NamedTextColor.RED) + text(
                            "${teamInventory.colors.size}/7",
                            NamedTextColor.GRAY
                        )
                    )

                    sendMessage(
                        text("+ ", NamedTextColor.GREEN)
                                + event.player.name + " collected "
                                + text(
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

                    when (event.fragment.finalColor) {
                        PaletteColor.ORANGE -> {
                            teamInventory.orangeFragments += 1

                            if (teamInventory.orangeFragments == 3) {
                                GlobalEventHandler.call(
                                    PlayerCollectColorEvent(
                                        this@JamGame, event.player, PaletteColor.ORANGE
                                    )
                                )
                            }

                            sidebar.updateLineContent(
                                "fragment_orange",
                                text(" ᴏʀᴀɴɢᴇ: ", PaletteColor.ORANGE.textColor) + text(
                                    "${teamInventory.orangeFragments}/3",
                                    NamedTextColor.GRAY
                                )
                            )
                        }

                        PaletteColor.BLUE -> {
                            teamInventory.blueFragments += 1

                            if (teamInventory.blueFragments == 3) {
                                GlobalEventHandler.call(
                                    PlayerCollectColorEvent(
                                        this@JamGame, event.player, PaletteColor.BLUE
                                    )
                                )
                            }

                            sidebar.updateLineContent(
                                "fragment_blue",
                                text(" ʙʟᴜᴇ: ", NamedTextColor.BLUE) + text(
                                    "${teamInventory.blueFragments}/3",
                                    NamedTextColor.GRAY
                                )
                            )
                        }

                        else -> throw IllegalStateException("Updated fragment must be orange or blue")
                    }

                }

                eventNode.listen<PlayerPlaceAllRelicsEvent> {
                    continuation.resume(Unit)
                }
            }

            task.cancel()

            COMPLETION_BATCH.apply(instance, null)

            sendMessage(text("You successfully cured the town!", NamedTextColor.GREEN))

            playSound(
                Sound.sound()
                    .type(SoundEvent.ENTITY_ENDER_DRAGON_DEATH)
                    .build()
            )
            delay(5000)
        }
        +element {
            hideBossBar(bossbar)
            players.forEach {
                sidebar.removeViewer(it)
            }
            val score: Int = max(teamInventory.colors.size * 5 - players.sumOf { it.deaths * 2 }, 0)
            sendMessage(
                newline() +
                        text("                   ɢᴀᴍᴇ ᴏᴠᴇʀᴠɪᴇᴡ", NamedTextColor.DARK_PURPLE) +
                        newline() + newline() +
                        text("Score: ", NamedTextColor.LIGHT_PURPLE) +
                        text("$score/35", NamedTextColor.WHITE) +
                        "      " +
                        text("Colors: ", PaletteColor.ORANGE.textColor) +
                        text("${teamInventory.colors.size}/8", NamedTextColor.GRAY) +
                        "      " +
                        text("Deaths: ", NamedTextColor.RED) +
                        text(players.sumOf { it.deaths }.toString(), NamedTextColor.GRAY) +
                        newline() +
                        text("Most Colors: ", PaletteColor.ORANGE.textColor) +
                        (players.filter { it.colorCount > 0 }.maxByOrNull { it.deaths }?.name
                            ?: text("Nobody", NamedTextColor.DARK_GRAY)) +
                        "        " +
                        text("Most Deaths: ", NamedTextColor.RED) +
                        (players.filter { it.deaths > 0 }.maxByOrNull { it.deaths }?.name
                            ?: text("Nobody", NamedTextColor.DARK_GRAY)) +
                        newline()
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