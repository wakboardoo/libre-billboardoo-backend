package be.zvz.billboardoo.dto

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val artist: String,
    val title: String,
    val videoIds: List<String>
)
