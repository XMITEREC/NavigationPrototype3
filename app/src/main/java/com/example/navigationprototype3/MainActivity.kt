@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.navigationprototype3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.navigationprototype3.ui.theme.Navigationprototype3Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.lang.Exception
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private val LOCATION_PERMISSION_REQUEST = 100

    private var mapView: MapView? = null
    private var currentLat = 48.8583
    private var currentLon = 2.2944

    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    private var dataJob: Job? = null

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        Log.d("MainActivity", "Returned from Location Settings.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required by OSMDroid
        Configuration.getInstance().userAgentValue = packageName

        // Check location permission on startup
        checkLocationPermission()

        setContent {
            Navigationprototype3Theme {
                // We are using experimental APIs like Scaffold & TopAppBar from Material 3
                Scaffold(
                    topBar = { MyTopBar() }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // OSMDroid map
                        MyMapView(
                            modifier = Modifier.fillMaxSize(),
                            onMapReady = { mv -> mapView = mv }
                        )

                        // UI Buttons at bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val coroutineScope = rememberCoroutineScope()

                            // Start Nav
                            ThreeDButton(text = "Start Navigation") {
                                coroutineScope.launch {
                                    ensureGPSIsOn()
                                    fetchUserLocationOnce()  // get initial fix
                                    startNavigation()         // sensor+websocket
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Stop Nav
                            ThreeDButton(text = "Stop Navigation") {
                                stopNavigation()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * A composable Top Bar with loriii.png centered.
     */
    @Composable
    fun MyTopBar() {
        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.loriii),
                        contentDescription = "App Logo",
                        modifier = Modifier.height(40.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = Color.Black,   // black background
                titleContentColor = Color.White
            )
        )
    }

    /**
     * A simple 3D-like button using a gradient background and shadow.
     */
    @Composable
    fun ThreeDButton(
        text: String,
        onClick: () -> Unit
    ) {
        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFEEEEEE), Color(0xFFAAAAAA))
        )

        Button(
            onClick = onClick,
            contentPadding = PaddingValues(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(gradient)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                color = Color.Black
            )
        }
    }

    @Composable
    fun MyMapView(
        modifier: Modifier = Modifier,
        onMapReady: (MapView) -> Unit
    ) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val mapView = org.osmdroid.views.MapView(context)
                mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                mapView.setMultiTouchControls(true)
                mapView.controller.setZoom(15.0)

                // Default: Eiffel Tower
                val startPoint = GeoPoint(48.8583, 2.2944)
                mapView.controller.setCenter(startPoint)

                onMapReady(mapView)
                mapView
            }
        )
    }

    // ------------------------------------------------------------------
    // LOCATION PERMISSION & GPS PROMPT
    // ------------------------------------------------------------------
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Location permission granted.")
            } else {
                Log.d("MainActivity", "Location permission denied.")
            }
        }
    }

    private suspend fun ensureGPSIsOn() = withContext(Dispatchers.Main) {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            locationSettingsLauncher.launch(intent)
        }
    }

    /**
     * Fetch a single fresh location from FusedLocationProviderClient (coroutines-play-services).
     */
    private suspend fun fetchUserLocationOnce() = withContext(Dispatchers.IO) {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            val loc = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()

            loc?.let {
                currentLat = it.latitude
                currentLon = it.longitude
                Log.d("MainActivity", "Got location lat=$currentLat, lon=$currentLon")
            }
        }

        // Update map on main thread
        withContext(Dispatchers.Main) {
            updateMapMarker(currentLat, currentLon, "You are here")
        }
    }

    // ------------------------------------------------------------------
    // NAVIGATION: START/STOP, SENSORS & WEBSOCKET
    // ------------------------------------------------------------------
    private fun startNavigation() {
        // 1) Start sensor service
        val intent = Intent(this, SensorService::class.java)
        startService(intent)

        // 2) Open WebSocket
        openWebSocket()

        // 3) Start a coroutine to send data every 2s
        dataJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(2000)
                sendSensorData()
            }
        }
    }

    private fun stopNavigation() {
        // Stop sensor service
        stopService(Intent(this, SensorService::class.java))

        // Cancel data job
        dataJob?.cancel()
        dataJob = null

        // Close WebSocket
        webSocket?.close(1000, "User stopped nav")
        okHttpClient?.dispatcher?.executorService?.shutdown()
        webSocket = null
        okHttpClient = null

        Log.d("MainActivity", "Navigation stopped.")
    }

    // ------------------------------------------------------------------
    // WEBSOCKET
    // ------------------------------------------------------------------
    private fun openWebSocket() {
        okHttpClient = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url("wss://websocket.loca.lt")
            .addHeader("bypass-tunnel-reminder", "true")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS", "WebSocket opened.")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WS", "Received: $text")
                handleIncomingSpeed(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("WS", "Received bytes: $bytes")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS", "WebSocket closed: $code $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "WebSocket error: ${t.message}")
            }
        }

        webSocket = okHttpClient?.newWebSocket(request, listener)
    }

    private fun sendSensorData() {
        val dataList = SensorService.sensorData.popData()
        if (dataList.isEmpty()) return

        val accelerometerList = dataList.map { triple ->
            mapOf(
                "ts" to triple.first,
                "ax" to triple.second[0],
                "ay" to triple.second[1],
                "az" to triple.second[2]
            )
        }
        val heading = dataList.last().third

        val jsonObj = mapOf(
            "accelerometer" to accelerometerList,
            "heading" to heading
        )

        val message = com.google.gson.Gson().toJson(jsonObj)
        webSocket?.send(message)
        Log.d("WS", "Sent sensor data => #samples=${dataList.size}, heading=$heading")
    }

    // ------------------------------------------------------------------
    // HANDLE INCOMING SPEED => UPDATE LOCATION (DRIFT FIX APPLIED HERE)
    // ------------------------------------------------------------------
    private fun handleIncomingSpeed(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            if (!json.has("avg_speed")) return
            val avgSpeed = json.getDouble("avg_speed") // in m/s

            // Our data interval is 4s
            val timeSec = 2.0
            val dist = avgSpeed * timeSec

            // We'll take the last heading from SensorService
            val headingDeg = SensorService.sensorData.getLatestHeading()
            val headingRad = Math.toRadians(headingDeg.toDouble())

            // Dead-reckoning update
            val earthRadius = 6371000.0
            val deltaLat = dist * cos(headingRad) / earthRadius
            val deltaLon = dist * sin(headingRad) / (earthRadius * cos(Math.toRadians(currentLat)))

            currentLat += Math.toDegrees(deltaLat)
            currentLon += Math.toDegrees(deltaLon)

            // Snap to nearest road using OSRM in a background coroutine
            CoroutineScope(Dispatchers.IO).launch {
                val snapped = snapToNearestRoadWithOSRM(currentLat, currentLon)
                if (snapped != null) {
                    currentLat = snapped.first
                    currentLon = snapped.second
                }
                // Update map on the main thread
                withContext(Dispatchers.Main) {
                    updateMapMarker(currentLat, currentLon, "Moving..")
                }
            }

        } catch (e: Exception) {
            Log.e("WS", "handleIncomingSpeed parse error: $e")
        }
    }

    // ------------------------------------------------------------------
    // SNAP TO OSRM (PUBLIC DEMO SERVER)
    // ------------------------------------------------------------------
    /**
     * Calls OSRM's "Nearest" service to snap (lat,lon) to the nearest road.
     * If successful, returns (snappedLat, snappedLon), else null.
     *
     * Docs: https://github.com/Project-OSRM/osrm-backend/blob/master/docs/http.md#nearest-service
     */
    private suspend fun snapToNearestRoadWithOSRM(
        lat: Double,
        lon: Double
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            // Example endpoint: https://router.project-osrm.org/nearest/v1/driving/{lon},{lat}?number=1
            val url = "https://router.project-osrm.org/nearest/v1/driving/$lon,$lat?number=1"

            val client = okHttpClient ?: OkHttpClient.Builder().build()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            val json = JSONObject(body)
            if (!json.has("waypoints")) return@withContext null

            val waypoints = json.getJSONArray("waypoints")
            if (waypoints.length() == 0) return@withContext null

            val firstWaypoint = waypoints.getJSONObject(0)
            val locationArray = firstWaypoint.getJSONArray("location")

            // OSRM returns [lon, lat]
            val snappedLon = locationArray.getDouble(0)
            val snappedLat = locationArray.getDouble(1)

            return@withContext Pair(snappedLat, snappedLon)

        } catch (e: Exception) {
            Log.e("WS", "Error snapping to road: ${e.message}")
            null
        }
    }

    // ------------------------------------------------------------------
    // MAP MARKER
    // ------------------------------------------------------------------
    private fun updateMapMarker(latitude: Double, longitude: Double, title: String = "Marker") {
        mapView?.let { mv ->
            val geoPoint = GeoPoint(latitude, longitude)
            mv.controller.setZoom(20.0)
            mv.controller.setCenter(geoPoint)

            mv.overlays.clear()
            val marker = Marker(mv)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = title
            mv.overlays.add(marker)
            mv.invalidate()
        }
    }
}
