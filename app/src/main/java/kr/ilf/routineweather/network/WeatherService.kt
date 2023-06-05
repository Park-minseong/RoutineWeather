package kr.ilf.routineweather.network

import kr.ilf.routineweather.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String?,
        @Query("appid") appid: String,
    ): Call<WeatherResponse>


    @GET("getUltraSrtNcst")
    fun getUltraSrtNcst(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("dataType") dataType: String,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int,
    ): Call<WeatherResponse>

    @GET("getUltraSrtFcst")
    fun getUltraSrtFcst(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("dataType") dataType: String,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int,
    ): Call<WeatherResponse>

    @GET("getVilageFcst")
    fun getVilageFcst(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("dataType") dataType: String,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int,
    ): Call<WeatherResponse>

    @GET("getUltraSrtNcst?ServiceKey=kS3LFJtXMu8jwui1luQ%2Fc5W2TWBrWX3BXa9jxOYO6s6bF3%2Bfp80rND5ux8MvizXc4BrqltuFIVM74BhzM%2FAMPQ%3D%3D&pageNo=1&numOfRows=10&dataType=JSON&base_date=20230605&base_time=1400&nx=61&ny=125")
    fun doTest(): Call<WeatherResponse>

}