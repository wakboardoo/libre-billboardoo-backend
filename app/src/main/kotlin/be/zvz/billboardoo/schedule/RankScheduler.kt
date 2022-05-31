package be.zvz.billboardoo.schedule

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.datastore.Rank
import be.zvz.billboardoo.dto.RankItem
import com.coreoz.wisp.schedule.cron.CronSchedule
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlin.math.max

object RankScheduler {
    private val youtube = YouTube.Builder(NetHttpTransport(), GsonFactory(), null)
        .setApplicationName("BillBodoo")
        .build()
    private val statisticsList = listOf("statistics")
    private val timeZone = TimeZone.getTimeZone("Asia/Seoul").toZoneId()

    fun init() {
        LoggerFactory.getLogger(RankScheduler::class.java).info("Initializing RankScheduler")
    }

    // TODO: 비효율적이지만 예쁜 데이터 저장 방법을 선택할 것인지,
    //  효율적이지만 예쁘지 않은 데이터 저장 방식을 사용할 것인지 고민해봐야 함
    //  일단은 전자를 사용할 예정 (몇천개 정도 element로는 크게 부담되지 않기 때문)
    private fun updateVideoCount(timestamp: Long) {
        Config.targetVideos.flatMap { (_, data) -> data.flatMap { (_, videoIds) -> videoIds } }
            .chunked(50)
            .parallelStream()
            .forEach { chunk ->
                val list = youtube.videos().list(statisticsList)
                list.id = chunk
                list.key = Config.settings.youtubeDataApiKey
                list.execute().items.forEach { videoData ->
                    Config.videoData.viewCount[videoData.id]?.let {
                        val count = it.hourly.values.lastOrNull() ?: 0
                        it.hourly[timestamp] = videoData.statistics.viewCount.toLong() - count
                        it.allTime = count
                    }
                }
            }
    }

    private fun findArtistAndTitle(videoId: String): Pair<String, String>? {
        Config.targetVideos.forEach { (artist, data) ->
            data.forEach { (title, videoIds) ->
                if (videoIds.contains(videoId)) {
                    return Pair(artist, title)
                }
            }
        }
        return null
    }

    private fun getRankList(
        videoIdsToArtistAndTitleMap: Map<String, Pair<String, String>>,
        countProcessor: (Config.VideoData.CountData) -> Long,
        maxRankProcessor: (Config.ChartDetails.RankDetails, Int) -> Unit,
        getPreviousRank: () -> List<RankItem>,
        previousRankProcessor: (Config.ChartDetails.RankDetails, Int) -> Unit,
    ): List<RankItem> = mutableListOf<RankItem>().apply {
        getPreviousRank().forEachIndexed { index, rankItem ->
            Config.chartData[rankItem.artist]?.get(rankItem.title)?.previousRank?.let {
                previousRankProcessor(it, index + 1)
            }
        }
        Config.videoData.viewCount.forEach { (videoId, data) ->
            val (artist, title) = videoIdsToArtistAndTitleMap[videoId] ?: return@forEach
            val count = countProcessor(data).let {
                if (it == -1L) {
                    return@forEach
                } else {
                    it
                }
            }
            val index = this.indexOfFirst { it.artist == artist && it.title == title }
            if (index != -1) {
                this[index].count += count
            } else {
                add(
                    RankItem(
                        videoId = videoId,
                        artist = artist,
                        title = title,
                        count = count
                    )
                )
            }
        }
        sortBy(RankItem::count)
        forEachIndexed { index, item ->
            val tempMap = mutableMapOf<String, Config.ChartDetails>()
            val tempChartDetails = Config.ChartDetails()
            val resultChartDetails = (
                Config.chartData
                    .putIfAbsent(item.artist, tempMap) ?: tempMap
                )
                .putIfAbsent(item.title, tempChartDetails) ?: tempChartDetails
            resultChartDetails.maxRank.apply {
                maxRankProcessor(this, index + 1)
            }
            if (index < 100) {
                resultChartDetails.chartInHours++
            }
        }
    }

    private fun updateRank(timestamp: Long) {
        val videoIdsToArtistAndTitleMap = mutableMapOf<String, Pair<String, String>>().apply {
            Config.videoData.viewCount.forEach { (videoId, _) ->
                put(videoId, findArtistAndTitle(videoId) ?: return@forEach)
            }
        }
        Rank.allTimeRank = getRankList(videoIdsToArtistAndTitleMap, Config.VideoData.CountData::allTime, { rankDetails, rank ->
            rankDetails.allTime = max(rankDetails.allTime, rank)
        }, {
            Rank.allTimeRank
        }, { rankDetails, rank ->
            rankDetails.allTime = rank
        })
        Rank.hourlyRank = getRankList(videoIdsToArtistAndTitleMap, {
            it.hourly[timestamp] ?: -1L
        }, { rankDetails, rank ->
            rankDetails.hourly = max(rankDetails.hourly, rank)
        }, {
            Rank.hourlyRank
        }, { rankDetails, rank ->
            rankDetails.hourly = rank
        })
        Rank.twentyFourHoursRank = getRankList(videoIdsToArtistAndTitleMap, {
            val zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            )
                .minusDays(1)
            val dayStartEpochSecond = zonedDateTime.toEpochSecond()
            val dayEndEpochSecond = zonedDateTime.plusDays(1).toEpochSecond()
            it.hourly.subMap(dayStartEpochSecond, dayEndEpochSecond + 1).values.sum()
        }, { rankDetails, rank ->
            rankDetails.twentyFourHour = max(rankDetails.twentyFourHour, rank)
        }, {
            Rank.twentyFourHoursRank
        }, { rankDetails, rank ->
            rankDetails.twentyFourHour = rank
        })
        Rank.dailyRank = getRankList(videoIdsToArtistAndTitleMap, {
            val zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            )
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusDays(1)
            val dayStartEpochSecond = zonedDateTime.toEpochSecond()
            val dayEndEpochSecond = zonedDateTime.plusDays(1).toEpochSecond()
            it.hourly.subMap(dayStartEpochSecond, dayEndEpochSecond).values.sum()
        }, { rankDetails, rank ->
            rankDetails.daily = max(rankDetails.daily, rank)
        }, {
            Rank.dailyRank
        }, { rankDetails, rank ->
            rankDetails.daily = rank
        })
        Rank.weeklyRank = getRankList(videoIdsToArtistAndTitleMap, {
            val zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            )
                .with(DayOfWeek.MONDAY)
                .minusWeeks(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            val weekStartEpochSecond = zonedDateTime
                .toEpochSecond()
            val weekEndEpochSecond = zonedDateTime
                .plusWeeks(1)
                .toEpochSecond()
            it.hourly.subMap(weekStartEpochSecond, weekEndEpochSecond).values.sum()
        }, { rankDetails, rank ->
            rankDetails.weekly = max(rankDetails.weekly, rank)
        }, {
            Rank.weeklyRank
        }, { rankDetails, rank ->
            rankDetails.weekly = rank
        })
        Rank.monthlyRank = getRankList(videoIdsToArtistAndTitleMap, {
            val zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            ).withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusMonths(1)
            val monthStartEpochSecond = zonedDateTime
                .toEpochSecond()
            val monthEndEpochSecond = zonedDateTime
                .plusMonths(1)
                .toEpochSecond()
            it.hourly.subMap(monthStartEpochSecond, monthEndEpochSecond).values.sum()
        }, { rankDetails, rank ->
            rankDetails.monthly = max(rankDetails.monthly, rank)
        }, {
            Rank.monthlyRank
        }, { rankDetails, rank ->
            rankDetails.monthly = rank
        })
        Rank.yearlyRank = getRankList(videoIdsToArtistAndTitleMap, {
            val zonedDateTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            ).withMonth(1)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusYears(1)
            val yearStartEpochSecond = zonedDateTime
                .toEpochSecond()
            val yearEndEpochSecond = zonedDateTime
                .plusYears(1)
                .toEpochSecond()
            it.hourly.subMap(yearStartEpochSecond, yearEndEpochSecond).values.sum()
        }, { rankDetails, rank ->
            rankDetails.yearly = max(rankDetails.yearly, rank)
        }, {
            Rank.yearlyRank
        }, { rankDetails, rank ->
            rankDetails.yearly = rank
        })
        Rank.newRank = mutableListOf<RankItem>().apply {
            Config.newItems.asMap().forEach { (videoId, _) ->
                val (artist, title) = videoIdsToArtistAndTitleMap[videoId] ?: return@forEach
                val count = Config.videoData.viewCount[videoId]?.hourly?.get(timestamp) ?: return@forEach
                this.find { it.artist == artist && it.title == title }?.let {
                    it.count += count
                    return@forEach
                } ?: add(
                    RankItem(
                        videoId = videoId,
                        artist = artist,
                        title = title,
                        count = count
                    )
                )
            }
            sortBy(RankItem::count)
        }
    }

    private fun apply() {
        val timestamp = ZonedDateTime.now(timeZone).toEpochSecond()
        updateVideoCount(timestamp)
        updateRank(timestamp)
        Config.Save.videoData()
        Config.Save.chartData()
    }

    init {
        Scheduler.scheduler.schedule(
            RankScheduler::apply,
            CronSchedule.parseQuartzCron("0 0 * * * ? *")
        )
    }
}
