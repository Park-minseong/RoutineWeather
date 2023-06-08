package kr.ilf.routineweather

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kr.ilf.routineweather.databinding.ActivityMainBinding
import kr.ilf.routineweather.model.UltraSrtFcst
import kr.ilf.routineweather.model.UltraSrtNcst
import kr.ilf.routineweather.model.WeatherResponse
import kr.ilf.routineweather.network.WeatherService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mSharedPreferences: SharedPreferences

    private var isCompletedCallUltraSrtNcst = true
    private var isCompletedCallUltraSrtFcst = true
    private var isCompletedCallVilageFcst = true

    private var ultraSrtNcstBaseTime = "0000"
    private var ultraSrtFcstBaseTime = "0030"
    private var vilageFcstBaseTime = "0000"
    private var currentDate = "000000"

    private var binding: ActivityMainBinding? = null

    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        showCustomProgressDialog()

        isCompletedCallUltraSrtNcst = false
        isCompletedCallUltraSrtFcst = false
        isCompletedCallVilageFcst = false

        if (!isLocationEnabled()) {
            Toast.makeText(
                this@MainActivity,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

            startActivity(intent)
        } else {
            Dexter.withContext(this@MainActivity).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        requestLocationData()
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            this@MainActivity,
                            "You have denied location permission. Please enable them as it is mandatory for the app to work.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                requestLocationData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        showCustomProgressDialog()

        val mLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdates(1)
            .build()

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location = locationResult.lastLocation!!

            val latitude = mLastLocation.latitude
            val longitude = mLastLocation.longitude

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            // HTTP요청 Logging용 클라이언트 선언
            val interceptor = HttpLoggingInterceptor()

            if (BuildConfig.DEBUG) {
                interceptor.level = HttpLoggingInterceptor.Level.BODY
            } else {
                interceptor.level = HttpLoggingInterceptor.Level.NONE
            }

            val okHttpClient = OkHttpClient().newBuilder()
                .addNetworkInterceptor(interceptor)
                .build()
            // HTTP요청 Logging용 클라이언트 선언 끝

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)

            ultraSrtNcstBaseTime = getBaseTime("ultraSrtNcst")
            Log.d("baseTime", ultraSrtNcstBaseTime)
            ultraSrtFcstBaseTime = getBaseTime("ultraSrtFcst")
            Log.d("baseTime", ultraSrtFcstBaseTime)
            vilageFcstBaseTime = getBaseTime("vilageFcst")
            Log.d("baseTime", vilageFcstBaseTime)

            currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            val ultraSrtNcstCall: Call<WeatherResponse> = service.getUltraSrtNcst(
                Constants.OPENAPI_API_KEY,
                1,
                10,
                "JSON",
                currentDate,
                ultraSrtNcstBaseTime,
                61,
                125
            )

            val ultraSrtFcstCall: Call<WeatherResponse> = service.getUltraSrtFcst(
                Constants.OPENAPI_API_KEY,
                1,
                60,
                "JSON",
                currentDate,
                ultraSrtFcstBaseTime,
                61,
                125
            )

            val vilageFcstCall: Call<WeatherResponse> = service.getVilageFcst(
                Constants.OPENAPI_API_KEY,
                1,
                60,
                "JSON",
                currentDate,
                vilageFcstBaseTime,
                61,
                125
            )

            isCompletedCallUltraSrtNcst = false
            isCompletedCallUltraSrtFcst = false
            isCompletedCallVilageFcst = false

            ultraSrtNcstCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        hideProgressDialog()

                        val responseData: WeatherResponse? = response.body()

                        val baseTime = responseData?.response?.body?.items?.item?.get(0)?.baseTime
                        val baseDate = responseData?.response?.body?.items?.item?.get(0)?.baseDate
                        var pty: String? = null
                        var t1h: String? = null
                        var rn1: String? = null
                        var reh: String? = null

                        responseData?.response?.body?.items?.item?.forEach {
                            Log.d("inForeach:", "OK")
                            when (it.category) {
                                "PTY" -> {
                                    pty = it.obsrValue!!
                                }

                                "T1H" -> {
                                    t1h = it.obsrValue!!
                                }

                                "RN1" -> {
                                    rn1 = it.obsrValue!!
                                }

                                "REH" -> {
                                    reh = it.obsrValue!!
                                }
                            }
                        }

                        val ultraSrtNcst =
                            UltraSrtNcst(baseTime!!, baseDate!!, pty!!, t1h!!, rn1!!, reh!!)

                        val ultraSrtNcstJSonString = Gson().toJson(ultraSrtNcst)

                        Log.d("ultraSrtNcstJSonString:", ultraSrtNcstJSonString)

                        val editor = mSharedPreferences.edit()
                        editor.putString(
                            Constants.WEATHER_RESPONSE_DATA_ULTRA_NCST,
                            ultraSrtNcstJSonString
                        )
                        editor.apply()

                        isCompletedCallUltraSrtNcst = true

                        setupUI()

                        Log.i("Response Result", "$responseData")
                    } else {
                        val rc = response.code()

                        when (rc) {
                            400 ->
                                Log.e("Error 400.", "Bad BadConnection")

                            404 ->
                                Log.e("Error 404.", "Not Found")

                            else ->
                                Log.e("Error.", "Generic Error")

                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressDialog()
                    Log.e("Errorrrrr.", t.message.toString())
                }
            })

            ultraSrtFcstCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseData = response.body()?.response?.body?.items?.item
                        val ultraSrtFcst = LinkedHashMap<String, HashMap<String, String>>()

                        responseData?.forEach {
                            if (!ultraSrtFcst.containsKey(it.fcstTime)) {
                                ultraSrtFcst[it.fcstTime!!] = HashMap()
                                ultraSrtFcst[it.fcstTime]?.put("fcstDate", it.fcstDate!!)
                                ultraSrtFcst[it.fcstTime]?.put("fcstTime", it.fcstTime!!)
                            }

                            ultraSrtFcst[it.fcstTime]?.put(it.category, it.fcstValue!!)
                        }

                        val ultraSrtFcstJSonString = Gson().toJson(ultraSrtFcst)

                        Log.d("ultraSrtFcst: ", ultraSrtFcstJSonString)

                        val editor = mSharedPreferences.edit()
                        editor.putString(
                            Constants.WEATHER_RESPONSE_DATA_ULTRA_FCST,
                            ultraSrtFcstJSonString
                        )
                        editor.apply()

                        isCompletedCallUltraSrtFcst = true

                        setupUI()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressDialog()
                    Log.e("Errorrrrr.", t.message.toString())
                }
            })

            vilageFcstCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseData = response.body()?.response?.body?.items?.item
                        val VilageFcst = LinkedHashMap<String, HashMap<String, String>>()

                        responseData?.forEach {
                            val key = it.fcstDate + it.fcstTime

                            if (!VilageFcst.containsKey(key)) {
                                VilageFcst[key] = HashMap()
                                VilageFcst[key]?.put("fcstDate", it.fcstDate!!)
                                VilageFcst[key]?.put("fcstTime", it.fcstTime!!)
                            }

                            VilageFcst[key]?.put(it.category, it.fcstValue!!)
                        }

                        val vilageFcstJSonString = Gson().toJson(VilageFcst)

                        Log.d("vilageFcst: ", vilageFcstJSonString)

                        val editor = mSharedPreferences.edit()
                        editor.putString(
                            Constants.WEATHER_RESPONSE_DATA_VILAGE_FCST,
                            vilageFcstJSonString
                        )
                        editor.apply()

                        isCompletedCallVilageFcst = true

                        setupUI()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressDialog()
                    Log.e("Errorrrrr.", t.message.toString())
                }
            })
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun showCustomProgressDialog() {
        if (mProgressDialog == null)
            mProgressDialog = Dialog(this)

        if (!mProgressDialog!!.isShowing) {
            mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

            mProgressDialog!!.show()
        }
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun setupUI() {

        if (isCompletedCallUltraSrtNcst &&
            isCompletedCallUltraSrtFcst &&
            isCompletedCallVilageFcst
        ) {

            val ultraSrtNcstJsonString =
                mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_ULTRA_NCST, "")

            val ultraSrtFcstJsonString =
                mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_ULTRA_FCST, "")

            val vilageFcstJsonString =
                mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA_VILAGE_FCST, "")

            val ultraSrtNcst = Gson().fromJson(ultraSrtNcstJsonString, UltraSrtNcst::class.java)
            val ultraSrtFcstsMap =
                Gson().fromJson(
                    ultraSrtFcstJsonString,
                    LinkedHashMap<String, Map<String, String>>()::class.java
                )
            val vilageFcsts =
                Gson().fromJson(
                    vilageFcstJsonString, LinkedHashMap<String, String>()::class.java
                )

            var count = 0

            var weatherCode: String? = null

            val ultraSrtFcsts = ArrayList<UltraSrtFcst>()

            ultraSrtFcstsMap.forEach { (key, ultraSrtFcstMap) ->
                if (count == 0) {
                    when (ultraSrtNcst.pty) {
                        "1", "4", "5" -> {
                            binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.rain))
                        }

                        "2", "3", "6", "7" -> {
                            binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.snowflake))
                        }

                        else -> {
                            if (ultraSrtFcstMap["SKY"] == "4") {
                                binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.cloud))
                            } else {
                                binding?.ivMain?.setImageDrawable(getDrawable(R.drawable.sunny))
                            }
                        }
                    }
                    ++count
                }

                val ultraSrtFcst = UltraSrtFcst(
                    null,
                    null,
                    ultraSrtFcstMap["fcstTime"]!!,
                    ultraSrtFcstMap["fcstDate"]!!,
                    ultraSrtFcstMap["PTY"]!!,
                    ultraSrtFcstMap["T1H"]!!,
                    ultraSrtFcstMap["SKY"]!!
                )

                ultraSrtFcsts.add(ultraSrtFcst)
            }

            binding?.rvSrtFcst?.adapter = SrtFcstAdapter(this, ultraSrtFcsts)

            binding?.tvTemp?.text = ultraSrtNcst.t1h + " °C"
            binding?.tvHumidity?.text = ultraSrtNcst.reh + " %"
            binding?.tvPrecipitation?.text = ultraSrtNcst.rn1 + "mm"
//
//                when (weatherList.weather[i].icon) {
//                    "01d" -> binding?.ivMain?.setImageResource(R.drawable.sunny)
//                    "02d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "03d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "04d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "04n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "10d" -> binding?.ivMain?.setImageResource(R.drawable.rain)
//                    "11d" -> binding?.ivMain?.setImageResource(R.drawable.storm)
//                    "13d" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
//                    "01n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "02n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "03n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "10n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
//                    "11n" -> binding?.ivMain?.setImageResource(R.drawable.rain)
//                    "13n" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
//                }


        }
    }

    private fun getUnit(): String {
        val value: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            application.resources.configuration.locales.get(0).toString()
        else
            application.resources.configuration.locale.toString()

        var unit = " °C"
        if ("US" == value || "LR" == value || "MM" == value) {
            unit = " °F"
        }
        return unit
    }

    private fun unixTime(timex: Long): String {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(date)
    }

    private fun getBaseTime(apiType: String): String {

        var currentHour = LocalDateTime.now().hour
        val currentMinute = LocalDateTime.now().minute

        Log.d("currentHour", currentHour.toString())


        return when (apiType) {
            "ultraSrtNcst" -> {
                if (currentMinute <= 31) {
                    currentHour--
                    if (currentHour == -1) {
                        return "2300"
                    } else if (currentHour < 10) {
                        return "0${currentHour}00"
                    } else {
                        return "${currentHour}00"
                    }
                } else {
                    if (currentHour < 10) {
                        return "0${currentHour}00"
                    } else {
                        return "${currentHour}00"
                    }
                }
            }

            "ultraSrtFcst" -> {
                if (currentMinute <= 31) {
                    currentHour--
                    if (currentHour == -1) {
                        return "2330"
                    } else if (currentHour < 10) {
                        return "0${currentHour}30"
                    } else {
                        return "${currentHour}30"
                    }
                } else {
                    if (currentHour < 10) {
                        return "0${currentHour}30"
                    } else {
                        return "${currentHour}30"
                    }
                }
            }

            "vilageFcst" -> {
                when (currentHour) {
                    in 2..4 -> "0200"
                    in 5..7 -> "0500"
                    in 8..10 -> "0800"
                    in 11..13 -> "1100"
                    in 14..16 -> "1400"
                    in 17..19 -> "1700"
                    in 20..22 -> "2000"
                    else -> "2300"
                }
            }

            else -> "0200"

        }
    }
}
