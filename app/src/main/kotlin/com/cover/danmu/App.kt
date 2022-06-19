/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.cover.danmu

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
import kotlin.math.max
import kotlin.math.min

fun main() {
    val httpClient = HttpClient(CIO)
    runBlocking {
        // 梦华录
//        TencentVideo(httpClient).getAndWrite("mzc00200p51jpn7", 25, 26) {
//            val dir = "/Volumes/Video/TV/A.Dream.of.Splendor"
//            val file = "A.Dream.of.Splendor.2022.S01.E${it.ep.prefixZero(2)}.HD1080P.X264.AAC.Mandarin.CHS.BDYS.ass"
//            "$dir/$file"
//        }
        // 破事精英
        IQiyi(httpClient).getAndWrite("a_1i8xk54mggd",1,4) {
            val dir = "/Volumes/Video/TV/The.Lord.of.Losers"
            val file = "The.Lord.of.Losers.2022.S01.E${it.ep.prefixZero(2)}.HD1080P.X264.AAC.Mandarin.CHS.BDYS.ass"
            "$dir/$file"
        }

    }
}

suspend fun <T : Danmu.EpisodeInfo> Danmu<T>.getAndWrite(sid: String, from: Int, to: Int, fileName: (T) -> String) {
    val min = min(from, to)
    val max = max(from, to)
    val episodeList = this.getEpisodeList(sid)
    val getDanmuEpList = episodeList.filter {
        val epIdx = try {
            it.ep.toInt()
        } catch (e: NumberFormatException) {
            it.ep.filter { it in '0'..'9' }.toString().toInt()
        }
        epIdx in min..max
    }
    val fontSize = 55
    val width = 1920
    val delay = 5
    val config = DanmuConfig(
        5, fontSize * 1.0 * delay, 3, delay * 1.0f * width, 55, delay, 1080, width
    )
    coroutineScope {
        getDanmuEpList.eachRow(5) {
            it.map {
                async {
                    fetchAndWrite(it, this@getAndWrite, config, fileName)
                }
            }.awaitAll()
        }
    }
}


/**
 *  分组处理
 */
suspend fun <T> List<T>.eachRow(colCount: Int, execute: suspend (List<T>) -> Unit) {
    mapIndexed { index, value -> index to value }
        .groupBy { it.first / colCount }.mapValues { it.value.map { it.second } }.forEach {
            execute(it.value)
        }
}

data class DanmuConfig(
    val lineLimit: Int,
    val oneWordTimeUsed: Double,
    val spaceCount: Int,
    val moveTime: Float,
    val fontSize: Int,
    val delay: Int,
    val height: Int,
    val width: Int,
)

private suspend fun <T : Danmu.EpisodeInfo> fetchAndWrite(
    ep: T,
    danmu: Danmu<T>,
    config: DanmuConfig,
    fileName: (T) -> String
) {
    val lineSlot = buildSlots(config.lineLimit)

    val danmuList = danmu.getDanmuList(ep)
    val styleMap = mutableMapOf<Danmu.DanmuStyle, ASS.Style>()
    var lineNumber = 0
    val eventList = danmuList.sortedBy { it.time }.mapNotNull {
        lineNumber++
        val lastTimeUsed = config.oneWordTimeUsed * (it.content.length + config.spaceCount)
        val showTime = floor(config.moveTime + lastTimeUsed).toLong()
        val slot = lineSlot[lineNumber % config.lineLimit]!!
        val startTime = slot.tryUse(it.time * 1000, floor(lastTimeUsed).toLong(), showTime)
        if (startTime != null) {
            val style = styleMap.computeIfAbsent(it.style) {
                ASS.Style(
                    name = UUID.randomUUID().toString().replace("-", "").substring(0, 6),
                    size = config.fontSize,
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
                    type = ASS.EffectType.Banner, delay = config.delay, leftToRight = false
                ),
                marginV = config.height - config.fontSize * (lineNumber % config.lineLimit + 2)
            )
        } else {
            null
        }
    }


    val output = File(fileName(ep)).outputStream().asOutput()
    output.writeText(
        """
[Script Info]
Title: Tencent Video Danmu
Original Script:  Tencent Video Danmu
ScriptType: v4.00+
Collisions: Normal
PlayResX: ${config.width}
PlayResY: ${config.height}
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
    return this.replace("\n", "\\n").replace(",", "，")
//        .replace(Regex("\\[([\\u4e00-\\u9fa5]+)\\]")) {
//
//            if (it.value.length <= 3) {
//                it.value
//            } else {
//                emoji[it.value.substring(1, it.value.length - 1)] ?: emoji[it.value.substring(3, it.value.length - 1)]
//                ?: it.value
//            }
//        }
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