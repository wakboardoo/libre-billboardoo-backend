package be.zvz.billboardoo.datastore

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.SortedMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

@OptIn(ExperimentalSerializationApi::class)
object Config {
    val settings: Settings = Path("settings.json").apply {
        if (!exists()) {
            createFile()
        }
        writeText(Json.encodeToString(Settings("", "")))
    }.inputStream().buffered().use(Json::decodeFromStream)
    /**
     * {
     *  "비챤": {
     *    "취기를 빌려": ["videoId1", "videoId2"]
     *  }
     * }
     */
    val targetVideos: MutableMap<String, MutableMap<String, MutableList<String>>> = // artist -> (title -> videoIds)
        Path("target_videos.json").apply {
            if (!exists()) {
                createFile()
            }
            writeText(Json.encodeToString(mutableMapOf<String, MutableMap<String, MutableList<String>>>()))
        }.inputStream().buffered().use(Json::decodeFromStream)
    private val automationPath = Path("automation").apply {
        if (!exists()) {
            createDirectory()
        }
    }
    val videoData: VideoData = automationPath.resolve("video_data.json").apply {
        if (!exists()) {
            createFile()
        }
        writeText(Json.encodeToString(VideoData(mutableMapOf())))
    }.inputStream().buffered().use(Json::decodeFromStream)
    val chartData: MutableMap<String, MutableMap<String, ChartDetails>> = // artist -> (title -> (chart-in-hours))
        automationPath.resolve("chart_data.json").apply {
            if (!exists()) {
                createFile()
            }
            writeText(
                Json.encodeToString(
                    mutableMapOf<String, MutableMap<String, ChartDetails>>()
                )
            )
        }.inputStream().buffered().use(Json::decodeFromStream)
    val newItems: Cache<String, Long> =
        CacheBuilder.newBuilder() // videoIds -> CountData
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build()

    @Serializable
    data class Settings(
        val youtubeDataApiKey: String,
        val secretKey: String
    )

    /**
     * {
     *  "viewCount": {
     *    "videoId1": {
     *      "hourly": {
     *        "1653948069": 3000,
     *        "1653948070": 4000,
     *      },
     *      "allTime": 7000
     *    }
     *  }
     */
    @Serializable
    data class VideoData(
        val viewCount: MutableMap<String, CountData> // videoId -> CountData
    ) {
        @Serializable
        data class CountData(
            val hourly: SortedMap<Long, Long>, // unix timestamp -> view count
            var allTime: Long
        )
    }

    @Serializable
    data class ChartDetails(
        val chartInHours: Long,
        val maxRank: RankDetails,
        val lastRank: RankDetails
    ) {
        @Serializable
        data class RankDetails(
            val hourly: Int,
            val daily: Int,
            val weekly: Int,
            val monthly: Int,
            val yearly: Int,
            val allTime: Int
        )
    }

    object Save {
        fun settings() = File("settings.json").bufferedWriter().use { it.write(Json.encodeToString(settings)) }
        fun targetVideos() = File("target_videos.json").bufferedWriter().use { it.write(Json.encodeToString(targetVideos)) }
        fun videoData() = Path("automation").resolve("video_data.json").bufferedWriter().use { it.write(Json.encodeToString(videoData)) }
        fun chartData() = Path("automation").resolve("chart_data.json").bufferedWriter().use { it.write(Json.encodeToString(chartData)) }
    }
}
