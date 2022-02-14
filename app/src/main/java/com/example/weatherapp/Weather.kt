package com.example.weatherapp

import java.time.LocalDateTime
import java.util.*

class Weather() {

    var id: Int = 0
    lateinit var description: String
    var temp: Double = 0.0
    var tempMin:Double = 0.0
    var tempMax:Double = 0.0
    var pressure: Double = 0.0
    var humidity:Double = 0.0
    var wind:Double = 0.0
    lateinit var cityName:String
    lateinit var sunrise: Calendar
    lateinit var sunset: Calendar
    lateinit var date: Calendar
    var timeZone = 0L

}