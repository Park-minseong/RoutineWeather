package kr.ilf.routineweather.model.dust

import java.io.Serializable

data class ResponseBody<T>(
    val dataType: String,
    val items: List<T>,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
): Serializable
