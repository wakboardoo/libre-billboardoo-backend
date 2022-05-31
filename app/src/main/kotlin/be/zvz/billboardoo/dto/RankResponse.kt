package be.zvz.billboardoo.dto

data class RankResponse(
    val timestamp: Long = 0,
    val ranking: List<RankItem> = emptyList()
)
