package co.`fun`.compare.api

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Decision {
    @SerialName("valid")
    @JsonProperty("valid")
    VALID,

    @SerialName("not_suited")
    @JsonProperty("not_suited")
    NOT_SUITED,

    @SerialName("banned")
    @JsonProperty("banned")
    BANNED
}

@Serializable
data class ModerationResponse(
    val id: String,
    val finalDecision: Decision,
    val contentDecision: Decision,
    val textDecision: Decision?,
    val type: ContentType,
    val text: String?,
    val labels: List<String>
)

@Serializable
data class ModerationPartialResponse(
    val id: String,
    val decision: Decision
)

@Serializable
data class RecognizeResponse(
    val id: String,
    val text: String?
)

@Serializable
data class LabelsResponse(
    val id: String,
    val labels: List<String>
)