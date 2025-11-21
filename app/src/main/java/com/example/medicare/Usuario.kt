package com.example.medicare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Usuario(
    @DocumentId
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val fechaNacimiento: Long? = null,
    val fotoPerfil: String? = null,
    val telefonoEmergencia: String = "",
    val contactoEmergencia: String = "",
    val fechaCreacion: Timestamp = Timestamp.now(),
    val ultimoAcceso: Timestamp = Timestamp.now(),
    val activo: Boolean = true
) {
    fun getEdad(): Int? {
        fechaNacimiento?.let {
            val ahora = System.currentTimeMillis()
            val diff = ahora - it
            return (diff / (365.25 * 24 * 60 * 60 * 1000)).toInt()
        }
        return null
    }

    fun tieneContactoEmergencia(): Boolean {
        return telefonoEmergencia.isNotEmpty()
    }
}