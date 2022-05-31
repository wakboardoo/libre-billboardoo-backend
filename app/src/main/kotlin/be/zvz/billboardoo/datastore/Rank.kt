package be.zvz.billboardoo.datastore

import be.zvz.billboardoo.dto.RankItem

object Rank {
    var hourlyRank: List<RankItem> = emptyList()
    var dailyRank: List<RankItem> = emptyList()
    var weeklyRank: List<RankItem> = emptyList()
    var monthlyRank: List<RankItem> = emptyList()
    var yearlyRank: List<RankItem> = emptyList()
    var allTimeRank: List<RankItem> = emptyList()
}
