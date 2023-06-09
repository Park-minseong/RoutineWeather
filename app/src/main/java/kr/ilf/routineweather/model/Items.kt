package kr.ilf.routineweather.model

import java.io.Serializable

data class Items(
    val item: List<ResponseItem>
): Serializable

