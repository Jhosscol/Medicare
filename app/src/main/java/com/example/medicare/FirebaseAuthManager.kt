package com.example.medicare

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirebaseAuthManager"
        private const val USERS_COLLECTION = "users"
    }

    // === ESTADO DE SESIÓN ===

    fun isUserLoggedIn(): Boolean {
        val user = auth.currentUser
        val isLogged = user != null
        Log.d(TAG, "Usuario logueado: $isLogged (${user?.email})")
        return isLogged
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun getCurrentUserId(): String? {
        val userId = auth.currentUser?.uid

        // ✅ GUARDAR EN SHAREDPREFERENCES
        if (userId != null) {
            sharedPrefs.edit().putString("user_id", userId).apply()
            Log.d(TAG, "✅ UserId guardado en SharedPrefs: $userId")
        }

        return userId
    }

    // === REGISTRO ===

    suspend fun registerUser(
        email: String,
        password: String,
        nombre: String,
        telefono: String = "",
        telefonoEmergencia: String = ""
    ): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Iniciando registro para: $email")

            // 1. Crear usuario en Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                Log.d(TAG, "✅ Usuario creado en Auth: ${user.uid}")

                // ✅ GUARDAR userId EN SHAREDPREFERENCES
                sharedPrefs.edit().putString("user_id", user.uid).apply()
                Log.d(TAG, "✅ UserId guardado: ${user.uid}")

                // 2. Actualizar perfil con nombre
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(nombre)
                    .build()

                user.updateProfile(profileUpdates).await()

                // 3. Crear documento en Firestore
                val usuario = Usuario(
                    uid = user.uid,
                    nombre = nombre,
                    email = email,
                    telefono = telefono,
                    telefonoEmergencia = telefonoEmergencia,
                    fechaCreacion = Timestamp.now(),
                    ultimoAcceso = Timestamp.now()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .set(usuario)
                    .await()

                Log.d(TAG, "✅ Documento usuario creado en Firestore")

                // 4. Enviar email de verificación (opcional)
                user.sendEmailVerification().await()
                Log.d(TAG, "📧 Email de verificación enviado")

                Result.success(user)
            } else {
                Result.failure(Exception("No se pudo crear el usuario"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en registro: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === LOGIN ===

    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Iniciando login para: $email")

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                Log.d(TAG, "✅ Login exitoso: ${user.uid}")

                // ✅ GUARDAR userId EN SHAREDPREFERENCES
                sharedPrefs.edit().putString("user_id", user.uid).apply()
                Log.d(TAG, "✅ UserId guardado: ${user.uid}")

                // Actualizar último acceso
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .update("ultimoAcceso", Timestamp.now())
                    .await()

                Result.success(user)
            } else {
                Result.failure(Exception("Error en el login"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en login: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === LOGOUT ===

    fun logout() {
        // ✅ LIMPIAR SHAREDPREFERENCES AL CERRAR SESIÓN
        sharedPrefs.edit().remove("user_id").apply()
        Log.d(TAG, "🗑️ UserId eliminado de SharedPrefs")

        auth.signOut()
        Log.d(TAG, "🔒 Usuario deslogueado")
    }

    // === RECUPERAR CONTRASEÑA ===

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            Log.d(TAG, "Enviando email de recuperación a: $email")

            auth.sendPasswordResetEmail(email).await()

            Log.d(TAG, "✅ Email de recuperación enviado")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando email: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === OBTENER DATOS USUARIO ===

    suspend fun getUserData(): Result<Usuario?> {
        return try {
            val userId = getCurrentUserId()

            if (userId == null) {
                return Result.failure(Exception("No hay usuario logueado"))
            }

            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val usuario = document.toObject(Usuario::class.java)
                Log.d(TAG, "✅ Datos usuario obtenidos: ${usuario?.nombre}")
                Result.success(usuario)
            } else {
                Log.w(TAG, "⚠️ Documento usuario no existe")
                Result.success(null)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo datos: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === ACTUALIZAR PERFIL ===

    suspend fun updateUserProfile(
        nombre: String? = null,
        telefono: String? = null,
        telefonoEmergencia: String? = null,
        fotoPerfil: String? = null
    ): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("No hay usuario logueado"))

            val updates = mutableMapOf<String, Any>()
            nombre?.let { updates["nombre"] = it }
            telefono?.let { updates["telefono"] = it }
            telefonoEmergencia?.let { updates["telefonoEmergencia"] = it }
            fotoPerfil?.let { updates["fotoPerfil"] = it }
            updates["ultimaActualizacion"] = Timestamp.now()

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Perfil actualizado")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando perfil: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === VERIFICACIÓN EMAIL ===

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Log.d(TAG, "✅ Email de verificación enviado")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando verificación: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === ELIMINAR CUENTA ===

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("No hay usuario logueado"))

            // 1. Eliminar datos de Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            // 2. Eliminar usuario de Auth
            auth.currentUser?.delete()?.await()

            Log.d(TAG, "✅ Cuenta eliminada")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando cuenta: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === MANEJO DE ERRORES ===

    fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("network error") == true ->
                "Error de conexión. Verifica tu internet."

            exception.message?.contains("email address is already in use") == true ->
                "Este email ya está registrado."

            exception.message?.contains("invalid-email") == true ->
                "Email inválido."

            exception.message?.contains("weak-password") == true ->
                "La contraseña debe tener al menos 6 caracteres."

            exception.message?.contains("user-not-found") == true ->
                "Usuario no encontrado."

            exception.message?.contains("wrong-password") == true ->
                "Contraseña incorrecta."

            exception.message?.contains("too-many-requests") == true ->
                "Demasiados intentos. Intenta más tarde."

            else -> "Error: ${exception.message ?: "Desconocido"}"
        }
    }
}