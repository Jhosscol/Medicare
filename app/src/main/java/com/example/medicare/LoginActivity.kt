package com.example.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authManager: FirebaseAuthManager

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: TextView
    private lateinit var btnForgotPassword: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = FirebaseAuthManager(this)

        // ✅ VERIFICAR SI YA ESTÁ LOGUEADO
        if (authManager.isUserLoggedIn()) {
            goToMainActivity()
            return
        }

        setContentView(R.layout.activity_login)
        initViews()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        btnLogin.setOnClickListener {
            login()
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Validaciones
        when {
            email.isEmpty() -> {
                tvError.text = "Ingrese su email"
                tvError.visibility = View.VISIBLE
                return
            }
            password.isEmpty() -> {
                tvError.text = "Ingrese su contraseña"
                tvError.visibility = View.VISIBLE
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tvError.text = "Email inválido"
                tvError.visibility = View.VISIBLE
                return
            }
        }

        // Mostrar loading
        showLoading(true)
        tvError.visibility = View.GONE

        // Login con Firebase
        lifecycleScope.launch {
            val resultado = authManager.loginUser(email, password)

            showLoading(false)

            resultado.fold(
                onSuccess = { user ->
                    Toast.makeText(
                        this@LoginActivity,
                        "✅ Bienvenido ${user.displayName ?: user.email}",
                        Toast.LENGTH_SHORT
                    ).show()

                    goToMainActivity()
                },
                onFailure = { exception ->
                    val mensaje = authManager.getErrorMessage(exception)
                    tvError.text = mensaje
                    tvError.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val etEmailReset = dialogView.findViewById<EditText>(R.id.etEmailReset)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Recuperar Contraseña")
            .setMessage("Ingrese su email y le enviaremos un enlace de recuperación")
            .setView(dialogView)
            .setPositiveButton("Enviar") { dialog, _ ->
                val email = etEmailReset.text.toString().trim()

                if (email.isEmpty()) {
                    Toast.makeText(this, "Ingrese un email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val resultado = authManager.resetPassword(email)

                    resultado.fold(
                        onSuccess = {
                            Toast.makeText(
                                this@LoginActivity,
                                "✅ Email de recuperación enviado a $email",
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                        },
                        onFailure = { exception ->
                            Toast.makeText(
                                this@LoginActivity,
                                authManager.getErrorMessage(exception),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}