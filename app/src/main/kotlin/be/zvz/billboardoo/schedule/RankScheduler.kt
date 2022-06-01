package be.zvz.billboardoo.schedule

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.datastore.Rank
import be.zvz.billboardoo.dto.RankItem
import be.zvz.billboardoo.dto.RankResponse
import com.coreoz.wisp.schedule.cron.CronSchedule
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlin.math.min

object RankScheduler {
    private val youtube = YouTube.Builder(NetHttpTransport(), JacksonFactory(), null)
        .setApplicationName("BillBodoo")
        .build()
    private val statisticsList = listOf("statistics")
    private val timeZone = TimeZone.getTimeZone("Asia/Seoul").toZoneId()
    private val logger = LoggerFactory.getLogger(RankScheduler::class.java)

    fun init() {
        logger.info("Initializing RankScheduler")
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
                    val viewCount = videoData.statistics.viewCount.toLong()
                    Config.videoData.viewCount.getOrPut(videoData.id) {
                        Config.VideoData.CountData(sortedMapOf(), viewCount)
                    }.let {
                        it.hourly[timestamp] = viewCount - it.allTime
                        it.allTime = viewCount
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
        chartInProcessor: (Config.ChartDetails.ChartInHoursDetails) -> Unit,
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
                this[index].apply {
                    this.videoIds.add(videoId)
                    this.count += count
                }
            } else {
                add(
                    RankItem(
                        videoIds = mutableListOf(videoId),
                        artist = artist,
                        title = title,
                        count = count
                    )
                )
            }
        }
        sortByDescending(RankItem::count)
        forEachIndexed { index, item ->
            val resultChartDetails = Config.chartData
                .getOrPut(item.artist) { mutableMapOf() }
                .getOrPut(item.title) { Config.ChartDetails() }
            resultChartDetails.maxRank.apply {
                maxRankProcessor(this, index + 1)
            }
            if (index < 100) {
                chartInProcessor(resultChartDetails.chartInDetails)
            }
        }
    }

    private fun updateRank(timestamp: Long) {
        val videoIdsToArtistAndTitleMap = mutableMapOf<String, Pair<String, String>>().apply {
            Config.videoData.viewCount.forEach { (videoId, _) ->
                put(videoId, findArtistAndTitle(videoId) ?: return@forEach)
            }
        }
        Rank.allTimeRank = RankResponse(
            timestamp,
            getRankList(videoIdsToArtistAndTitleMap, Config.VideoData.CountData::allTime, { rankDetails, rank ->
                rankDetails.allTime = min(rankDetails.allTime, rank)
            }, {
                Rank.allTimeRank.ranking
            }, { rankDetails, rank ->
                rankDetails.allTime = rank
            }, {
                it.allTime++
            })
        )
        Rank.hourlyRank = RankResponse(
            timestamp,
            getRankList(videoIdsToArtistAndTitleMap, {
                it.hourly[timestamp] ?: -1L
            }, { rankDetails, rank ->
                rankDetails.hourly = min(rankDetails.hourly, rank)
            }, {
                Rank.hourlyRank.ranking
            }, { rankDetails, rank ->
                rankDetails.hourly = rank
            }, {
                it.hourly++
            })
        )
        val zonedDateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp),
            timeZone
        )
        zonedDateTime.minusDays(1).apply {
            val dayStartEpochSecond = toEpochSecond()
            Rank.twentyFourHoursRank = RankResponse(
                timestamp,
                getRankList(videoIdsToArtistAndTitleMap, {
                    it.hourly.subMap(dayStartEpochSecond, timestamp + 1).values.sum()
                }, { rankDetails, rank ->
                    rankDetails.twentyFourHours = min(rankDetails.twentyFourHours, rank)
                }, {
                    Rank.twentyFourHoursRank.ranking
                }, { rankDetails, rank ->
                    rankDetails.twentyFourHours = rank
                }, {
                    it.twentyFourHours++
                })
            )
        }
        zonedDateTime.withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusDays(1)
            .apply {
                val dayStartEpochSecond = toEpochSecond()
                val dayEndEpochSecond = plusDays(1).toEpochSecond()
                Rank.dailyRank = RankResponse(
                    dayEndEpochSecond,
                    getRankList(videoIdsToArtistAndTitleMap, {
                        it.hourly.subMap(dayStartEpochSecond, dayEndEpochSecond).values.sum()
                    }, { rankDetails, rank ->
                        rankDetails.daily = min(rankDetails.daily, rank)
                    }, {
                        Rank.dailyRank.ranking
                    }, { rankDetails, rank ->
                        rankDetails.daily = rank
                    }, {
                        it.daily++
                    })
                )
            }
        zonedDateTime.with(DayOfWeek.MONDAY)
            .minusWeeks(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .apply {
                val weekStartEpochSecond = toEpochSecond()
                val weekEndEpochSecond = plusWeeks(1)
                    .toEpochSecond()
                Rank.weeklyRank = RankResponse(
                    weekEndEpochSecond,
                    getRankList(videoIdsToArtistAndTitleMap, {
                        it.hourly.subMap(weekStartEpochSecond, weekEndEpochSecond).values.sum()
                    }, { rankDetails, rank ->
                        rankDetails.weekly = min(rankDetails.weekly, rank)
                    }, {
                        Rank.weeklyRank.ranking
                    }, { rankDetails, rank ->
                        rankDetails.weekly = rank
                    }, {
                        it.weekly++
                    })
                )
            }
        zonedDateTime.withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusMonths(1).apply {
                val monthStartEpochSecond = toEpochSecond()
                val monthEndEpochSecond = plusMonths(1)
                    .toEpochSecond()
                Rank.monthlyRank = RankResponse(
                    monthEndEpochSecond,
                    getRankList(videoIdsToArtistAndTitleMap, {
                        it.hourly.subMap(monthStartEpochSecond, monthEndEpochSecond).values.sum()
                    }, { rankDetails, rank ->
                        rankDetails.monthly = min(rankDetails.monthly, rank)
                    }, {
                        Rank.monthlyRank.ranking
                    }, { rankDetails, rank ->
                        rankDetails.monthly = rank
                    }, {
                        it.monthly++
                    })
                )
            }
        zonedDateTime.withMonth(1)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .minusYears(1)
            .apply {
                val yearStartEpochSecond = toEpochSecond()
                val yearEndEpochSecond = plusYears(1)
                    .toEpochSecond()
                Rank.yearlyRank = RankResponse(
                    yearEndEpochSecond,
                    getRankList(videoIdsToArtistAndTitleMap, {
                        it.hourly.subMap(yearStartEpochSecond, yearEndEpochSecond).values.sum()
                    }, { rankDetails, rank ->
                        rankDetails.yearly = min(rankDetails.yearly, rank)
                    }, {
                        Rank.yearlyRank.ranking
                    }, { rankDetails, rank ->
                        rankDetails.yearly = rank
                    }, {
                        it.yearly++
                    })
                )
            }
        Rank.newRank = RankResponse(
            timestamp,
            mutableListOf<RankItem>().apply {
                Config.newItems.asMap().forEach { (videoId, _) ->
                    val (artist, title) = videoIdsToArtistAndTitleMap[videoId] ?: return@forEach
                    val count = Config.videoData.viewCount[videoId]?.hourly?.get(timestamp) ?: return@forEach
                    this.find { it.artist == artist && it.title == title }?.let {
                        it.videoIds.add(videoId)
                        it.count += count
                        return@forEach
                    } ?: add(
                        RankItem(
                            videoIds = mutableListOf(videoId),
                            artist = artist,
                            title = title,
                            count = count
                        )
                    )
                }
                sortByDescending(RankItem::count)
            }
        )
    }

    fun apply() {
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
