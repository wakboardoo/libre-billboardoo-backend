package be.zvz.billboardoo.routes

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.dto.Song
import com.google.gson.Gson
import com.google.gson.JsonObject
import guru.zoroark.ratelimit.rateLimited
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

private val gson = Gson()
fun Route.songRouting() {
    route("/song") {
        put {
            if (call.request.header("Authorization") == Config.settings.secretKey) {
                val songs = call.receive<List<Song>>()
                songs.forEach {
                    val tempMap = mutableMapOf<String, MutableList<String>>()
                    val resultMap = Config.targetVideos.putIfAbsent(it.artist, tempMap) ?: tempMap
                    val tempList = mutableListOf<String>()
                    val resultList = resultMap.putIfAbsent(it.title, tempList) ?: tempList
                    resultList.addAll(it.videoIds)
                    it.videoIds.forEach { videoId ->
                        Config.newItems.put(videoId, 0)
                    }
                }
                Config.Save.targetVideos()
                call.respondText(
                    gson.toJson(JsonObject().addProperty("result", true)),
                    ContentType.Application.Json
                )
            } else {
                call.respondText(
                    gson.toJson(JsonObject().addProperty("result", false)),
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
                        Config.chartData[artist]?.get(title)?.let(Json::encodeToString) ?: "{}",
                        ContentType.Application.Json
                    )
                }
            }
        }
        rateLimited(
            limit = 30,
            timeBeforeReset = Duration.ofMinutes(1)
        ) {
            get("/all") {
                call.respondText(
                    Json.encodeToString(Config.chartData),
                    ContentType.Application.Json
                )
            }
        }
    }
}
