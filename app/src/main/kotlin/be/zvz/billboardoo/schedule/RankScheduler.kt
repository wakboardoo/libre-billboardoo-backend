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
        .setApplicationName("BillBoardoo")
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

    private fun findArtistAndTitle(videoId: String): List<Pair<String, String>> =
        mutableListOf<Pair<String, String>>().apply {
            Config.targetVideos.forEach { (artist, data) ->
                data.forEach { (title, videoIds) ->
                    if (videoIds.contains(videoId)) {
                        add(Pair(artist, title))
                    }
                }
            }
        }

    private fun getRankList(
        videoIdsToArtistAndTitleMap: Map<String, List<Pair<String, String>>>,
        countProcessor: (Config.VideoData.CountData) -> Long,
        maxRankProcessor: (Config.ChartDetails.RankDetails, Int) -> Unit,
        getPreviousRank: () -> List<RankItem>,
        previousRankProcessor: (Config.ChartDetails.RankDetails, Int) -> Unit,
        chartInProcessor: (Config.ChartDetails.ChartInHoursDetails) -> Unit
    ): List<RankItem> = mutableListOf<RankItem>().apply {
        getPreviousRank().forEachIndexed { index, rankItem ->
            Config.chartData[rankItem.artist]?.get(rankItem.title)?.previousRank?.let {
                previousRankProcessor(it, index + 1)
            }
        }
        Config.videoData.viewCount.forEach { (videoId, data) ->
            videoIdsToArtistAndTitleMap[videoId]?.forEach titleMapForEach@{ pair ->
                val (artist, title) = pair
                val count = countProcessor(data).let {
                    if (it == -1L) {
                        return@titleMapForEach
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

    internal fun updateRank(timestamp: Long) {
        val videoIdsToArtistAndTitleMap = mutableMapOf<String, List<Pair<String, String>>>().apply {
            Config.videoData.viewCount.forEach { (videoId, _) ->
                val list = findArtistAndTitle(videoId)
                if (list.isNotEmpty()) {
                    put(videoId, list)
                }
            }
        }
        Rank.allTime = RankResponse(
            timestamp,
            getRankList(videoIdsToArtistAndTitleMap, Config.VideoData.CountData::allTime, { rankDetails, rank ->
                rankDetails.allTime = min(rankDetails.allTime, rank)
            }, {
                Rank.allTime.ranking
            }, { rankDetails, rank ->
                rankDetails.allTime = rank
            }, {
                it.allTime++
            })
        )
        Rank.hourly = RankResponse(
            timestamp,
            getRankList(videoIdsToArtistAndTitleMap, {
                it.hourly[timestamp] ?: -1L
            }, { rankDetails, rank ->
                rankDetails.hourly = min(rankDetails.hourly, rank)
            }, {
                Rank.hourly.ranking
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
            Rank.twentyFourHours = RankResponse(
                timestamp,
                getRankList(videoIdsToArtistAndTitleMap, {
                    it.hourly.subMap(dayStartEpochSecond, timestamp + 1).values.sum()
                }, { rankDetails, rank ->
                    rankDetails.twentyFourHours = min(rankDetails.twentyFourHours, rank)
                }, {
                    Rank.twentyFourHours.ranking
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
            .apply {
                if (
                    Rank.daily.timestamp == 0L || isAfter(
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Rank.daily.timestamp),
                            timeZone
                        )
                    )
                ) {
                    val dayStartEpochSecond = minusDays(1).toEpochSecond()
                    val dayEndEpochSecond = toEpochSecond()
                    Rank.daily = RankResponse(
                        dayEndEpochSecond,
                        getRankList(videoIdsToArtistAndTitleMap, {
                            it.hourly.subMap(dayStartEpochSecond, dayEndEpochSecond).values.sum()
                        }, { rankDetails, rank ->
                            rankDetails.daily = min(rankDetails.daily, rank)
                        }, {
                            Rank.daily.ranking
                        }, { rankDetails, rank ->
                            rankDetails.daily = rank
                        }, {
                            it.daily++
                        })
                    )
                }
            }
        zonedDateTime
            .with(DayOfWeek.SATURDAY)
            .withHour(12)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .apply {
                val tempDateTime = when (zonedDateTime.dayOfWeek) {
                    DayOfWeek.SATURDAY -> {
                        if (zonedDateTime.hour < 18) {
                            minusWeeks(1)
                        } else {
                            this
                        }
                    }
                    DayOfWeek.SUNDAY -> {
                        this
                    }
                    else -> {
                        minusWeeks(1)
                    }
                }
                if (
                    Rank.weekly.timestamp == 0L || tempDateTime.withHour(18).isAfter(
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Rank.weekly.timestamp),
                            timeZone
                        ).withHour(18)
                    )
                ) {
                    val weekStartEpochSecond = tempDateTime.minusWeeks(1).toEpochSecond()
                    val weekEndEpochSecond = tempDateTime.toEpochSecond()
                    Rank.weekly = RankResponse(
                        weekEndEpochSecond,
                        getRankList(videoIdsToArtistAndTitleMap, {
                            it.hourly.subMap(weekStartEpochSecond, weekEndEpochSecond).values.sum()
                        }, { rankDetails, rank ->
                            rankDetails.weekly = min(rankDetails.weekly, rank)
                        }, {
                            Rank.weekly.ranking
                        }, { rankDetails, rank ->
                            rankDetails.weekly = rank
                        }, {
                            it.weekly++
                        })
                    )
                }
            }
        zonedDateTime.withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .apply {
                if (
                    Rank.monthly.timestamp == 0L || isAfter(
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Rank.monthly.timestamp),
                            timeZone
                        )
                    )
                ) {
                    val monthStartEpochSecond = minusMonths(1).toEpochSecond()
                    val monthEndEpochSecond = toEpochSecond()
                    Rank.monthly = RankResponse(
                        monthEndEpochSecond,
                        getRankList(videoIdsToArtistAndTitleMap, {
                            it.hourly.subMap(monthStartEpochSecond, monthEndEpochSecond).values.sum()
                        }, { rankDetails, rank ->
                            rankDetails.monthly = min(rankDetails.monthly, rank)
                        }, {
                            Rank.monthly.ranking
                        }, { rankDetails, rank ->
                            rankDetails.monthly = rank
                        }, {
                            it.monthly++
                        })
                    )
                }
            }
        zonedDateTime.withMonth(1)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .apply {
                if (
                    Rank.yearly.timestamp == 0L || isAfter(
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(Rank.yearly.timestamp),
                            timeZone
                        )
                    )
                ) {
                    val yearStartEpochSecond = minusYears(1).toEpochSecond()
                    val yearEndEpochSecond = toEpochSecond()
                    Rank.yearly = RankResponse(
                        yearEndEpochSecond,
                        getRankList(videoIdsToArtistAndTitleMap, {
                            it.hourly.subMap(yearStartEpochSecond, yearEndEpochSecond).values.sum()
                        }, { rankDetails, rank ->
                            rankDetails.yearly = min(rankDetails.yearly, rank)
                        }, {
                            Rank.yearly.ranking
                        }, { rankDetails, rank ->
                            rankDetails.yearly = rank
                        }, {
                            it.yearly++
                        })
                    )
                }
            }
        Rank.new = RankResponse(
            timestamp,
            mutableListOf<RankItem>().apply {
                Config.newItems.asMap().forEach { (videoId, _) ->
                    videoIdsToArtistAndTitleMap[videoId]?.forEach titleMapForEach@{ pair ->
                        val (artist, title) = pair
                        val count = Config.videoData.viewCount[videoId]?.hourly?.get(timestamp) ?: return@titleMapForEach
                        this.find { it.artist == artist && it.title == title }?.let {
                            it.videoIds.add(videoId)
                            it.count += count
                            return@titleMapForEach
                        } ?: add(
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
            }
        )
    }

    fun apply() {
        val timestamp = ZonedDateTime.now(timeZone).toEpochSecond()
        updateVideoCount(timestamp)
        updateRank(timestamp)
        Config.Save.videoData()
        Config.Save.chartData()
        Config.Save.newItems()
    }

    init {
        Scheduler.scheduler.schedule(
            RankScheduler::apply,
            CronSchedule.parseQuartzCron("0 0 * * * ? *")
        )
    }
}
