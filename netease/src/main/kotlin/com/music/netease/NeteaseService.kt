/**
 * VIVI-NETEASE (C) 2026
 * NetEase Cloud Music Audio Streaming Service
 */

package com.music.netease

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class NeteaseArtist(
    val id: Long = 0,
    val name: String = ""
)

data class NeteaseAlbum(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String? = null
)

data class NeteaseSong(
    val id: Long = 0,
    val name: String = "",
    val artists: List<NeteaseArtist> = emptyList(),
    val album: NeteaseAlbum? = null,
    val duration: Long = 0
) {
    val effectiveArtists: List<NeteaseArtist>
        get() = artists

    val effectiveAlbum: NeteaseAlbum?
        get() = album

    val effectiveDurationMs: Long
        get() = duration
}

data class NeteaseStreamUrlItem(
    val id: Long = 0,
    val url: String? = null,
    val br: Long = 0,
    val size: Long = 0,
    val code: Int = 200,
    val type: String? = null,
    val level: String? = null
)

enum class NeteaseAudioQuality(val apiLevel: String, val displayName: String) {
    LOSSLESS("lossless", "FLAC / Lossless"),
    EXHIGH("exhigh", "320 kbps (High)"),
    STANDARD("standard", "128 kbps (Standard)");

    companion object {
        fun fromApiValue(value: String): NeteaseAudioQuality {
            return values().firstOrNull { it.apiLevel.equals(value, ignoreCase = true) } ?: EXHIGH
        }
    }
}

object NeteaseService {

    private const val TAG = "NeteaseService"
    private const val BASE_URL = "https://music.163.com"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun sanitizeQuery(query: String): String {
        if (query.isBlank()) return ""
        val cleaned = query
            .replace(Regex("[/&#?:\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned
    }

    fun encodeQuery(query: String): String {
        return try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }
    }

    suspend fun searchSongs(rawQuery: String): Result<List<NeteaseSong>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = sanitizeQuery(rawQuery)
            Log.d(TAG, "searchSongs: rawQuery=\"$rawQuery\", cleanQuery=\"$cleanQuery\"")
            
            if (cleanQuery.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Query is empty after sanitization"))
            }

            val encodedQuery = encodeQuery(cleanQuery)
            val url = "$BASE_URL/api/search/get/web?s=$encodedQuery&type=1&limit=5&offset=0"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://music.163.com/")
                .addHeader("Cookie", "os=pc; osver=Microsoft-Windows-10-Professional-build-19041-64bit; appver=2.9.7")
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "searchSongs: HTTP status: ${response.code}")
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("NetEase search failed: HTTP ${response.code}"))
            }

            val responseText = response.body?.string().orEmpty()
            val jsonRoot = JSONObject(responseText)
            val resultObj = jsonRoot.optJSONObject("result")
            val songsArray = resultObj?.optJSONArray("songs") ?: JSONArray()

            val songs = mutableListOf<NeteaseSong>()
            for (i in 0 until songsArray.length()) {
                val songObj = songsArray.optJSONObject(i) ?: continue
                val id = songObj.optLong("id")
                val name = songObj.optString("name")
                val duration = songObj.optLong("duration", songObj.optLong("dt", 0))

                val artistsArray = songObj.optJSONArray("artists") ?: songObj.optJSONArray("ar") ?: JSONArray()
                val artists = mutableListOf<NeteaseArtist>()
                for (j in 0 until artistsArray.length()) {
                    val artObj = artistsArray.optJSONObject(j) ?: continue
                    artists.add(NeteaseArtist(id = artObj.optLong("id"), name = artObj.optString("name")))
                }

                val albumObj = songObj.optJSONObject("album") ?: songObj.optJSONObject("al")
                val album = if (albumObj != null) {
                    NeteaseAlbum(
                        id = albumObj.optLong("id"),
                        name = albumObj.optString("name"),
                        picUrl = albumObj.optString("picUrl").takeIf { it.isNotBlank() }
                    )
                } else null

                songs.add(
                    NeteaseSong(
                        id = id,
                        name = name,
                        artists = artists,
                        album = album,
                        duration = duration
                    )
                )
            }

            Log.d(TAG, "searchSongs: found ${songs.size} songs for query=\"$cleanQuery\"")

            if (songs.isEmpty()) {
                return@withContext Result.failure(NoSuchElementException("No songs found on NetEase for: \"$cleanQuery\""))
            }

            Result.success(songs)
        } catch (e: Exception) {
            Log.e(TAG, "searchSongs failed for rawQuery=\"$rawQuery\"", e)
            Result.failure(e)
        }
    }

    suspend fun getStreamUrl(songId: Long, level: String = "lossless"): NeteaseStreamUrlItem? = withContext(Dispatchers.IO) {
        Log.d(TAG, "getStreamUrl: songId=$songId, level=$level")
        
        val levelsToTry = when (level.lowercase()) {
            "lossless" -> listOf("lossless", "exhigh", "standard")
            "exhigh" -> listOf("exhigh", "standard")
            else -> listOf("standard")
        }

        for (targetLevel in levelsToTry) {
            try {
                val url = "$BASE_URL/api/song/enhance/player/url/v1?ids=%5B$songId%5D&level=$targetLevel&encodeType=flac"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                    .addHeader("Referer", "https://music.163.com/")
                    .addHeader("Cookie", "os=pc; osver=Microsoft-Windows-10-Professional-build-19041-64bit; appver=2.9.7")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseText = response.body?.string().orEmpty()
                    val jsonRoot = JSONObject(responseText)
                    val dataArray = jsonRoot.optJSONArray("data") ?: JSONArray()
                    val itemObj = dataArray.optJSONObject(0)

                    if (itemObj != null) {
                        val streamItem = NeteaseStreamUrlItem(
                            id = itemObj.optLong("id"),
                            url = itemObj.optString("url").takeIf { it.isNotBlank() },
                            br = itemObj.optLong("br"),
                            size = itemObj.optLong("size"),
                            code = itemObj.optInt("code", 200),
                            type = itemObj.optString("type"),
                            level = itemObj.optString("level")
                        )

                        if (!streamItem.url.isNullOrBlank()) {
                            Log.d(TAG, "getStreamUrl: resolved stream URL for level=$targetLevel, url=${streamItem.url}")
                            return@withContext streamItem
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "getStreamUrl attempt failed for level=$targetLevel", e)
            }
        }

        Log.w(TAG, "getStreamUrl: no valid stream URL found for songId=$songId")
        null
    }

    suspend fun getContentLength(url: String): Long? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "getContentLength: url=$url")
            val request = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=0-0")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 206 || response.code == 200) {
                val contentRange = response.header("Content-Range")
                val len = contentRange?.substringAfter('/')?.toLongOrNull()
                    ?: response.header("Content-Length")?.toLongOrNull()
                Log.d(TAG, "getContentLength: resolved length=$len")
                len
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getContentLength failed for url=$url", e)
            null
        }
    }
}
