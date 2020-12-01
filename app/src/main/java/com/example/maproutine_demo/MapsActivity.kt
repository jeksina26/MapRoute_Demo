package com.example.maproutine_demo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var mLocationManager: LocationManager? = null
    private var mLocationListener: LocationListener? = null
    private var mMarkerOptions: MarkerOptions? = null
    private var mOrigin: LatLng? = null
    private var mDestination: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        myLocation
    }


    private val myLocation: Unit
        private get() {

            mLocationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
            mLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    mOrigin = LatLng(location.latitude, location.longitude)
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(mOrigin, 12f))
                    if (mOrigin != null && mDestination != null) drawRoute()
                }

                override fun onStatusChanged(
                    provider: String,
                    status: Int,
                    extras: Bundle
                ) {
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            val currentApiVersion = Build.VERSION.SDK_INT
            if (currentApiVersion >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED) {
                    mMap!!.isMyLocationEnabled = true
                    mLocationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        10000,
                        0f,
                        mLocationListener
                    )
                    mMap!!.setOnMapClickListener { latLng ->
                        mDestination = latLng
                        mMap!!.clear()
                        mMarkerOptions =
                            MarkerOptions().position(mDestination!!).title("Destination")
                        mMap!!.addMarker(mMarkerOptions)
                        if (mOrigin != null && mDestination != null) drawRoute()
                    }
                } else {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ), 100
                    )
                }
            }
        }

    private fun drawRoute() {

        val url = getDirectionsUrl(mOrigin, mDestination)

        val downloadTask = DownloadTask()

        downloadTask.execute(url)
    }

    private fun getDirectionsUrl(origin: LatLng?, dest: LatLng?): String {

        val str_origin = "origin=" + origin!!.latitude + "," + origin.longitude

        val str_dest = "destination=" + dest!!.latitude + "," + dest.longitude

        val key = "key=" + getString(R.string.google_maps_key)

        val parameters = "$str_origin&$str_dest&$key"

        val output = "json"

        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }

    companion object {
        @Throws(IOException::class)
        private fun downloadUrl(strUrl: String?): String {
            var data = ""
            var iStream: InputStream? = null
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(strUrl)


                urlConnection = url.openConnection() as HttpURLConnection

                urlConnection.connect()

                iStream = urlConnection!!.inputStream
                val br =
                    BufferedReader(InputStreamReader(iStream))
                val sb = StringBuffer()
                var line: String? = ""
                while (br.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                data = sb.toString()
                Log.d("downloadUrl", data)
                br.close()
            } catch (e: java.lang.Exception) {
                Log.d("Exception", e.toString())
            } finally {
                iStream!!.close()
                urlConnection!!.disconnect()
            }
            return data
        }
    }


    private class DownloadTask :
        AsyncTask<String?, Void?, String?>() {
        override fun doInBackground(vararg url: String?): String {

            var data = ""
            try {
                data = downloadUrl(url[0])
                Log.d("DownloadTask", "DownloadTask : $data")
            } catch (e: java.lang.Exception) {
                Log.d("Background Task", e.toString())
            }
            return data
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val parserTask = ParseTask()

            parserTask.execute(result)
        }
    }

    class ParseTask :
        AsyncTask<String?, Int?, List<List<HashMap<String, String>>>?>() {

             override fun doInBackground(vararg p0: String?): List<List<HashMap<String, String>>>? {
                val jObject: JSONObject
                var routes: List<List<HashMap<String, String>>>? =
                    null
                try {
                    jObject = JSONObject(p0[0])
                    val parser = DirectionsJSONParser()

                    routes = parser.parse(jObject)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return routes
            }


            override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
                var points: ArrayList<LatLng?>? = null
                var lineOptions: PolylineOptions? = null


                for (i in result!!.indices) {
                    points = ArrayList()
                    lineOptions = PolylineOptions()

                    val path =
                        result[i]

                    for (j in path.indices) {
                        val point = path[j]
                        val lat = point["lat"]!!.toDouble()
                        val lng = point["lng"]!!.toDouble()
                        val position = LatLng(lat, lng)
                        points.add(position)
                    }

                    lineOptions.addAll(points)
                    lineOptions.width(8f)
                    lineOptions.color(Color.RED)
                }

                if (lineOptions != null) {
                     var mPolyline: Polyline? = null
                     var mMap: GoogleMap? = null
                    if (mPolyline != null) {
                        mPolyline!!.remove()
                    }
                    mMap!!.addPolyline(lineOptions)
                }
            }
        }

    }


