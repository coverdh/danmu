package com.cover.danmu

interface Danmu<EP : Danmu.EpisodeInfo> {

    suspend fun getEpisodeList(cid: String, withTrailer: Boolean = false): List<EP>

    suspend fun getDanmuList(ep: EP): List<DanmuItem>


    interface EpisodeInfo {

    }

    data class DanmuItem(
        val content: String,
        val time: Long,
        val headUrl: String,
        val userName: String,
        val style: Map<String, Any>
    )
}