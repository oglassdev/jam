package team.ktusers.gen

import com.github.ajalt.colormath.calculate.differenceCIE2000
import com.github.ajalt.colormath.calculate.differenceCIE76
import com.github.ajalt.colormath.model.HSL
import com.github.ajalt.colormath.model.Oklab
import com.github.ajalt.colormath.model.Oklch
import com.github.ajalt.colormath.model.RGB
import com.sksamuel.scrimage.ImmutableImage
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.NamespaceID
import java.awt.Color
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.floor

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

                var color: Oklab? = null
                try {
                    val loaded = imageLoader.fromFile(File(texture)).pixels()
                        .mapNotNull { pixel ->
                            pixel.takeIf { pixel.alpha() > 10 }?.let { px ->
                                val r = px.red().floorDiv(16) / 16.0
                                val g = px.green().floorDiv(16) / 16.0
                                val b = px.blue().floorDiv(16) / 16.0

                                val rgb = RGB(
                                    r ,
                                    g,
                                    b
                                )

                                rgb.toOklab()
                            }
                        }

                    val combinedWeightCounts = loaded.groupingBy { it }
                        .eachCount()
                        .map { Pair(it.key, (1.0 - (1.0 / it.value)) + it.key.l) }


                    val col: Oklab? = combinedWeightCounts.maxByOrNull { it.second }?.first

                    println(col?.let { lab ->
                        "${file.name} ${lab.l}, ${lab.a}, ${lab.b}"
                    })

                    col?.let { color = it }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@launch
                }

                if (color == null) return@launch

                statements.add(
                    "this.register(Block.${
                        block.namespace().path().uppercase()
                    }, ${findClosestPaletteColor(color!!)})"
                )
            }
        } ?: throw IllegalStateException("No blockstates were found")
    }

    dispatcher.close()

    val paletteColorName = ClassName("team.ktusers.jam.generated", "PaletteColor")
    val file = FileSpec.builder(className)
        .addFileComment("GENERATED!!! DO NOT EDIT")
        .addType(
            TypeSpec.objectBuilder(className.simpleName)
                .addProperty(
                    PropertySpec.builder(
                        "colors",
                        HashMap::class.asClassName().parameterizedBy(
                            ClassName("net.minestom.server.utils", NamespaceID::class.simpleName!!),
                            paletteColorName
                        )
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
                        .returns(paletteColorName)
                        .addParameter(
                            ParameterSpec.builder("namespace", NamespaceID::class)
                                .build()
                        )
                        .addStatement("return this.colors.get(namespace) ?: PaletteColor.GREY")
                        .build()
                )
                .addFunction(
                    FunSpec.builder("getColor")
                        .returns(paletteColorName)
                        .addParameter(
                            ParameterSpec.builder("block", Block::class)
                                .build()
                        )
                        .addStatement("return this.colors.get(block.namespace()) ?: PaletteColor.GREY")
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
                            ParameterSpec.builder("color", paletteColorName)
                                .build()
                        )
                        .addStatement("this.colors.put(block.namespace(), color)")
                        .build()
                )
                .build()
        )
        .build()

    file.writeTo(File("./src/generated/kotlin"))

    val paletteFile = FileSpec.builder(paletteColorName)
        .addFileComment("GENERATED!!! DO NOT EDIT")
        .addType(
            TypeSpec.enumBuilder(paletteColorName.simpleName)
                .addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("color", Color::class)
                        .addParameter("textColor", TextColor::class)
                        .build()
                )
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addFunction(
                            FunSpec.builder("fromColor")
                                .addParameter("color", Color::class)
                                .returns(ClassName("team.ktusers.jam.generated", "PaletteColor").copy(nullable = true))
                                .addStatement("return entries.firstOrNull { it.color == color }")
                                .build()
                        )
                        .build()
                )
                .addFunction(
                    FunSpec.constructorBuilder()
                        .addParameter("colorInt", Int::class)
                        .addModifiers(KModifier.PRIVATE)
                        .callThisConstructor("Color(colorInt)", "TextColor.color(colorInt)")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("color", Color::class)
                        .initializer("color")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("textColor", TextColor::class)
                        .initializer("textColor")
                        .build()
                )
                .addEnumConstant(
                    "RED",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.RED.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "ORANGE",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.ORANGE.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "YELLOW",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.YELLOW.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "GREEN",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.GREEN.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "BLUE",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.BLUE.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "INDIGO",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.INDIGO.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "VIOLET",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.VIOLET.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "GREY",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.GREY.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "BLACK",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + Palette.BLACK.toSRGB().toHex(withNumberSign = false)
                        )
                        .build()
                )
                .addEnumConstant(
                    "NONE",
                    TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter(
                            "%L",
                            "0x" + RGB(0, 0, 0).toHex(withNumberSign = false)
                        )
                        .build()
                )
                .build()
        )
        .build()

    paletteFile.writeTo(File("./src/generated/kotlin"))
}

fun findClosestPaletteColor(lab: Oklab): String {
    val color = Oklab(lab.l * 1.18, lab.a, lab.b)
    return buildMap {
        put("PaletteColor.RED", Palette.RED.differenceCIE2000(color))
        put("PaletteColor.ORANGE", Palette.ORANGE.differenceCIE2000(color))
        put("PaletteColor.YELLOW", Palette.YELLOW.differenceCIE2000(color))
        put("PaletteColor.GREEN", Palette.GREEN.differenceCIE2000(color))
        put("PaletteColor.BLUE", Palette.BLUE.differenceCIE2000(color))
        put("PaletteColor.INDIGO", Palette.INDIGO.differenceCIE2000(color))
        put("PaletteColor.VIOLET", Palette.VIOLET.differenceCIE2000(color))
        put("PaletteColor.GREY", Palette.GREY.differenceCIE2000(color))
    }.also {
        it.forEach { (color, diff) ->
            println("$color: $diff")
        }
    }.minBy { it.value }.key
}