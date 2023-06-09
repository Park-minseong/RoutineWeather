package kr.ilf.routineweather.model

import java.io.Serializable

data class ResponseBody(
    val dataType: String,
    val items: Items,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
): Serializable
