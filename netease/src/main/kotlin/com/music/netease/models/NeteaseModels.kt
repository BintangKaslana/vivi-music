package com.music.netease.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NeteaseArtist(
    val id: Long = 0,
    val name: String = ""
)

@Serializable
data class NeteaseAlbum(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String? = null
)

@Serializable
data class NeteaseSong(
    val id: Long = 0,
    val name: String = "",
    val artists: List<NeteaseArtist> = emptyList(),
    @SerialName("ar") val ar: List<NeteaseArtist> = emptyList(),
    val album: NeteaseAlbum? = null,
    @SerialName("al") val al: NeteaseAlbum? = null,
    @SerialName("dt") val dt: Long = 0,
    val duration: Long = 0
) {
    val effectiveArtists: List<NeteaseArtist>
        get() = artists.ifEmpty { ar }

    val effectiveAlbum: NeteaseAlbum?
        get() = album ?: al

    val effectiveDurationMs: Long
        get() = if (duration > 0) duration else dt
}

@Serializable
data class NeteaseSearchSongResult(
    val songs: List<NeteaseSong> = emptyList(),
    val songCount: Int = 0
)

@Serializable
data class NeteaseSearchResponse(
    val result: NeteaseSearchSongResult? = null,
    val code: Int = 200
)

@Serializable
data class NeteaseStreamUrlItem(
    val id: Long = 0,
    val url: String? = null,
    val br: Long = 0,
    val size: Long = 0,
    val code: Int = 200,
    val type: String? = null,
    val level: String? = null
)

@Serializable
data class NeteaseStreamUrlResponse(
    val data: List<NeteaseStreamUrlItem> = emptyList(),
    val code: Int = 200
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
