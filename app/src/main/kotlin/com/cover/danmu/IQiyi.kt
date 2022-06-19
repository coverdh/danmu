package com.cover.danmu

import com.alibaba.fastjson.JSONObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import org.jsoup.Jsoup
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class IQiyi(
    val client: HttpClient
) : Danmu<IQiyi.IQiyiEpisodeInfo> {

    val epListUrl: (String) -> String = { "https://www.iqiyi.com/$it.html" }

    val document = DocumentBuilderFactory.newInstance()

    val xpath = XPathFactory.newInstance().newXPath()

    val danmuPath = "/danmu/data/entry/list/bulletInfo"

    override suspend fun getEpisodeList(cid: String, withTrailer: Boolean): List<IQiyiEpisodeInfo> {
        val html = client.get(epListUrl(cid)).bodyAsText()
        val document = Jsoup.parse(html)
        val avList = document.select("#album-avlist-data")
        val data = JSONObject.parse(avList.first()!!.attr("value")) as JSONObject
        val epList = data.getJSONArray("epsodelist")
        return epList.map {
            it as JSONObject
            IQiyiEpisodeInfo(
                cid, it["tvId"].toString(), it["vid"].toString(), it["imageUrl"].toString(), it["duration"].let {
                    val dur = it.toString().split(":")
                    dur[0].toInt().minutes + dur[1].toInt().seconds
                }, it["subtitle"].toString(), it["description"].toString(), it["shortTitle"].toString().let {
                    it.filter { it in '0'..'9' }
                }, it["contentType"].toString() == "1"
            )
        }
    }

    override suspend fun getDanmuList(ep: IQiyiEpisodeInfo): List<Danmu.DanmuItem> {
        val ret = mutableListOf<Danmu.DanmuItem>()
        danmuUrls(ep.tvId, ep.duration).forEach { url ->
            val get = client.get(url)
            if (get.status == HttpStatusCode.OK) {
                val xmldata = get.bodyAsChannel().toInputStream().readAllBytes().zlibDecompress()
                val xml = document.newDocumentBuilder().parse(xmldata.toByteArray().inputStream())
                val danmus = xpath.evaluate(danmuPath, xml, XPathConstants.NODESET) as NodeList
                danmus.each { danmu ->
                    val data = danmu.childNodes.toMap()
                    ret.add(
                        Danmu.DanmuItem(
                            content = data["content"].toString(),
                            position = data["position"].toString().toInt(),
                            time = data["showTime"].toString().toLong(),
                            headUrl = (data["userInfo"] as Map<String, Any>)["senderAvatar"].toString(),
                            userName = (data["userInfo"] as Map<String, Any>)["name"].toString(),
                            style = Danmu.DanmuStyle(data["color"].toString().toInt(16))
                        )
                    )

                }
            }
        }
        return ret
    }

    fun NodeList.each(each: (Node) -> Unit) {
        for (current in 0 until length) {
            each(item(current))
        }
    }

    fun NodeList.toMap(): Map<String, Any> {
        val ret = mutableMapOf<String, Any>()
        each {
            ret[it.nodeName] = if (it.childNodes.length <= 1) {
                it.textContent
            } else {
                it.childNodes.toMap()
            }
        }
        return ret
    }

    fun Node.path(path: String): String {
        this.childNodes
        return (xpath.evaluate(path, this, XPathConstants.NODE) as Node).textContent
    }

    fun ByteArray.zlibDecompress(): String {
        val inflater = Inflater()
        val outputStream = ByteArrayOutputStream()

        return outputStream.use {
            val buffer = ByteArray(1024)

            inflater.setInput(this)

            var count = -1
            while (count != 0) {
                count = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }

            inflater.end()
            outputStream.toString("UTF-8")
        }
    }

    private fun danmuUrls(tvId: String, duration: Duration): List<String> {
        return IntRange(0, ceil(duration.inWholeSeconds / 300.0).toInt() + 1).map {
            "https://cmts.iqiyi.com/bullet/${
                tvId.substring(
                    tvId.length - 4,
                    tvId.length - 2
                )
            }/${tvId.substring(tvId.length - 2, tvId.length)}/${tvId}_300_$it.z"
        }

    }


    data class IQiyiEpisodeInfo(
        val cid: String, //剧集id
        val tvId: String, //视频id
        val vid: String, // ?
        val imageUrl: String, // 封面
        val duration: Duration, // 长度
        val title: String, // 标题
        val description: String, //  描述
        override val ep: String, //  集
        val trailer: Boolean,//  预告
    ) : Danmu.EpisodeInfo
}