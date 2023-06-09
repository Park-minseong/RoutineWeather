package kr.ilf.routineweather.network

import kr.ilf.routineweather.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherService {
    // openWeatherMap api
    @GET("2.5/weather")
    fun getOpenWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String?,
        @Query("appid") appid: String,
    ): Call<WeatherResponse>

    // 기상청 Api
    @GET("1360000/VilageFcstInfoService_2.0/{path}")
    fun getOpenApiWeather(
        @Path("path") path: String,
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("dataType") dataType: String,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int,
    ): Call<WeatherResponse>

    // 미세먼지 api
    @GET("B552584/ArpltnInforInqireSvc/getMinuDustFrcstDspth")
    fun getCtprvnRltmMesureDnsty(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("returnType") dataType: String,
        @Query("sidoName") sidoName: String,
        @Query("ver") ver: String,
    )
}