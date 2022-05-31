package be.zvz.billboardoo.dto

data class RankItem(
    val videoIds: MutableList<String>,
    val artist: String,
    val title: String,
    var count: Long
)
