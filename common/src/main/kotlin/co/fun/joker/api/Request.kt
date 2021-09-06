package co.`fun`.joker.api

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModerationRequest(
    val id: String,
    val url: String,
    val additionalDelay: Long? = null
)

@Serializable
enum class ContentType {
    @SerialName("image")
    @JsonProperty("image")
    IMAGE,

    @SerialName("video")
    @JsonProperty("video")
    VIDEO
}

@Serializable
data class ContentClassifyRequest(
    val id: String,
    val url: String,
    val type: ContentType,
    val additionalDelay: Long? = null
)

@Serializable
data class ContentLabelRequest(
    val id: String,
    val url: String,
    val additionalDelay: Long? = null
)

@Serializable
data class TextClassifyRequest(
    val id: String,
    val text: String,
    val additionalDelay: Long? = null
)

@Serializable
data class TextRecognizeRequest(
    val id: String,
    val url: String,
    val type: ContentType,
    val additionalDelay: Long? = null
)