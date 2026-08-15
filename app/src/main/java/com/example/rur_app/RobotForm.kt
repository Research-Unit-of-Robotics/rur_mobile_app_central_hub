package com.example.rur_app

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RobotForm : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_robot_form)

        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom))
            insets
        }

        val etName = findViewById<EditText>(R.id.editTextText2)
        val etCode = findViewById<EditText>(R.id.editTextNumber)
        val rgCategory = findViewById<RadioGroup>(R.id.rg_category)
        val cbCamera = findViewById<CheckBox>(R.id.cb_camera)
        val btnSubmit = findViewById<Button>(R.id.button9)

        findViewById<ImageView>(R.id.imageView26).setOnClickListener {
            finish()
        }

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val code = etCode.text.toString().trim()
            val hasCamera = cbCamera.isChecked

            val selectedCategoryId = rgCategory.checkedRadioButtonId
            if (name.isEmpty() || code.isEmpty() || selectedCategoryId == -1) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double clicks
            btnSubmit.isEnabled = false
            Toast.makeText(this, "Adding robot...", Toast.LENGTH_SHORT).show()

            val category = when (selectedCategoryId) {
                R.id.rb_drone -> "DRONE"
                R.id.rb_rover -> "ROVER"
                R.id.rb_other -> "OTHER"
                else -> "OTHER"
            }

            // ——————————————— Diagnosis ———————————————
            val currentUser = FirebaseAuth.getInstance().currentUser
            Log.d("RobotFormDebug", "Current Auth UID: ${currentUser?.uid}")

            val robotMap = hashMapOf(
                "name" to name,
                "code" to code,
                "category" to category,
                "hasCamera" to hasCamera,
                "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "")
            )

            db.collection("robots").add(robotMap)
                .addOnSuccessListener { ref ->
                    Log.d("RobotFormDebug", "Successfully added document ID: ${ref.id}")
                    Toast.makeText(this, "Robot added successfully!", Toast.LENGTH_LONG).show()

                    // 1. Create intent to navigate to your home screen Activity
                    val intent = Intent(this, HomePage::class.java)

                    // 2. Clear back stack so pressing back won't return to the form
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

                    // 3. Start activity and close current form
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("RobotFormDebug", "Firestore write failed!", e)
                    btnSubmit.isEnabled = true
                    Toast.makeText(this, "Error adding robot: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}