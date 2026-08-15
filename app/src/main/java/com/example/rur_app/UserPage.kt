package com.example.rur_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvUsername = findViewById<TextView>(R.id.textView16)
        val tvRole = findViewById<TextView>(R.id.tv_user_role)
        val ivProfile = findViewById<ImageView>(R.id.imageView25)

        // Set generic static image
        ivProfile.setImageResource(R.drawable.rur_logo)

        // Fetch user data from Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username")
                        val role = document.getString("role")

                        tvUsername.text = username ?: "User"
                        tvRole.text = role ?: "Guest"
                    }
                }
        }

        // -- ACTION BUTTONS --
        val btnChangePhoto = findViewById<ImageView>(R.id.imageButton7)
        val btnChangeRole = findViewById<ImageView>(R.id.imageButton8)
        val btnChangeUsername = findViewById<ImageView>(R.id.imageButton9)

        btnChangePhoto.setOnClickListener {
            Toast.makeText(this, "Profile picture feature pending plan upgrade", Toast.LENGTH_SHORT).show()
        }

        btnChangeRole.setOnClickListener {
            showChangeRoleDialog(tvRole)
        }

        btnChangeUsername.setOnClickListener {
            showChangeUsernameDialog(tvUsername)
        }

        // -- NAVEGACIÓN DE LA BARRA INFERIOR --
        val btnNavHome = findViewById<ImageButton>(R.id.imageButton10)
        val btnNavFiles = findViewById<ImageButton>(R.id.imageButton11)
        // (El botón de Usuario no hace nada porque ya estamos en UserPage)

        btnNavHome.setOnClickListener {
            startActivity(Intent(this, HomePage::class.java))
            overridePendingTransition(0, 0)
        }

        btnNavFiles.setOnClickListener {
            startActivity(Intent(this, MediaGallery::class.java))
            overridePendingTransition(0, 0)
        }

        // -- BOTÓN DE CERRAR SESIÓN --
        val btnLogout = findViewById<Button>(R.id.button7)
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LogInPage::class.java)
            // Estas banderas borran el historial de pantallas para que no se pueda usar el botón "Atrás" para volver a entrar
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showChangeRoleDialog(tvRole: TextView) {
        val roles = arrayOf("administrator", "developer", "tester", "guest")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Role")
            .setItems(roles) { _, which ->
                val selectedRole = roles[which]
                updateUserField("role", selectedRole, tvRole)
            }
            .show()
    }

    private fun showChangeUsernameDialog(tvUsername: TextView) {
        val input = EditText(this)
        input.hint = "New username"
        
        // Add some margin to the EditText
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 20, 60, 20)
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Change Username")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUserField("username", newName, tvUsername)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUserField(field: String, value: String, textView: TextView) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update(field, value)
            .addOnSuccessListener {
                textView.text = value
                Toast.makeText(this, "$field updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error updating $field", Toast.LENGTH_SHORT).show()
            }
    }
}