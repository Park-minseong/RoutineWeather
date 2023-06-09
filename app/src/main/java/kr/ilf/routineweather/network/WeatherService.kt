package kr.ilf.routineweather.network

import kr.ilf.routineweather.model.dust.DustItem
import kr.ilf.routineweather.model.dust.DustResponse
import kr.ilf.routineweather.model.dust.StationItem
import kr.ilf.routineweather.model.weather.WeatherResponse
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

    // 미세먼지 api(측정소별 데이터)
    @GET("B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty")
    fun getMsrstnAcctoRltmMesureDnsty(
        @Query("serviceKey") serviceKey: String,
        @Query("returnType") dataType: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("stationName") stationName: String,
        @Query("dataTerm") dataTerm: String = "DAILY",
        @Query("ver") ver: String = "1.4",
    ):Call<DustResponse<DustItem>>

    // 대기오염 근접측정소 api
    @GET("B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList")
    fun getNearbyMsrstnList(
        @Query("serviceKey") serviceKey: String,
        @Query("returnType") returnType: String,
        @Query("tmX") tmX: Double,
        @Query("tmY") tmY: Double,
    ):Call<DustResponse<StationItem>>
}