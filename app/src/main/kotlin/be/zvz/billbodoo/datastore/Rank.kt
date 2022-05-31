package be.zvz.billbodoo.datastore

import be.zvz.billbodoo.dto.RankItem

object Rank {
    var hourlyRank: List<RankItem> = emptyList()
    var dailyRank: List<RankItem> = emptyList()
    var weeklyRank: List<RankItem> = emptyList()
    var monthlyRank: List<RankItem> = emptyList()
    var yearlyRank: List<RankItem> = emptyList()
    var allTimeRank: List<RankItem> = emptyList()
}
