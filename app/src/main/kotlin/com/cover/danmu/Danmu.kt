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
        val position: Int,
        val style: DanmuStyle
    )

    data class DanmuStyle(
        val color: Int,
        val gradient: Pair<Int, Int>? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            return if (other is DanmuStyle) {
                gradient == other.gradient && color == other.color
            } else {
                false
            }
        }

        override fun hashCode(): Int {
            return color.hashCode() * 32 + gradient.hashCode()
        }
    }


}