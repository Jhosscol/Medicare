package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicare.FirebaseAuthManager
import com.example.medicare.MainActivity
import com.example.medicare.R
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var authManager: FirebaseAuthManager

    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etTelefonoEmergencia: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPasswordConfirm: EditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authManager = FirebaseAuthManager(this)
        initViews()
    }

    private fun initViews() {
        etNombre = findViewById(R.id.etNombre)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etTelefonoEmergencia = findViewById(R.id.etTelefonoEmergencia)
        etPassword = findViewById(R.id.etPassword)
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        btnRegister.setOnClickListener {
            register()
        }

        findViewById<TextView>(R.id.tvBackToLogin).setOnClickListener {
            finish()
        }
    }

    private fun register() {
        val nombre = etNombre.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val telefonoEmergencia = etTelefonoEmergencia.text.toString().trim()
        val password = etPassword.text.toString()
        val passwordConfirm = etPasswordConfirm.text.toString()

        // Validaciones
        when {
            nombre.isEmpty() -> {
                tvError.text = "Ingrese su nombre"
                tvError.visibility = View.VISIBLE
                return
            }
            email.isEmpty() -> {
                tvError.text = "Ingrese su email"
                tvError.visibility = View.VISIBLE
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tvError.text = "Email inválido"
                tvError.visibility = View.VISIBLE
                return
            }
            password.length < 6 -> {
                tvError.text = "La contraseña debe tener al menos 6 caracteres"
                tvError.visibility = View.VISIBLE
                return
            }
            password != passwordConfirm -> {
                tvError.text = "Las contraseñas no coinciden"
                tvError.visibility = View.VISIBLE
                return
            }
        }

        showLoading(true)
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            val resultado = authManager.registerUser(
                email = email,
                password = password,
                nombre = nombre,
                telefono = telefono,
                telefonoEmergencia = telefonoEmergencia
            )

            showLoading(false)

            resultado.fold(
                onSuccess = { user ->
                    Toast.makeText(
                        this@RegisterActivity,
                        "✅ Cuenta creada exitosamente. Te enviamos un email de verificación.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Ir a MainActivity
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                },
                onFailure = { exception ->
                    val mensaje = authManager.getErrorMessage(exception)
                    tvError.text = mensaje
                    tvError.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
        etNombre.isEnabled = !show
        etEmail.isEnabled = !show
        etTelefono.isEnabled = !show
        etTelefonoEmergencia.isEnabled = !show
        etPassword.isEnabled = !show
        etPasswordConfirm.isEnabled = !show
    }
}