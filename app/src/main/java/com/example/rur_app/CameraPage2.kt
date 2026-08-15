package com.example.rur_app

import android.graphics.Color
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import livekit.org.webrtc.RendererCommon

class CameraPage2 : AppCompatActivity() {

    private lateinit var room: Room
    private lateinit var videoRenderer: TextureViewRenderer
    
    // UI Elements
    private lateinit var tvStatusText: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvRobotConnected: TextView
    private lateinit var tvParticipantsInfo: TextView
    private lateinit var imgNoVideoPlaceholder: ImageView

    // --- LIVEKIT CONFIGURATION ---
    // Change these values to connect to your LiveKit server
    private val LIVEKIT_URL = "<LIVEKIT-URL>"
    private val CONNECTION_TOKEN = "<LIVEKIT-TOKEN>"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_page2)

        // 1. Inicialización de vistas (Layout Principal)
        val mainRootLayout = findViewById<ConstraintLayout>(R.id.mainRootLayout)

        tvStatusText = findViewById(R.id.tvStatusText)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvRobotConnected = findViewById(R.id.tvRobotConnected)
        tvParticipantsInfo = findViewById(R.id.tvParticipantsInfo)
        imgNoVideoPlaceholder = findViewById(R.id.imgNoVideoPlaceholder)
        videoRenderer = findViewById(R.id.textureViewVideo)

        // Setup LiveKit Room
        room = LiveKit.create(applicationContext)

        // Initialize Video Renderer
        room.initVideoRenderer(videoRenderer)
        videoRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)

        // 2. Controladores para Mostrar/Ocultar Settings
        val settingsPanel = findViewById<LinearLayout>(R.id.settingsPanel)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        val btnCloseSettings = findViewById<ImageButton>(R.id.btnCloseSettings)

        btnSettings.setOnClickListener {
            TransitionManager.beginDelayedTransition(mainRootLayout)
            settingsPanel.visibility = View.VISIBLE
        }

        btnCloseSettings.setOnClickListener {
            TransitionManager.beginDelayedTransition(mainRootLayout)
            settingsPanel.visibility = View.GONE
        }

        // 3. Controladores para Mostrar/Ocultar Telemetría
        val rightMetricsBar = findViewById<FrameLayout>(R.id.rightMetricsBar)
        val telemetryPanel = findViewById<LinearLayout>(R.id.telemetryPanel)
        val tvMetricsToggle = findViewById<TextView>(R.id.tvMetricsToggle)

        rightMetricsBar.setOnClickListener {
            TransitionManager.beginDelayedTransition(mainRootLayout)
            if (telemetryPanel.visibility == View.GONE) {
                telemetryPanel.visibility = View.VISIBLE
                tvMetricsToggle.text = "Hide Telemetry"
            } else {
                telemetryPanel.visibility = View.GONE
                tvMetricsToggle.text = "Show Metrics"
            }
        }

        // 4. Navegación hacia atrás
        findViewById<TextView>(R.id.tvTitle).setOnClickListener {
            finish()
        }

        // 5. Joystick Logic
        val joystick = findViewById<JoystickView>(R.id.joystickControl)
        joystick.setOnJoystickMoveListener(object : JoystickView.OnJoystickMoveListener {
            override fun onValueChanged(x: Float, y: Float) {
                Log.d("JoystickControl", "X: $x, Y: $y")
                publishJoystickData(x, y)
            }
        })

        // 6. Connect to LiveKit Room
        connectToRoom()
    }

    private fun connectToRoom() {
        lifecycleScope.launch {
            try {
                room.connect(LIVEKIT_URL, CONNECTION_TOKEN)
                updateStatus(true)
            } catch (e: Exception) {
                Log.e("LiveKit", "Failed to connect", e)
                Toast.makeText(this@CameraPage2, "Connection Failed", Toast.LENGTH_SHORT).show()
                updateStatus(false)
            }
        }

        // Listen for Room Events
        lifecycleScope.launch {
            room.events.collect { event ->
                when (event) {
                    is RoomEvent.ParticipantConnected -> updateParticipants()
                    is RoomEvent.ParticipantDisconnected -> updateParticipants()
                    is RoomEvent.TrackSubscribed -> {
                        val track = event.track
                        if (track is VideoTrack) {
                            track.addRenderer(videoRenderer)
                            runOnUiThread { imgNoVideoPlaceholder.visibility = View.GONE }
                        }
                    }
                    is RoomEvent.Disconnected -> {
                        updateStatus(false)
                        runOnUiThread { imgNoVideoPlaceholder.visibility = View.VISIBLE }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateStatus(connected: Boolean) {
        runOnUiThread {
            if (connected) {
                tvStatusText.text = "Connected"
                tvStatusText.setTextColor(Color.parseColor("#75FB4C"))
                ivStatusIcon.setImageResource(android.R.drawable.presence_online)
                ivStatusIcon.setColorFilter(Color.parseColor("#75FB4C"))
            } else {
                tvStatusText.text = "Disconnected"
                tvStatusText.setTextColor(Color.RED)
                ivStatusIcon.setImageResource(R.drawable.nointernet)
                ivStatusIcon.setColorFilter(Color.RED)
                tvRobotConnected.visibility = View.GONE
            }
            updateParticipants()
        }
    }

    private fun updateParticipants() {
        val remoteCount = room.remoteParticipants.size
        val totalCount = remoteCount + 1 // including local
        val identities = room.remoteParticipants.values.map { it.identity?.value ?: "Unknown" }

        runOnUiThread {
            tvParticipantsInfo.text = "Participants: $totalCount"
            
            // Check for TRINITY identity
            if (identities.any { it.equals("TRINITY", ignoreCase = true) }) {
                tvRobotConnected.visibility = View.VISIBLE
            } else {
                tvRobotConnected.visibility = View.GONE
            }
        }
    }

    private fun publishJoystickData(x: Float, y: Float) {
        if (room.state != Room.State.CONNECTED) return

        // Create a simple JSON string with coordinates
        val data = "{\"x\": $x, \"y\": $y}".toByteArray()
        lifecycleScope.launch {
            room.localParticipant.publishData(data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoRenderer.release()
        room.disconnect()
    }
}
