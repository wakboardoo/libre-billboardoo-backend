package be.zvz.billboardoo.schedule

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.datastore.Rank
import be.zvz.billboardoo.dto.RankItem
import com.coreoz.wisp.schedule.cron.CronSchedule
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime
import java.util.TimeZone

object RankScheduler {
    private val youtube = YouTube.Builder(NetHttpTransport(), GsonFactory(), null)
        .setApplicationName("BillBodoo")
        .build()
    private val statisticsList = listOf("statistics")
    private val timeZone = TimeZone.getTimeZone("Asia/Seoul").toZoneId()

    // TODO: 비효율적이지만 예쁜 데이터 저장 방법을 선택할 것인지,
    //  효율적이지만 예쁘지 않은 데이터 저장 방식을 사용할 것인지 고민해봐야 함
    //  일단은 전자를 사용할 예정 (몇천개 정도 element로는 크게 부담되지 않기 때문)
    private fun updateVideoCount(timestamp: Long) {
        val list = youtube.videos().list(statisticsList)
        list.id = Config.targetVideos.data.flatMap { (_, data) -> data.flatMap { (_, videoIds) -> videoIds } }
        list.key = Config.settings.youtubeDataApiKey
        list.execute().items.forEach { videoData ->
            Config.itemsData.viewCount[videoData.id]?.let {
                val count = it.hourly.values.lastOrNull() ?: 0
                it.hourly[timestamp] = videoData.statistics.viewCount.toLong() - count
                it.allTime = count
            }
        }
    }

    private fun findArtistAndTitle(videoId: String): Pair<String, String>? {
        Config.targetVideos.data.forEach { (artist, data) ->
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
        countProcessor: (Config.ItemsData.CountData) -> Long
    ): List<RankItem> = mutableListOf<RankItem>().apply {
        Config.itemsData.viewCount.forEach { (videoId, data) ->
            val (artist, title) = videoIdsToArtistAndTitleMap[videoId] ?: return@forEach
            val count = countProcessor(data).let {
                if (it == -1L) {
                    return@forEach
                } else {
                    it
                }
            }
            add(
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

    private fun updateRank(timestamp: Long) {
        val videoIdsToArtistAndTitleMap = mutableMapOf<String, Pair<String, String>>().apply {
            Config.itemsData.viewCount.forEach { (videoId, _) ->
                put(videoId, findArtistAndTitle(videoId) ?: return@forEach)
            }
        }
        Rank.allTimeRank = getRankList(videoIdsToArtistAndTitleMap, Config.ItemsData.CountData::allTime)
        Rank.hourlyRank = getRankList(videoIdsToArtistAndTitleMap) {
            it.hourly[timestamp] ?: -1L
        }
        Rank.dailyRank = getRankList(videoIdsToArtistAndTitleMap) {
            val dayStartEpochSecond = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            )
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toEpochSecond()
            it.hourly.tailMap(dayStartEpochSecond).values.sum()
        }
        Rank.weeklyRank = getRankList(videoIdsToArtistAndTitleMap) {
            val weekStartEpochSecond = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            )
                .with(DayOfWeek.MONDAY)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toEpochSecond()
            it.hourly.tailMap(weekStartEpochSecond).values.sum()
        }
        Rank.monthlyRank = getRankList(videoIdsToArtistAndTitleMap) {
            val monthStartEpochSecond = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            )
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toEpochSecond()
            it.hourly.tailMap(monthStartEpochSecond).values.sum()
        }
        Rank.yearlyRank = getRankList(videoIdsToArtistAndTitleMap) {
            val yearStartEpochSecond = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                timeZone
            )
                .withMonth(1)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toEpochSecond()
            it.hourly.tailMap(yearStartEpochSecond).values.sum()
        }
        Rank.newRank = mutableListOf<RankItem>().apply {
            Config.newItems.asMap().forEach { (videoId, _) ->
                val (artist, title) = videoIdsToArtistAndTitleMap[videoId] ?: return@forEach
                add(
                    RankItem(
                        videoId = videoId,
                        artist = artist,
                        title = title,
                        count = Config.itemsData.viewCount[videoId]?.hourly?.get(timestamp) ?: return@forEach
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
        Config.Save.itemsData()
    }

    init {
        Scheduler.scheduler.schedule(
            RankScheduler::apply,
            CronSchedule.parseQuartzCron("0 0 * * * ? *")
        )
    }
}
