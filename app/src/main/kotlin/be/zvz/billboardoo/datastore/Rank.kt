package be.zvz.billboardoo.datastore

import be.zvz.billboardoo.dto.RankResponse
import be.zvz.billboardoo.schedule.RankScheduler

object Rank {
    var hourly = RankResponse()
    var twentyFourHours = RankResponse()
    var daily = RankResponse()
    var weekly = RankResponse()
    var monthly = RankResponse()
    var yearly = RankResponse()
    var allTime = RankResponse()
    var new = RankResponse()
    var festival = RankResponse()

    fun init() {
        Config.videoData.viewCount.firstNotNullOf { (_, countData) ->
            RankScheduler.updateRank(countData.hourly.lastKey())
        }
    }
}
