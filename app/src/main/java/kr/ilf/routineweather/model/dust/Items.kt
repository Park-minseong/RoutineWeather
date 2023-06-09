package kr.ilf.routineweather.model.dust

import kr.ilf.routineweather.model.weather.ResponseItem
import java.io.Serializable

data class Items<T>(
    val item: List<T>
): Serializable

