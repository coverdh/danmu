package com.cover.danmu

import io.ktor.client.*
import kotlin.time.Duration

class IQiyi(
    val client: HttpClient
) : Danmu<IQiyi.IQiyiEpisodeInfo> {


    override suspend fun getEpisodeList(cid: String, withTrailer: Boolean): List<IQiyiEpisodeInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getDanmuList(ep: IQiyiEpisodeInfo): List<Danmu.DanmuItem> {
        TODO("Not yet implemented")
    }


    data class IQiyiEpisodeInfo(
        val cid: String, //剧集id
        val tvId: String, //视频id
        val vid:String, // ?
        val imageUrl: String, // 封面
        val duration: Duration, // 长度
        val title: String, // 标题
        val description:String, //  描述
        val ep: String, //  集
        val trailer: Boolean,//  预告
    ) : Danmu.EpisodeInfo
}