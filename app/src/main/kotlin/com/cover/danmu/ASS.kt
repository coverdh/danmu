package com.cover.danmu

object ASS {
    class Style(
        val name: String,
        val font: String,
        val size: Int,
        val primaryColor: Int = 0, // 主体颜色
        val secondaryColor: Int = 0, //次要颜色
        val outlineColor: Int = 0,
        val backColor: Int = 0,
        val bold: Boolean = false, // 粗体
        val italic: Boolean = false,//   斜体
        val borderStyle: BorderStyle = BorderStyle.NONE,
        val outline: Float = 0F,
        val shadow: Float = 0F,
        val alignment: Int = 0,
        val marginL: Int = 0,
        val marginR: Int = 0,
        val marginV: Int = 0,
        val encoding: Encode = Encode.DEFAULT,
    ) {
        override fun toString(): String {
            return "Style: $name, $font, $size, $primaryColor, $secondaryColor, $outlineColor, $backColor, ${bold.toInt()}, ${italic.toInt()}, ${borderStyle.id}, $outline, $shadow, $alignment, $marginL, $marginR, $marginV, ${encoding.id}"
        }


        enum class BorderStyle(val id: Int) {
            NONE(0),
            BORDER_SHADOW(1), // 边框 影音
            BORDER(3)//  不透明底框
        }

        enum class Encode(val id: Int) {
            ANSI(0),
            DEFAULT(1),
            JEP(128),
            ZH_CN(134),
            ZH_HK(136)
        }

        companion object {
            fun header(): String =
                "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding"
        }
    }


    class Event(
        val type: EventType,
        val start: Long,
        val end: Long,
        val text: String,
        val style: String,
        val effect: Effect? = null,
        val marked: Boolean = false,
        val name: String = "Unknown",
        val marginLeft: Int = 0,
        val marginRight: Int = 0,
        val marginV: Int = 0,
    ) {
        fun toEventString() =
            "$type: Marked=${marked.toInt()},${start.toTimeDuration()},${end.toTimeDuration()},$style,$name, ${
                marginLeft.prefixZero(
                    4
                )
            }, ${marginRight.prefixZero(4)}, ${marginV.prefixZero(0)}, ${effect ?: ""}, $text"

        companion object {
            fun header(): String =
                "Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text"
        }
    }


    class Effect(
        val type: EffectType,
        val delay: Int,
        val y1: Int = 0,
        val y2: Int = 0,
        val leftToRight: Boolean = false
    ) {
        override fun toString(): String {
            return when (type) {
                EffectType.ScrollUp -> "${type.showName};$y1;$y2;$delay"
                EffectType.ScrollDown -> "${type.showName};$y1;$y2;$delay"
                EffectType.Banner -> "${type.showName};$delay${if (leftToRight) ";1" else ";0"}"
            }
        }
    }

    enum class EffectType(
        val showName: String,
    ) {
        ScrollUp("Scroll up"),
        ScrollDown("Scroll down"),
        Banner("Banner");
    }

    enum class EventType {
        Dialogue,
        Comment,
        Picture,
        Command;
    }

    private fun Boolean.toInt() = if (this) 1 else 0

    private fun Long.toTimeDuration() =
        "${this / 1000 / 60 / 60}:${(this / 1000 / 60) % 60}:${(this / 1000) % 60}.${(this / 10) % 100}"

    fun Any.prefixZero(size: Int): String {
        var ret = this.toString()
        while (ret.length < size) {
            ret = "0" + ret
        }
        return ret
    }
}