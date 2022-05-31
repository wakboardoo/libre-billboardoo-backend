package be.zvz.billboardoo.dto

data class Song(
    val artist: String,
    val title: String,
    val videoIds: List<String>
)
