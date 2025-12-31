package com.github.andock.daemon.search

import com.github.andock.daemon.app.AppGlobals
import io.ktor.http.Parameters
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Search parameters for Docker Hub repository search API.
 *
 * API Endpoint: https://hub.docker.com/v2/search/repositories/
 */
@Serializable
data class SearchParameters(
    @SerialName("query")
    val query: String,
    @SerialName("page_size")
    val pageSize: Int = 25,
    @SerialName("page")
    val page: Int = 1,
    @SerialName("is_official")
    val isOfficial: Boolean?,
    @SerialName("type")
    val type: String = "image"
) {
    fun toParameters(): Parameters {
        val jsonObject = AppGlobals.json.encodeToJsonElement(this) as JsonObject
        return parameters {
            jsonObject.forEach { (k, v) ->
                if (v !is JsonNull) {
                    append(k, v.toString())
                }
            }
        }
    }
}