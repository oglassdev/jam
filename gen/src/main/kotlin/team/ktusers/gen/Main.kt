package team.ktusers.gen

import com.github.ajalt.colormath.calculate.differenceCIE2000
import com.github.ajalt.colormath.model.RGB
import com.sksamuel.scrimage.ImmutableImage
import com.squareup.kotlinpoet.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.NamespaceID
import java.awt.Color
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

val JsonSerializer = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    val dispatcher = Executors
        .newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        .asCoroutineDispatcher()

    val className = ClassName("team.ktusers.jam.generated", "BlockColor")

    val initializer = CodeBlock.builder()

    val inputDirectory = File("./generated/input/blockstates")

    check(inputDirectory.exists()) { "blockstates directory does not exist" }

    val statements = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    withContext(dispatcher) {
        val imageLoader = ImmutableImage.loader()
        inputDirectory.listFiles()?.forEach { file ->
            launch {
                if (file.name.contains("air.json")) return@launch
                val json = file.inputStream().use { stream ->
                    JsonSerializer.decodeFromStream<Blockstates>(stream)
                }

                val modelData: ModelData = when {
                    json.variants != null -> {
                        when (val variant = json.variants.values.firstOrNull()) {
                            is JsonArray -> variant.firstOrNull()
                            is JsonObject -> variant
                            else -> null
                        }?.let { el -> JsonSerializer.decodeFromJsonElement<ModelData>(el) }
                    }

                    json.multipart != null -> {
                        when (val data = json.multipart.firstOrNull()?.apply) {
                            is JsonArray -> data.firstOrNull()
                            is JsonObject -> data
                            else -> null
                        }?.let { el -> JsonSerializer.decodeFromJsonElement<ModelData>(el) }
                    }

                    else -> null
                }
                    ?: error("modeldata was null")

                val block = Block.fromNamespaceId(file.nameWithoutExtension) ?: return@launch

                val model: Model
                try {
                    model =
                        File("./generated/input/models/${modelData.model.removePrefix("minecraft:block/")}.json").inputStream()
                            .use {
                                JsonSerializer.decodeFromStream(it)
                            }
                } catch (e: Exception) {
                    println(file.name)
                    e.printStackTrace()
                    return@launch
                }

                val textureName = model.textures.values.first()
                    .removePrefix("minecraft:block/")
                    .removePrefix("block/")
                if (textureName.contains("minecraft:item")) return@launch
                val texture = "./generated/input/textures/$textureName.png"

                var color: RGB? = null
                try {
                    val loaded = imageLoader.fromFile(File(texture)).pixels()
                        .mapNotNull { pixel ->
                            pixel.takeIf { pixel.alpha() > 10 }?.let { px ->
                                RGB(
                                    (px.red().floorDiv(16) / 16.0).toFloat(),
                                    (px.green().floorDiv(16) / 16.0).toFloat(),
                                    (px.blue().floorDiv(16) / 16.0).toFloat()
                                )
                            }
                        }
                        .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

                    println(loaded?.let { color -> "${file.name} ${color.redInt}, ${color.greenInt}, ${color.blueInt}" })
                    if (loaded != null) color = loaded
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@launch
                }

                if (color == null) return@launch

                statements.add(
                    "this.register(Block.${
                        block.namespace().path().uppercase()
                    }, ${findClosestPaletteColor(color)})"
                )
            }
        } ?: throw IllegalStateException("No blockstates were found")
    }

    dispatcher.close()

    val file = FileSpec.builder(className)
        .addFileComment("GENERATED!!! DO NOT EDIT")
        .addType(
            TypeSpec.objectBuilder(className.simpleName)
                .addProperty(
                    PropertySpec.builder(
                        "colors",
                        typeOf<HashMap<NamespaceID, Color>>().javaType
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .initializer(
                            CodeBlock.builder()
                                .addStatement("hashMapOf()")
                                .build()
                        )
                        .build()
                )
                .addInitializerBlock(initializer
                    .also { builder ->
                        statements.forEach {
                            builder.addStatement(it)
                        }
                    }
                    .build())
                .addFunction(
                    FunSpec.builder("getColor")
                        .returns(Color::class)
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter(
                            ParameterSpec.builder("namespace", NamespaceID::class)
                                .build()
                        )
                        .addStatement("return this.colors.get(namespace) ?: Palette.GREY")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("getColor")
                        .returns(Color::class)
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter(
                            ParameterSpec.builder("block", Block::class)
                                .build()
                        )
                        .addStatement("return this.colors.get(block.namespace()) ?: Palette.GREY")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("register")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter(
                            ParameterSpec.builder("block", Block::class)
                                .build()
                        )
                        .addParameter(
                            ParameterSpec.builder("color", Color::class)
                                .build()
                        )
                        .addStatement("this.colors.put(block.namespace(), color)")
                        .build()
                )
                .build()
        )
        .build()

    file.writeTo(File("./src/generated/kotlin"))

    val paletteFile = FileSpec.builder(ClassName("team.ktusers.jam.generated", "Palette"))
        .addFileComment("GENERATED!!! DO NOT EDIT")
        .addType(
            TypeSpec.objectBuilder("Palette")
                .addProperty(
                    PropertySpec.builder("RED", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.RED.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("ORANGE", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.ORANGE.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("YELLOW", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.YELLOW.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("GREEN", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.GREEN.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("BLUE", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.BLUE.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("INDIGO", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.INDIGO.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("VIOLET", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.VIOLET.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("GREY", Color::class)
                        .initializer(
                            CodeBlock.of(
                                Palette.GREY.toSRGB().let { "Color(${it.redInt}, ${it.greenInt}, ${it.blueInt})" })
                        )
                        .build()
                )
                .build()
        )
        .build()

    paletteFile.writeTo(File("./src/generated/kotlin"))
}

fun findClosestPaletteColor(input: RGB): String {
    val hsl = input.toHSL()
    if (abs(hsl.s) < 0.1) return "Palette.GREY"


    return buildMap {
        put("Palette.RED", Palette.RED.differenceCIE2000(hsl))
        put("Palette.ORANGE", Palette.ORANGE.differenceCIE2000(hsl))
        put("Palette.YELLOW", Palette.YELLOW.differenceCIE2000(hsl))
        put("Palette.GREEN", Palette.GREEN.differenceCIE2000(hsl))
        put("Palette.BLUE", Palette.BLUE.differenceCIE2000(hsl))
        put("Palette.INDIGO", Palette.INDIGO.differenceCIE2000(hsl))
        put("Palette.VIOLET", Palette.VIOLET.differenceCIE2000(hsl))
        put("Palette.GREY", Palette.GREY.differenceCIE2000(hsl))
    }.minBy { it.value }.key
}