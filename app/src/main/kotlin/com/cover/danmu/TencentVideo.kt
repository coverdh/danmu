package com.cover.danmu

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TencentVideo(
    val client: HttpClient
) : Danmu<TencentVideo.TencentVideoEpisodeInfo> {

    data class TencentVideoEpisodeInfo(
        val cid: String, //剧集id
        val vid: String, //视频id
        val imageUrl: String, // 封面
        val duration: Duration, // 长度
        val title: String, // 标题
        val ep: String, //  集
        val trailer: Boolean,//  预告
    ) : Danmu.EpisodeInfo

    override suspend fun getEpisodeList(
        cid: String,
        withTrailer: Boolean
    ): List<TencentVideoEpisodeInfo> {
        val epList = mutableListOf<TencentVideoEpisodeInfo>()
        var pageContext: String? = ""
        val limit = 10
        var loopCount = 0
        while (pageContext != null) {
            loopCount++
            pageContext = getEpisodeList(cid, pageContext, epList)
            if (loopCount >= limit) {
                throw java.lang.RuntimeException("Get ep loop is  over limit, now limit = $limit")
            }
        }
        return if (withTrailer) epList else epList.filter { !it.trailer }
    }

    @OptIn(InternalAPI::class)
    suspend fun getEpisodeList(
        cid: String,
        pageContent: String,
        epList: MutableList<TencentVideoEpisodeInfo>
    ): String? {
        val response: HttpResponse = client.post(GET_EP_LIST_URL) {
            contentType(ContentType.Application.Json)
            cookie("vversion_name", "8.2.95")
            headers {
                append("origin", "https://v.qq.com")
                append("referer", "https://v.qq.com/")
                append("authority", "pbaccess.video.qq.com")
                append(
                    "user-agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36"
                )
            }
            body = """
                {
                    "page_params": {
                        "req_from": "web",
                        "page_type": "detail_operation",
                        "page_id": "vsite_episode_list",
                        "id_type": "1",
                        "cid": "$cid",
                        "lid": "",
                        "page_num": "",
                        "page_size": "30",
                        "page_context": "$pageContent"
                    },
                    "has_cache": 1
                }
             """.trimIndent()
        }
        val body = response.body<String>()
        val responseData = JSON.parseObject(body)
        val retCode = responseData.getIntValue("ret")
        val msg = responseData.getString("msg")
        if (retCode != 0) {
            throw java.lang.RuntimeException("Get tencent video episode list failed, code:$retCode, mgs:$msg")
        }
        val data = responseData.getJSONObject("data")
        var nextPageContent: String? = null

        data.getJSONArray("module_list_datas").forEach { module ->
            module as JSONObject
            module.getJSONArray("module_datas").forEach { moduleData ->
                moduleData as JSONObject
                val moduleParams = moduleData.getJSONObject("module_params")
                if (moduleParams.getBoolean("has_next")) {
                    nextPageContent = moduleParams.getString("page_context")
                }
                moduleData.getJSONObject("item_data_lists").getJSONArray("item_datas").forEach { ep ->
                    ep as JSONObject
                    val params = ep.getJSONObject("item_params")
                    epList.add(
                        TencentVideoEpisodeInfo(
                            cid = cid,
                            vid = params.getString("vid"),
                            imageUrl = params.getString("image_url"),
                            duration = params.getString("duration").toLong().seconds,
                            title = params.getString("play_title"),
                            ep = params.getString("title"),
                            trailer = params.getString("is_trailer") == "1"
                        )
                    )
                }
            }
        }
        return nextPageContent
    }

    override suspend fun getDanmuList(ep: TencentVideoEpisodeInfo): List<Danmu.DanmuItem> {
        var timeStamp = 0
        var duration = 30
        val ret = mutableListOf<Danmu.DanmuItem>()
        val targetId = getTargetId(ep)
        coroutineScope {
            val ts = mutableListOf<Int>()
            val max = ep.duration.inWholeSeconds - duration
            while (timeStamp < max) {
                ts.add(timeStamp)
                timeStamp += duration
            }
            ts.map {
                async {
                    getDanmuList(targetId, it)
                }
            }.awaitAll().forEach {
                ret.addAll(it)
            }
        }
        return ret
    }

    suspend fun getDanmuList(targetId: String, timeStamp: Int): List<Danmu.DanmuItem> {
        val response: HttpResponse = client.get(String.format(GET_DANMU_URL, targetId, timeStamp))
        val body = response.body<String>()
        val responseData = JSON.parseObject(body)
        val retCode = responseData.getIntValue("err_code")
        val msg = responseData.getString("err_msg")
        if (retCode != 0) {
            throw java.lang.RuntimeException("Get tencent video danmu meta info failed, code:$retCode, mgs:$msg")
        }
        val comments = responseData.getJSONArray("comments")
        return comments.map {
            it as JSONObject
            val style = JSON.parseObject(it.getString("content_style"))
            val styleMap = mutableMapOf<String, Any>()
            if (style != null) {
                if (style.containsKey("color")) {
                    styleMap["color"] = style.getString("color")
                }
                if (style.containsKey("position")) {
                    styleMap["position"] = style.getString("position")
                }
            }
            Danmu.DanmuItem(
                content = it.getString("content"),
                time = it.getLong("timepoint"),
                headUrl = it.getString("headurl"),
                userName = it.getString("opername"),
                style = styleMap
            )
        }
    }

    @OptIn(InternalAPI::class)
    suspend fun getTargetId(ep: TencentVideoEpisodeInfo): String {
        val response: HttpResponse = client.post(GET_DANMU_PARAMS_URL) {
            contentType(ContentType.Application.Json)
            cookie("vversion_name", "8.2.95")
            headers {
                append("origin", "https://v.qq.com")
                append("referer", "https://v.qq.com/")
                append("authority", "access.video.qq.com")
                append(
                    "user-agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36"
                )
            }
            body = """
                {
                    "wRegistType": 2,
                    "vecIdList": [
                        "${ep.vid}"
                    ],
                    "wSpeSource": 0,
                    "bIsGetUserCfg": 1,
                    "mapExtData": {
                        "s0042spdnkc": {
                            "strCid": "${ep.cid}",
                            "strLid": ""
                        }
                    }
                }
                """.trimIndent()
        }
        val body = response.body<String>()
        val responseData = JSON.parseObject(body)
        val retCode = responseData.getIntValue("ret")
        val msg = responseData.getString("msg")
        if (retCode != 0) {
            throw java.lang.RuntimeException("Get tencent video danmu meta info failed, code:$retCode, mgs:$msg")
        }
        val data = responseData.getJSONObject("data")
        val info = data.getJSONObject("stMap").getJSONObject(ep.vid)
        val strDanMuKey = info.getString("strDanMuKey")
        val params = parseQueryString(strDanMuKey)
        return params["targetid"]
            ?: throw java.lang.RuntimeException("Get tencent video danmu meta info failed, targetId not exist, strDanMuKey=$strDanMuKey. ")
    }

    companion object {
        const val GET_EP_LIST_URL =
            "https://pbaccess.video.qq.com/trpc.universal_backend_service.page_server_rpc.PageServer/GetPageData?video_appid=3000010&vplatform=2"
        const val GET_DANMU_PARAMS_URL =
            "https://access.video.qq.com/danmu_manage/regist?vappid=97767206&vsecret=c0bdcbae120669fff425d0ef853674614aa659c605a613a4&raw=1"
        const val GET_DANMU_URL = "https://mfm.video.qq.com/danmu?target_id=%s&timestamp=%s"
    }
}