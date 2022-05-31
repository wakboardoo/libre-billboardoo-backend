package be.zvz.billbodoo.dto

import kotlinx.serialization.Serializable

@Serializable
data class RankItem(
    val videoId: String,
    val author: String,
    val title: String,
    val count: Long
)
