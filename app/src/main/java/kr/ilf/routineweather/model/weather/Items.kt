package kr.ilf.routineweather.model.weather

import java.io.Serializable

data class Items<T>(
    val item: List<T>
): Serializable

