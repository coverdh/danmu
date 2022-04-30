/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.cover.danmu

import com.alibaba.fastjson.JSON
import com.cover.danmu.ASS.prefixZero
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import kotlin.math.floor
import kotlin.text.toByteArray


fun main() {
    val httpClient = HttpClient(CIO)
    val danmu = TencentVideo(httpClient)
    runBlocking {
        val min = 11
        val max = 18
        val episodeList = danmu.getEpisodeList("mzc00200v3lnbmd")
        val getDanmuEpList = episodeList.filter {
            val epIdx = try {
                it.ep.toInt()
            } catch (e: NumberFormatException) {
                it.ep.filter { it in '0'..'9' }.toString().toInt()
            }
            epIdx in min..max
        }
        val lineLimit = 5
        val fontSize = 55
        val height = 1080
        val width = 1920
        val delay = 5
        val moveTime = delay * 1.0f * width
        val oneWordTimeUsed = fontSize * 1.0 * delay
        val spaceCount = 3
        coroutineScope {
            getDanmuEpList.map {
                async {
                    val lineSlot = buildSlots(lineLimit)
                    val fileName =
                        "Who.Rules.The.World.2022.S01.EP${it.ep.prefixZero(2)}.HD1080P.X264.AAC.Mandarin.CHS.BDYS.ass"
                    val danmuList = danmu.getDanmuList(it)
                    val styleMap = mutableMapOf<Danmu.DanmuStyle, ASS.Style>()
                    var lineNumber = 0
                    val eventList = danmuList.sortedBy { it.time }.map {
                        lineNumber++
                        val lastTimeUsed = oneWordTimeUsed * (it.content.length + spaceCount)
                        val showTime = floor(moveTime + lastTimeUsed).toLong()
                        val slot = lineSlot[lineNumber % lineLimit]!!
                        val startTime = slot.tryUse(it.time * 1000, floor(lastTimeUsed).toLong(), showTime)
                        if (startTime != null) {
                            val style = styleMap.computeIfAbsent(it.style) {
                                ASS.Style(
                                    name = UUID.randomUUID().toString().replace("-", "").substring(0, 6),
                                    size = fontSize,
                                    font = "PingFang SC",
                                    primaryColor = if (it.color == -1) "FFFFFF".toInt(16) else it.color,
                                    outlineColor = "666666".toInt(16),
                                    outline = 1.0f,
                                    bold = false,
                                    borderStyle = ASS.Style.BorderStyle.BORDER_SHADOW
                                )
                            }
                            ASS.Event(
                                type = ASS.EventType.Dialogue,
                                start = startTime,
                                end = startTime + showTime,
                                text = it.content.textWrapper(),
                                name = it.userName.textWrapper(),
                                style = style.name,
                                effect = ASS.Effect(
                                    type = ASS.EffectType.Banner,
                                    delay = delay,
                                    leftToRight = false
                                ),
                                marginV = height - fontSize * (lineNumber % lineLimit + 2)
                            )
                        } else {
                            null
                        }
                    }.filterNotNull()


                    val output = File("/Volumes/Data/Video/TV/且试天下/" + fileName).outputStream().asOutput()
                    output.writeText(
                        """
[Script Info]
Title: Tencent Video Danmu
Original Script:  Tencent Video Danmu
ScriptType: v4.00+
Collisions: Normal
PlayResX: $width
PlayResY: $height
Timer: 100.0000
YCbCr Matrix: TV.601

[V4+ Styles]
${ASS.Style.header()}
${styleMap.values.joinToString("\n") { it.toString() }}
[Events]
${ASS.Event.header()}
${eventList.joinToString("\n") { it.toEventString() }}
                """.trimIndent()
                    )
                    output.flush()
                    output.close()
                }
            }.awaitAll()
        }
    }

}

private fun buildSlots(lineLimit: Int): MutableMap<Int, Slot> {
    val lineSlot = mutableMapOf<Int, Slot>()
    for (line in 0..lineLimit) {
        lineSlot[line] = Slot()
    }
    return lineSlot
}

val emoji = mapOf(
    "星星眼" to "/︎星星眼",//""🤩",
    "笑哭" to "/笑哭︎",// "😂",
    "生气" to "︎/生气",//"😤",
    "哭" to "/哭",//"😭",
    "吃醋" to "/吃醋",//"😣",
    "喜" to "/喜",//"😍",
)

fun String.textWrapper(): String {
    return this.replace("\n", "\\n").replace(",", "，").replace(Regex("\\[([\\u4e00-\\u9fa5]+)\\]")) {

        if (it.value.length <= 3) {
            it.value
        } else {
            emoji[it.value.substring(1, it.value.length - 1)] ?: emoji[it.value.substring(3, it.value.length - 1)]
            ?: it.value
        }
    }
}


class Slot {
    private var canUseTime: Long = 0

    /**
     *   尝试使用该槽，返回使用开始时间，如果返回 null 丢弃该弹幕
     */
    fun tryUse(startTime: Long, time: Long, showTime: Long): Long? {
        if (canUseTime < startTime) {
            canUseTime = startTime
        }
        if (canUseTime > startTime + showTime * 1.1) {
            return null
        }
        val s = canUseTime
        canUseTime += time
        return s
    }
}