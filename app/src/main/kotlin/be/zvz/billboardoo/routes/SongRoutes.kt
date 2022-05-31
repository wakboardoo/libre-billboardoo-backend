package be.zvz.billboardoo.routes

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.dto.Song
import be.zvz.billboardoo.utils.JacksonUtils
import guru.zoroark.ratelimit.rateLimited
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.Duration

@Serializable
data class SongResponse(
    val result: Boolean,
    val message: String
)
fun Route.songRouting() {
    route("/song") {
        put {
            if (call.request.header("Authorization") == Config.settings.secretKey) {
                val songs = call.receive<List<Song>>()
                songs.forEach {
                    Config.targetVideos.getOrPut(it.artist) {
                        mutableMapOf()
                    }.getOrPut(it.title) {
                        mutableListOf()
                    }.addAll(it.videoIds)
                    it.videoIds.forEach { videoId ->
                        Config.newItems.put(videoId, 0)
                    }
                }
                Config.Save.targetVideos()
                call.respondText(
                    JacksonUtils.mapper.writeValueAsString(
                        SongResponse(true, "Songs added")
                    ),
                    ContentType.Application.Json
                )
            } else {
                call.respondText(
                    JacksonUtils.mapper.writeValueAsString(
                        SongResponse(false, "Invalid secret key")
                    ),
                    ContentType.Application.Json,
                    HttpStatusCode.Unauthorized
                )
            }
        }
        rateLimited(
            limit = 1000,
            timeBeforeReset = Duration.ofMinutes(1)
        ) {
            get("/{artist?}/{title?}") {
                val artist = call.parameters["artist"]
                val title = call.parameters["title"]
                if (artist != null && title != null) {
                    call.respondText(
                        Config.chartData[artist]?.get(title)?.let(JacksonUtils.mapper::writeValueAsString) ?: "{}",
                        ContentType.Application.Json
                    )
                }
            }
        }
        rateLimited(
            limit = 30,
            timeBeforeReset = Duration.ofMinutes(1)
        ) {
            get("/target-videos") {
                call.respondText(
                    JacksonUtils.mapper.writeValueAsString(Config.targetVideos),
                    ContentType.Application.Json
                )
            }
        }
        rateLimited(
            limit = 30,
            timeBeforeReset = Duration.ofMinutes(1)
        ) {
            get("/chart-data") {
                call.respondText(
                    JacksonUtils.mapper.writeValueAsString(Config.chartData),
                    ContentType.Application.Json
                )
            }
        }
    }
}
