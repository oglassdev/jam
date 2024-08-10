package team.ktusers.gen

import com.squareup.kotlinpoet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.serialization.json.*
import net.minestom.server.instance.block.Block
import java.awt.Color
import java.io.File
import java.util.concurrent.Executors
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

val JsonSerializer = Json {
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val dispatcher = Executors
        .newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        .asCoroutineDispatcher()

    val className = ClassName("team.ktusers.jam.generated", "BlockColor")

    val initializer = CodeBlock.builder()

    val inputDirectory = File("./generated/input/blockstates")

    check(inputDirectory.exists()) { "blockstates directory does not exist" }


    CoroutineScope(dispatcher).apply {
        inputDirectory.listFiles()?.forEach { file ->
            file.inputStream().use { stream ->
                val json = JsonSerializer.decodeFromStream<Blockstates>(stream)

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

                val block = Block.fromNamespaceId(file.nameWithoutExtension) ?: return@use

                initializer.addStatement("this.register(Block.${block.namespace().path().uppercase()}, Color(127))")
            }
        } ?: throw IllegalStateException("No blockstates were found")
    }

    val file = FileSpec.builder(className)
        .addFileComment("GENERATED!!! DO NOT EDIT")
        .addType(
            TypeSpec.classBuilder(className.simpleName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        "colors",
                        typeOf<HashMap<Block, Color>>().javaType
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .initializer(
                            CodeBlock.builder()
                                .addStatement("hashMapOf()")
                                .build()
                        )
                        .build()
                )
                .addInitializerBlock(initializer.build())
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
                        .addStatement("this.colors.put(block, color)")
                        .build()
                )
                .build()
        )
        .build()

    file.writeTo(File("./src/generated/kotlin"))
}