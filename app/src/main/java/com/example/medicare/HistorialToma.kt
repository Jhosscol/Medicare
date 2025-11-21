package com.example.medicare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class HistorialToma(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val medicamentoId: String = "",
    val medicamentoNombre: String = "",
    val fechaToma: Timestamp = Timestamp.now(),
    val confirmada: Boolean = true,
    val pospuesta: Boolean = false,
    val notas: String = ""
)