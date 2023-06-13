package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class ResponseBody<T>(
    val dataType: String,
    val items: Items<T>,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
): Serializable
