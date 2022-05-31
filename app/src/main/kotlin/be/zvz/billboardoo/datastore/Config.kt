package be.zvz.billboardoo.datastore

import be.zvz.billboardoo.utils.JacksonUtils
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.LoggerFactory
import java.io.File
import java.util.SortedMap
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

object Config {
    fun init() {
        LoggerFactory.getLogger(Config::class.java).info("Initializing config")
    }

    val settings: Settings = Path("settings.json").apply {
        if (!exists()) {
            createFile()
            writeText(JacksonUtils.mapper.writeValueAsString(Settings("", "")))
        }
    }.inputStream().buffered().use(JacksonUtils.mapper::readValue)
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
                writeText(
                    JacksonUtils.mapper.writeValueAsString(
                        mutableMapOf<String, MutableMap<String, MutableList<String>>>()
                    )
                )
            }
        }.inputStream().buffered().use(JacksonUtils.mapper::readValue)
    private val automationPath = Path("automation").apply {
        if (!exists()) {
            createDirectory()
        }
    }
    val videoData: VideoData = automationPath.resolve("video_data.json").apply {
        if (!exists()) {
            createFile()
            writeText(JacksonUtils.mapper.writeValueAsString(VideoData(mutableMapOf())))
        }
    }.inputStream().buffered().use(JacksonUtils.mapper::readValue)
    val chartData: MutableMap<String, MutableMap<String, ChartDetails>> = // artist -> (title -> ChartDetails)
        automationPath.resolve("chart_data.json").apply {
            if (!exists()) {
                createFile()
                writeText(
                    JacksonUtils.mapper.writeValueAsString(
                        mutableMapOf<String, MutableMap<String, ChartDetails>>()
                    )
                )
            }
        }.inputStream().buffered().use(JacksonUtils.mapper::readValue)
    val newItems: Cache<String, Long> =
        CacheBuilder.newBuilder() // videoIds -> CountData
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build()

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
    data class VideoData(
        val viewCount: MutableMap<String, CountData> // videoId -> CountData
    ) {
        data class CountData(
            val hourly: SortedMap<Long, Long>, // unix timestamp -> view count
            var allTime: Long
        )
    }

    data class ChartDetails(
        var chartInDetails: ChartInHoursDetails = ChartInHoursDetails(),
        var maxRank: RankDetails = RankDetails(),
        val previousRank: RankDetails = RankDetails()
    ) {
        data class ChartInHoursDetails(
            var hourly: Int = 0,
            var twentyFourHours: Int = 0,
            var daily: Int = 0,
            var weekly: Int = 0,
            var monthly: Int = 0,
            var yearly: Int = 0,
            var allTime: Int = 0
        )
        data class RankDetails(
            var hourly: Int = 0,
            var twentyFourHours: Int = 0,
            var daily: Int = 0,
            var weekly: Int = 0,
            var monthly: Int = 0,
            var yearly: Int = 0,
            var allTime: Int = 0
        )
    }

    object Save {
        fun settings() = File("settings.json").bufferedWriter().use {
            it.write(JacksonUtils.mapper.writeValueAsString(settings))
        }
        fun targetVideos() = File("target_videos.json").bufferedWriter().use {
            it.write(JacksonUtils.mapper.writeValueAsString(targetVideos))
        }
        fun videoData() = Path("automation").resolve("video_data.json").bufferedWriter().use {
            it.write(JacksonUtils.mapper.writeValueAsString(videoData))
        }
        fun chartData() = Path("automation").resolve("chart_data.json").bufferedWriter().use {
            it.write(JacksonUtils.mapper.writeValueAsString(chartData))
        }
    }
}
