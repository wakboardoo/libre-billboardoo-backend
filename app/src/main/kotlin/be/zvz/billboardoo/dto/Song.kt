package be.zvz.billboardoo.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Song(
    val artist: String,
    val title: String,
    val videoIds: List<String>,
    val isOld: Boolean
)
