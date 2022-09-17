package be.zvz.billboardoo.dto

data class SummaryRankResponse(
    var hourly: List<RankItem>,
    var twentyFourHours: List<RankItem>,
    var daily: List<RankItem>,
    var weekly: List<RankItem>,
    var monthly: List<RankItem>,
    var yearly: List<RankItem>,
    var allTime: List<RankItem>,
    var new: List<RankItem>,
    var festival: List<RankItem>
)
