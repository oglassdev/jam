package team.ktusers.gen

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class Blockstates(
    val variants: JsonObject? = null,
    val multipart: List<Multipart>? = null
)

@Serializable
data class Multipart(
    val apply: JsonElement
)

@Serializable
data class ModelData(
    val model: String
)

