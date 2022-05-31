package be.zvz.billboardoo.routes

import be.zvz.billboardoo.datastore.Config
import be.zvz.billboardoo.dto.Song
import com.google.gson.JsonObject
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.songRouting() {
    route("/song") {
        put {
            if (call.request.header("Authorization") == Config.settings.secretKey) {
                val songs = call.receive<List<Song>>()
                songs.forEach {
                    Config.targetVideos.putIfAbsent(it.artist, mutableMapOf())?.let { data ->
                        data.putIfAbsent(it.title, mutableListOf())?.addAll(it.videoIds)
                    }
                    it.videoIds.forEach { videoId ->
                        Config.newItems.put(videoId, 0)
                    }
                }
                Config.Save.targetVideos()
                call.respondText(
                    JsonObject().addProperty("result", true).toString(),
                    ContentType.Application.Json
                )
            } else {
                call.respondText(
                    JsonObject().addProperty("result", false).toString(),
                    ContentType.Application.Json,
                    HttpStatusCode.Unauthorized
                )
            }
        }
    }
}
