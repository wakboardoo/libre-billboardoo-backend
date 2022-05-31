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

@OptIn(ExperimentalSerializationApi::class)
object Config {
    val settings: Settings = File("settings.json").inputStream().buffered().use(Json::decodeFromStream)
    val targetVideos: TargetVideos = File("target_videos.json").inputStream().buffered().use(Json::decodeFromStream)
    val itemsData: ItemsData = File("items_data.json").inputStream().buffered().use(Json::decodeFromStream)
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
     *  "data": {
     *    "비챤": {
     *      "취기를 빌려": ["videoId1", "videoId2"]
     *    }
     *  }
     * }
     */
    @Serializable
    data class TargetVideos(
        val data: MutableMap<String, MutableMap<String, MutableList<String>>> // artist -> (title -> videoIds)
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
    data class ItemsData(
        val viewCount: MutableMap<String, CountData> // videoId -> CountData
    ) {
        @Serializable
        data class CountData(
            val hourly: SortedMap<Long, Long>, // unix timestamp -> view count
            var allTime: Long
        )
    }

    object Save {
        fun settings() = File("settings.json").bufferedWriter().write(Json.encodeToString(settings))
        fun targetVideos() = File("target_videos.json").bufferedWriter().write(Json.encodeToString(targetVideos))
        fun itemsData() = File("items_data.json").bufferedWriter().write(Json.encodeToString(itemsData))
    }
}
