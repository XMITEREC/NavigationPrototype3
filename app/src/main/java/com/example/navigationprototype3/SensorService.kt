package com.example.navigationprototype3

import android.app.Service
import android.content.Intent
import android.hardware.*
import android.os.IBinder
import android.util.Log

class SensorService : Service(), SensorEventListener {

    companion object {
        val sensorData = SensorData()
    }

    private lateinit var sensorManager: SensorManager
    private var linearAcc: Sensor? = null
    private var rotVector: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        linearAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        rotVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        linearAcc?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rotVector?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        Log.d("SensorService", "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        Log.d("SensorService", "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val (ax, ay, az) = event.values
                sensorData.addAccelSample(event.timestamp, ax, ay, az)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                var hdg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (hdg < 0) hdg += 360
                sensorData.updateHeading(hdg)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    class SensorData {
        private val dataBuffer = mutableListOf<Triple<Long, FloatArray, Float>>()
        private var currentHeading = 0f

        fun addAccelSample(ts: Long, ax: Float, ay: Float, az: Float) {
            synchronized(dataBuffer) {
                dataBuffer.add(Triple(ts, floatArrayOf(ax, ay, az), currentHeading))
            }
        }

        fun updateHeading(hdg: Float) {
            currentHeading = hdg
        }

        fun getLatestHeading(): Float {
            return currentHeading
        }

        fun popData(): List<Triple<Long, FloatArray, Float>> {
            synchronized(dataBuffer) {
                if (dataBuffer.isEmpty()) return emptyList()
                val copy = dataBuffer.toList()
                dataBuffer.clear()
                return copy
            }
        }
    }
}
