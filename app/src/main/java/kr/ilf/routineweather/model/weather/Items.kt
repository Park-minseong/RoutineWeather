package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class Items(
    val item: List<ResponseItem>
): Serializable

