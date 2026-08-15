package com.example.rur_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var robotGrid: androidx.gridlayout.widget.GridLayout
    private lateinit var addCard: CardView
    private var allRobots = mutableListOf<Robot>()
    private var currentFilter: String? = "ROVER"

    data class Robot(
        val id: String,
        val name: String,
        val category: String,
        val hasCamera: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        robotGrid = findViewById(R.id.robot_grid)
        addCard = findViewById(R.id.card_add_robot)

        val tvUserName = findViewById<TextView>(R.id.textView5)
        val ivProfile = findViewById<ImageView>(R.id.imageView2)

        // Set generic static image
        ivProfile.setImageResource(R.drawable.rur_logo)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username")
                        
                        if (!username.isNullOrEmpty()) {
                            tvUserName.text = username
                        }
                    }
                }
        }

        // -- FILTER BUTTONS --
        val btnDrone = findViewById<Button>(R.id.btn_filter_drone)
        val btnRover = findViewById<Button>(R.id.btn_filter_rover)
        val btnOther = findViewById<Button>(R.id.btn_filter_other)

        btnDrone.setOnClickListener { toggleFilter("DRONE") }
        btnRover.setOnClickListener { toggleFilter("ROVER") }
        btnOther.setOnClickListener { toggleFilter("OTHER") }

        // -- NAVEGACIÓN DE LA BARRA INFERIOR --
        val btnNavFiles = findViewById<ImageButton>(R.id.imageButton2)
        val btnNavUser = findViewById<ImageButton>(R.id.imageButton3)

        btnNavFiles.setOnClickListener {
            startActivity(Intent(this, MediaGallery::class.java))
            overridePendingTransition(0, 0)
        }

        btnNavUser.setOnClickListener {
            startActivity(Intent(this, UserPage::class.java))
            overridePendingTransition(0, 0)
        }

        // -- NAVEGACIÓN A ACCIONES DE ROBOTS --
        addCard.setOnClickListener {
            startActivity(Intent(this, RobotForm::class.java))
        }

        listenToRobots()
    }

    private fun listenToRobots() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("robots")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                allRobots.clear()
                for (doc in snapshots!!) {
                    val robot = Robot(
                        doc.id,
                        doc.getString("name") ?: "",
                        doc.getString("category") ?: "OTHER",
                        doc.getBoolean("hasCamera") ?: false
                    )
                    allRobots.add(robot)
                }
                displayRobots()
            }
    }

    private fun toggleFilter(category: String) {
        currentFilter = category
        displayRobots()
        //if (currentFilter == null) return
        updateFilterButtonStyles()
    }

    private fun displayRobots() {
        // Clear only the robot cards, but not the addCard
        // To do this simply: clear all and re-add robots then addCard
        robotGrid.removeAllViews()

        val filteredList = if (currentFilter == null) {
            allRobots
        } else {
            allRobots.filter { it.category == currentFilter }
        }

        // Add robot cards
        for (robot in filteredList) {
            val cardView = LayoutInflater.from(this).inflate(R.layout.item_robot_card, robotGrid, false)
            
            cardView.findViewById<TextView>(R.id.tv_robot_name).text = robot.name
            val ivType = cardView.findViewById<ImageView>(R.id.iv_robot_type)
            val ivCamera = cardView.findViewById<ImageView>(R.id.iv_camera_icon)

            when (robot.category) {
                "DRONE" -> ivType.setImageResource(R.drawable.drone_icon)
                "ROVER" -> ivType.setImageResource(R.drawable.mars_rover_icon)
                else -> ivType.setImageResource(R.drawable.robotic_arm_icon)
            }

            if (robot.hasCamera) {
                ivCamera.visibility = View.VISIBLE
            }

            cardView.setOnClickListener {
                if (robot.hasCamera) {
                    startActivity(Intent(this, CameraPage2::class.java))
                }
            }

            val ivDelete = cardView.findViewById<ImageView>(R.id.iv_delete_robot)
            ivDelete.setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Robot")
                    .setMessage("Are you sure you want to delete ${robot.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteRobot(robot)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            val ivEdit = cardView.findViewById<ImageView>(R.id.iv_edit_robot)
            ivEdit.setOnClickListener {
                showEditRobotNameDialog(robot)
            }

            robotGrid.addView(cardView)
        }

        // Always ensure the "Add" card is at the end
        if (addCard.parent != null) {
            (addCard.parent as android.view.ViewGroup).removeView(addCard)
        }
        
        robotGrid.addView(addCard)
    }

    private fun updateFilterButtonStyles() {
        val btnDrone = findViewById<Button>(R.id.btn_filter_drone)
        val btnRover = findViewById<Button>(R.id.btn_filter_rover)
        val btnOther = findViewById<Button>(R.id.btn_filter_other)

        // Reset all
        btnDrone.setBackgroundColor(getColor(R.color.accent_yellow))
        btnDrone.setTextColor(getColor(R.color.white))
        btnRover.setBackgroundColor(getColor(R.color.accent_yellow))
        btnRover.setTextColor(getColor(R.color.white))
        btnOther.setBackgroundColor(getColor(R.color.accent_yellow))
        btnOther.setTextColor(getColor(R.color.white))

        // Highlight selected
        when (currentFilter) {
            "DRONE" -> { btnDrone.setBackgroundColor(getColor(android.R.color.white))
                       btnDrone.setTextColor(getColor(R.color.accent_yellow)) }
            "ROVER" -> { btnRover.setBackgroundColor(getColor(android.R.color.white))
                       btnRover.setTextColor(getColor(R.color.accent_yellow)) }
            "OTHER" -> { btnOther.setBackgroundColor(getColor(android.R.color.white))
                       btnOther.setTextColor(getColor(R.color.accent_yellow)) }
        }
    }

    private fun deleteRobot(robot: Robot) {
        db.collection("robots").document(robot.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "${robot.name} deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting robot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditRobotNameDialog(robot: Robot) {
        val input = EditText(this)
        input.hint = "New robot name"
        input.setText(robot.name)
        
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 20, 60, 20)
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Robot Name")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != robot.name) {
                    updateRobotName(robot.id, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRobotName(robotId: String, newName: String) {
        db.collection("robots").document(robotId)
            .update("name", newName)
            .addOnSuccessListener {
                Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating name: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}