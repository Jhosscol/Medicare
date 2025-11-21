package com.example.medicare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class MedicamentoFirebase(
    @DocumentId
    val id: String = "",
    val userId: String = "", // UID del dueño
    val nombre: String = "",
    val cantidad: Int = 0,
    val horarioHoras: Int = 0,
    val horaInicio: Long = 0L,
    val activo: Boolean = true,
    val fechaCreacion: Timestamp = Timestamp.now(),
    val ultimaActualizacion: Timestamp = Timestamp.now(),
    val notas: String = ""
) {
    // Convertir a Medicamento local
    fun toMedicamento(): Medicamento {
        return Medicamento(
            id = id.hashCode().toLong(),
            nombre = nombre,
            cantidad = cantidad,
            horarioHoras = horarioHoras,
            horaInicio = horaInicio,
            activo = activo
        )
    }

    companion object {
        // Crear desde Medicamento local
        fun fromMedicamento(medicamento: Medicamento, userId: String): MedicamentoFirebase {
            return MedicamentoFirebase(
                userId = userId,
                nombre = medicamento.nombre,
                cantidad = medicamento.cantidad,
                horarioHoras = medicamento.horarioHoras,
                horaInicio = medicamento.horaInicio ?: System.currentTimeMillis(),
                activo = medicamento.activo
            )
        }
    }
}