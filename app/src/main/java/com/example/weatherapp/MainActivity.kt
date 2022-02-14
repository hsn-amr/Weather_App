package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.sql.Timestamp
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var cityAndCountry: TextView
    lateinit var lastUpdate: TextView

    lateinit var description: TextView
    lateinit var temperature: TextView
    lateinit var lowTemperature: TextView
    lateinit var highTemperature: TextView

    lateinit var sunrise: TextView
    lateinit var sunset: TextView
    lateinit var wind: TextView
    lateinit var pressure: TextView
    lateinit var humidity: TextView
    lateinit var refresh: LinearLayout

    var defaultZipCode = "10001"
    var defaultUnit = "metric"
    val APIKey = "b4d2ede8c55b5638b6dd94cf4db8c1c8"
    private lateinit var progressBar: ProgressBar
    private lateinit var cm: ConnectivityManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val extras = intent.extras
        if (extras != null) {
            defaultZipCode = extras.getString("zipCode").toString()
        }

        cityAndCountry = findViewById(R.id.tvCityAndCountry)
        lastUpdate = findViewById(R.id.tvLastUpdate)

        description = findViewById(R.id.tvDescription)
        temperature = findViewById(R.id.tvTemperature)
        temperature.setOnClickListener {
            if(defaultUnit == "metric"){
                defaultUnit = "imperial"
                requestApi(defaultUnit)
            }else{
                defaultUnit = "metric"
                requestApi(defaultUnit)
            }
        }
        lowTemperature = findViewById(R.id.tvLowTemerature)
        highTemperature = findViewById(R.id.tvHighTemerature)

        sunrise = findViewById(R.id.tvSunriseTime)
        sunset = findViewById(R.id.tvSunsetTime)
        wind = findViewById(R.id.tvWindSpeed)
        pressure = findViewById(R.id.tvPressureValue)
        humidity = findViewById(R.id.tvHumidityValue)
        refresh = findViewById(R.id.llRefresh)
        refresh.setOnClickListener {
            showProgressDialog()
            requestApi(defaultUnit)
        }

        progressBar = findViewById(R.id.progressBar)
        showProgressDialog()
        requestApi(defaultUnit)

    }

    private fun showProgressDialog(){
        progressBar.visibility = View.VISIBLE
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    private fun removeProgressDialog(){
        progressBar.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun requestApi(unit: String){

        cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        if(activeNetwork?.isConnectedOrConnecting == true){
            CoroutineScope(Dispatchers.IO).launch {
                val data = async {
                    fetchWeatherData(unit)
                }.await()

                if(data.isNotEmpty()){
                    updateWeatherDate(data)
                }else{
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MainActivity, "invalid zip code", Toast.LENGTH_LONG).show()
                        defaultZipCode = "10001"
                        requestApi(defaultUnit)
                    }
                }
            }
        }else{
            removeProgressDialog()
            Toast.makeText(this, "Please Connect To Internet First", Toast.LENGTH_LONG).show()
        }

    }

    fun fetchWeatherData(unit:String): String{
        var response = ""
        try {
            response = URL(createWeatherUrl(unit)).readText(Charsets.UTF_8)
        }catch (e:Exception){
            print("Error $e")
        }
        return response
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateWeatherDate(data:String){

        val weatherObject = Weather()
        val jsonObject = JSONObject(data)

        val weather = jsonObject.getJSONArray("weather").getJSONObject(0)
        weatherObject.id = weather.getInt("id")
        weatherObject.description = weather.getString("description")

        val main = jsonObject.getJSONObject("main")
        weatherObject.temp = main.getDouble("temp")
        weatherObject.tempMin = main.getDouble("temp_min")
        weatherObject.tempMax = main.getDouble("temp_max")
        weatherObject.pressure = main.getDouble("pressure")
        weatherObject.humidity = main.getDouble("humidity")

        weatherObject.wind = jsonObject.getJSONObject("wind").getDouble("speed")

        weatherObject.timeZone = jsonObject.getLong("timezone")
        Log.e("TAG","${weatherObject.timeZone}")
        weatherObject.date = convertTimestampToDatetime(jsonObject.getLong("dt"), weatherObject.timeZone)

        val sys = jsonObject.getJSONObject("sys")
        weatherObject.sunrise = convertTimestampToDatetime(sys.getLong("sunrise"), weatherObject.timeZone)
        weatherObject.sunset = convertTimestampToDatetime(sys.getLong("sunset"), weatherObject.timeZone)

        weatherObject.cityName = jsonObject.getString("name")

        withContext(Dispatchers.Main){
            cityAndCountry.text = "${weatherObject.cityName}, US"
            val year = weatherObject.date.get(Calendar.YEAR).toString()
            val month = (weatherObject.date.get(Calendar.MONTH) + 1).toString()
            val day = weatherObject.date.get(Calendar.DAY_OF_MONTH).toString()
            val hour = from24To12(weatherObject.date.get(Calendar.HOUR_OF_DAY))
            val minute = getMinuteWithZero(weatherObject.date.get(Calendar.MINUTE))
            val amORpmDay = getAmOrPm(weatherObject.date)
            lastUpdate.text =  getString(R.string.last_update, year, month, day, hour, minute, amORpmDay)

            description.text = weatherObject.description
            temperature.text = "${weatherObject.temp}${checkTheUnit()}"
            lowTemperature.text = "Low:${weatherObject.tempMin}${checkTheUnit()}"
            highTemperature.text = "High:${weatherObject.tempMax}${checkTheUnit()}"

            val sunriseHour = from24To12(weatherObject.sunrise.get(Calendar.HOUR_OF_DAY))
            val sunriseMinute = getMinuteWithZero(weatherObject.sunrise.get(Calendar.MINUTE))
            val amORpmSunrise = getAmOrPm(weatherObject.sunrise)
            sunrise.text = getString(R.string.hour_minute_am_or_pm, sunriseHour, sunriseMinute, amORpmSunrise)

            val sunsetHour = from24To12(weatherObject.sunset.get(Calendar.HOUR_OF_DAY))
            val sunsetMinute = getMinuteWithZero(weatherObject.sunset.get(Calendar.MINUTE))
            val amORpmSunset = getAmOrPm(weatherObject.sunset)
            sunset.text = getString(R.string.hour_minute_am_or_pm, sunsetHour, sunsetMinute, amORpmSunset)
            wind.text = weatherObject.wind.toString()
            pressure.text = weatherObject.pressure.toString()
            humidity.text = weatherObject.humidity.toString()
            removeProgressDialog()
        }

    }

    private fun createWeatherUrl(unit: String):String{
        return "https://api.openweathermap.org/data/2.5/weather?zip=$defaultZipCode&units=$unit&appid=$APIKey"
    }

    private fun checkTheUnit(): String {
        return if(defaultUnit == "metric") "C"
        else "F"
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    fun convertTimestampToDatetime(num: Long, cityTimeZone: Long): Calendar {
        val localTimeZone = TimeZone.getDefault().rawOffset
        val sdf = Timestamp(((num + cityTimeZone) * 1000L) - localTimeZone)
        val calendar = Calendar.getInstance()
        calendar.time = Date(sdf.time)
        return calendar
    }

    private fun getAmOrPm(calendar: Calendar): String{
        return if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    }

    private fun from24To12(hour: Int):String{
        return if(hour < 10 && hour!=0) "0${hour}"
        else "${hour-12}"
    }

    private fun getMinuteWithZero(minute: Int): String{
        return if(minute < 10 && minute!=0) "0${minute}"
        else "$minute"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = Intent(this, SelectNewCity::class.java)
        startActivity(intent)
        return super.onOptionsItemSelected(item)
    }
}