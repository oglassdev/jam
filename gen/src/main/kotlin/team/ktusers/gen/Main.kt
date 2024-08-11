package team.ktusers.gen

import com.github.ajalt.colormath.model.Oklab
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
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
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

                val color: Oklab
                try {
                    val loaded = imageLoader.fromFile(File(texture)).average()
                    color = RGB(loaded.red, loaded.green, loaded.blue).toOklab()

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@launch
                }

                val rgb = mapOf(
                    "Palette.RED" to deltaE2000(Palette.RED, color),
                    "Palette.ORANGE" to deltaE2000(Palette.ORANGE, color),
                    "Palette.YELLOW" to deltaE2000(Palette.YELLOW, color),
                    "Palette.GREEN" to deltaE2000(Palette.GREEN, color),
                    "Palette.BLUE" to deltaE2000(Palette.BLUE, color),
                    "Palette.INDIGO" to deltaE2000(Palette.INDIGO, color),
                    "Palette.VIOLET" to deltaE2000(Palette.VIOLET, color),
                    "Palette.GREY" to deltaE2000(Palette.GREY, color)
                ).minBy { it.value }.key

                statements.add(
                    "this.register(Block.${
                        block.namespace().path().uppercase()
                    }, $rgb)"
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

fun deltaE2000(lab1: Oklab, lab2: Oklab): Double {
    val l = lab1.l - lab2.l
    val a = lab1.a - lab2.a
    val b = lab1.b - lab2.b

    val c1 = sqrt(lab1.a * lab1.a + lab1.b * lab1.b)
    val c2 = sqrt(lab2.a * lab2.a + lab2.b * lab2.b)

    val deltaC = c1 - c2

    val deltaH = sqrt(a * a + b * b - deltaC * deltaC)

    val kL = 1.0
    val kC = 1.0
    val kH = 1.0


    val sL = 1.0
    val sC = 1.0 + 0.045 * c1
    val sH = 1.0 + 0.015 * c1

    val deltaTheta = 0.5236
    val c1p = c1 * cos(deltaTheta)
    val c2p = c2 * cos(deltaTheta)

    val rT = -sin(deltaTheta) * (c1p + c2p)

    val deltaE = sqrt(
        (l / (kL * sL)).pow(2) +
                (deltaC / (kC * sC)).pow(2) +
                (deltaH / (kH * sH)).pow(2) +
                rT * (deltaC / (kC * sC)) * (deltaH / (kH * sH))
    )

    return deltaE
}