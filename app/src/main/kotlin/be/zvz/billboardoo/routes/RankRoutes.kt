package be.zvz.billboardoo.routes

import be.zvz.billboardoo.datastore.Rank
import guru.zoroark.ratelimit.rateLimited
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

fun Route.rankRouting() {
    route("/rank") {
        route("/now") {
            rateLimited(
                limit = 30,
                timeBeforeReset = Duration.ofMinutes(1)
            ) {
                get("/hourly") {
                    call.respondText(Json.encodeToString(Rank.hourlyRank), ContentType.Application.Json)
                }
                get("/daily") {
                    call.respondText(Json.encodeToString(Rank.dailyRank), ContentType.Application.Json)
                }
                get("/weekly") {
                    call.respondText(Json.encodeToString(Rank.weeklyRank), ContentType.Application.Json)
                }
                get("/monthly") {
                    call.respondText(Json.encodeToString(Rank.monthlyRank), ContentType.Application.Json)
                }
                get("/yearly") {
                    call.respondText(Json.encodeToString(Rank.yearlyRank), ContentType.Application.Json)
                }
                get("/all-time") {
                    call.respondText(Json.encodeToString(Rank.allTimeRank), ContentType.Application.Json)
                }
                get("/new") {
                    call.respondText(Json.encodeToString(Rank.newRank), ContentType.Application.Json)
                }
            }
        }
    }
}
