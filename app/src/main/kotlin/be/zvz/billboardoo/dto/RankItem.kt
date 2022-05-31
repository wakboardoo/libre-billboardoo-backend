package be.zvz.billboardoo.dto

import kotlinx.serialization.Serializable

@Serializable
data class RankItem(
    val videoIds: MutableList<String>,
    val artist: String,
    val title: String,
    var count: Long
)
