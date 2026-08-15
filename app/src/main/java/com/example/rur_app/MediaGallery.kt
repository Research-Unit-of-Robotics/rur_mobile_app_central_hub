package com.example.rur_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MediaGallery : AppCompatActivity() {
    private lateinit var mediaGrid: androidx.gridlayout.widget.GridLayout
    private var allMedia = mutableListOf<MediaItem>()
    private var currentFilter: String? = "DRONE"

    data class MediaItem(
        val id: String,
        val robotName: String,
        val date: String,
        val category: String,
        val isVideo: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_media_gallery)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mediaGrid = findViewById(R.id.media_grid)

        // Initialize with some dummy data for demonstration
        loadDummyMedia()

        // -- FILTER BUTTONS --
        val btnDrone = findViewById<Button>(R.id.btn_filter_drone)
        val btnRover = findViewById<Button>(R.id.btn_filter_rover)
        val btnOther = findViewById<Button>(R.id.btn_filter_other)

        btnDrone.setOnClickListener { toggleFilter("DRONE") }
        btnRover.setOnClickListener { toggleFilter("ROVER") }
        btnOther.setOnClickListener { toggleFilter("OTHER") }

        // -- NAVEGACIÓN DE LA BARRA INFERIOR --
        val btnNavHome = findViewById<ImageButton>(R.id.imageButton4)
        val btnNavUser = findViewById<ImageButton>(R.id.imageButton6)

        btnNavHome.setOnClickListener {
            startActivity(Intent(this, HomePage::class.java))
            overridePendingTransition(0, 0)
        }

        btnNavUser.setOnClickListener {
            startActivity(Intent(this, UserPage::class.java))
            overridePendingTransition(0, 0)
        }

        displayMedia()
        updateFilterButtonStyles()
    }

    private fun loadDummyMedia() {
        allMedia.add(MediaItem("1", "Drone Alpha", "01/08/26", "DRONE", false))
        allMedia.add(MediaItem("2", "Rover X1", "02/08/26", "ROVER", true))
        allMedia.add(MediaItem("3", "Proto-Bot", "03/08/26", "OTHER", false))
        allMedia.add(MediaItem("4", "Drone Beta", "04/08/26", "DRONE", true))
        allMedia.add(MediaItem("5", "Mars Rover", "05/08/26", "ROVER", false))
        allMedia.add(MediaItem("6", "Arm-Unit", "06/08/26", "OTHER", true))
    }

    private fun toggleFilter(category: String) {
        currentFilter = category
        displayMedia()
        updateFilterButtonStyles()
    }

    private fun displayMedia() {
        mediaGrid.removeAllViews()

        val filteredList = allMedia.filter { it.category == currentFilter }

        for (item in filteredList) {
            val cardView = LayoutInflater.from(this).inflate(R.layout.item_media_card, mediaGrid, false)
            
            cardView.findViewById<TextView>(R.id.tv_media_info).text = "${item.robotName} - ${item.date}"
            val ivPlay = cardView.findViewById<ImageView>(R.id.iv_play_icon)

            if (item.isVideo) {
                ivPlay.visibility = View.VISIBLE
            } else {
                ivPlay.visibility = View.GONE
            }

            mediaGrid.addView(cardView)
        }
    }

    private fun updateFilterButtonStyles() {
        val btnDrone = findViewById<Button>(R.id.btn_filter_drone)
        val btnRover = findViewById<Button>(R.id.btn_filter_rover)
        val btnOther = findViewById<Button>(R.id.btn_filter_other)

        // Reset all to yellow background, white text
        btnDrone.setBackgroundColor(getColor(R.color.accent_yellow))
        btnDrone.setTextColor(getColor(android.R.color.white))
        btnRover.setBackgroundColor(getColor(R.color.accent_yellow))
        btnRover.setTextColor(getColor(android.R.color.white))
        btnOther.setBackgroundColor(getColor(R.color.accent_yellow))
        btnOther.setTextColor(getColor(android.R.color.white))

        // Highlight selected: white background, yellow text
        when (currentFilter) {
            "DRONE" -> {
                btnDrone.setBackgroundColor(getColor(android.R.color.white))
                btnDrone.setTextColor(getColor(R.color.accent_yellow))
            }
            "ROVER" -> {
                btnRover.setBackgroundColor(getColor(android.R.color.white))
                btnRover.setTextColor(getColor(R.color.accent_yellow))
            }
            "OTHER" -> {
                btnOther.setBackgroundColor(getColor(android.R.color.white))
                btnOther.setTextColor(getColor(R.color.accent_yellow))
            }
        }
    }
}