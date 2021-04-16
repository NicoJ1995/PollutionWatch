package com.nju95.pollution

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.json.JSONObject
import org.w3c.dom.Text
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {
    val APIkey = "c3d963fd0f08989565d6053174f271a74c287e33";
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    val PERMISSION_ID = 1010

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermission()
        getLocation()

        pollutionGet().execute()

    }


    //check if we have the necessary permissions
    private fun checkPermission(): Boolean
    {
        if(
            ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ){
            return true
        }

        return false
    }

    //request permission if user has not given permission
    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    //check if location is enabled
    private fun isLocationEnabled():Boolean{
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //get user location
    private fun getLocation()
    {
        if(checkPermission()){
            if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener {task->
                    var location:Location? = task.result
                    if(location == null){
                        newLocationData()
                    }else{
                        findViewById<TextView>(R.id.location).text = getCityName(location.latitude,location.longitude)
                    }
                }
            }else{
                Toast.makeText(this,"Please authorize localization services",Toast.LENGTH_SHORT).show()
            }
        }else{
            requestPermission()
        }
    }

    //get new location data if changed
    private fun newLocationData(){
        var locationRequest =  LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest,locationCallback, Looper.myLooper()
        )
    }

    //update if changed
    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            var lastLocation: Location = locationResult.lastLocation
            findViewById<TextView>(R.id.location).text = getCityName(lastLocation.latitude,lastLocation.longitude)
        }
    }

    private fun getCityName(lat: Double,long: Double):String{
        var cityName:String = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var Address = geoCoder.getFromLocation(lat,long,3)

        cityName = Address.get(0).locality

        return cityName
    }

    inner class pollutionGet(): AsyncTask<String, Void, String>()
    {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loading).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorMsg).visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            var response:String?
            try {
                response = URL("https://api.waqi.info/feed/here/?token=$APIkey").readText(Charsets.UTF_8)
            }catch (e: Exception){
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            try {
                val json = JSONObject(result)
                val data = json.getJSONObject("data")
                val iaqi = data.getJSONObject("iaqi")
                val pm25 = iaqi.getJSONObject("pm25")
                val pm10 = iaqi.getJSONObject("pm10")
                val no2 = iaqi.getJSONObject("no2")
                val so2 = iaqi.getJSONObject("so2")
                val o3 = iaqi.getJSONObject("o3")

                val aqival = "Air Quality Index (AQI) of " + data.getDouble("aqi").toBigDecimal().toPlainString()
                val pm25val = pm25.getDouble("v").toBigDecimal().toPlainString()
                val pm10val = pm10.getDouble("v").toBigDecimal().toPlainString()
                val no2val = no2.getDouble("v").toBigDecimal().toPlainString()
                val so2val = so2.getDouble("v").toBigDecimal().toPlainString()
                val o3val= o3.getDouble("v").toBigDecimal().toPlainString()

                val status:String?
                val aqi = data.getDouble("aqi")

                if(aqi<=50)
                {
                    status = "Good"
                    findViewById<ImageView>(R.id.img).setImageResource(R.drawable.happy)
                }
                else if(aqi>50 && aqi<=100)
                {
                    status = "Moderate"
                    findViewById<ImageView>(R.id.img).setImageResource(R.drawable.neutral)
                }
                else if(aqi>101 && aqi<=200)
                {
                    status = "Bad"
                    findViewById<ImageView>(R.id.img).setImageResource(R.drawable.sad)
                }
                else if(aqi>200 && aqi<=300)
                {
                    status = "Very bad"
                    findViewById<ImageView>(R.id.img).setImageResource(R.drawable.upset)
                }
                else
                {
                    status = "Hazardous"
                    findViewById<ImageView>(R.id.img).setImageResource(R.drawable.dead)
                }

                findViewById<TextView>(R.id.status).text = status
                findViewById<TextView>(R.id.aqi).text = aqival
                findViewById<TextView>(R.id.pm10).text = pm10val
                findViewById<TextView>(R.id.pm25).text = pm25val
                findViewById<TextView>(R.id.no2).text = no2val
                findViewById<TextView>(R.id.so2).text = so2val
                findViewById<TextView>(R.id.o3).text = o3val

                findViewById<ProgressBar>(R.id.loading).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            }
            catch (e: java.lang.Exception)
            {
                findViewById<ProgressBar>(R.id.loading).visibility = View.GONE
                findViewById<TextView>(R.id.errorMsg).visibility = View.VISIBLE
            }
        }
    }
}