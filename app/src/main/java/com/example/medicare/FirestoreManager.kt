package com.example.medicare

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreManager {

    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirestoreManager"
        private const val USERS_COLLECTION = "users"
        private const val MEDICAMENTOS_COLLECTION = "medicamentos"
        private const val HISTORIAL_COLLECTION = "historial"
    }

    // === MEDICAMENTOS ===

    // Crear medicamento
    suspend fun crearMedicamento(
        userId: String,
        medicamento: MedicamentoFirebase
    ): Result<String> {
        return try {
            val docRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MEDICAMENTOS_COLLECTION)
                .add(medicamento.copy(userId = userId, fechaCreacion = Timestamp.now()))
                .await()

            Log.d(TAG, "✅ Medicamento creado: ${docRef.id}")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando medicamento: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Obtener todos los medicamentos del usuario
    suspend fun obtenerMedicamentos(userId: String): Result<List<MedicamentoFirebase>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MEDICAMENTOS_COLLECTION)
                .whereEqualTo("activo", true)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get()
                .await()

            val medicamentos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(MedicamentoFirebase::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "✅ ${medicamentos.size} medicamentos obtenidos")
            Result.success(medicamentos)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo medicamentos: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Observar medicamentos en tiempo real
    fun observarMedicamentos(userId: String): Flow<List<MedicamentoFirebase>> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(MEDICAMENTOS_COLLECTION)
            .whereEqualTo("activo", true)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observando medicamentos: ${error.message}")
                    return@addSnapshotListener
                }

                val medicamentos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MedicamentoFirebase::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(medicamentos)
            }

        awaitClose { listener.remove() }
    }

    // Actualizar cantidad de medicamento
    suspend fun actualizarCantidad(
        userId: String,
        medicamentoId: String,
        nuevaCantidad: Int
    ): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MEDICAMENTOS_COLLECTION)
                .document(medicamentoId)
                .update(
                    mapOf(
                        "cantidad" to nuevaCantidad,
                        "ultimaActualizacion" to Timestamp.now()
                    )
                )
                .await()

            Log.d(TAG, "✅ Cantidad actualizada: $medicamentoId -> $nuevaCantidad")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando cantidad: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Actualizar medicamento completo
    suspend fun actualizarMedicamento(
        userId: String,
        medicamentoId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            val finalUpdates = updates.toMutableMap()
            finalUpdates["ultimaActualizacion"] = Timestamp.now()

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MEDICAMENTOS_COLLECTION)
                .document(medicamentoId)
                .update(finalUpdates)
                .await()

            Log.d(TAG, "✅ Medicamento actualizado: $medicamentoId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando medicamento: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Eliminar medicamento (soft delete)
    suspend fun eliminarMedicamento(userId: String, medicamentoId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MEDICAMENTOS_COLLECTION)
                .document(medicamentoId)
                .update("activo", false)
                .await()

            Log.d(TAG, "✅ Medicamento eliminado: $medicamentoId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando medicamento: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === HISTORIAL DE TOMAS ===

    // Registrar toma de medicamento
    suspend fun registrarToma(
        userId: String,
        medicamentoId: String,
        medicamentoNombre: String,
        confirmada: Boolean = true,
        pospuesta: Boolean = false
    ): Result<String> {
        return try {
            val toma = HistorialToma(
                userId = userId,
                medicamentoId = medicamentoId,
                medicamentoNombre = medicamentoNombre,
                fechaToma = Timestamp.now(),
                confirmada = confirmada,
                pospuesta = pospuesta
            )

            val docRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(HISTORIAL_COLLECTION)
                .add(toma)
                .await()

            Log.d(TAG, "✅ Toma registrada: ${docRef.id}")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registrando toma: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Obtener historial
    suspend fun obtenerHistorial(
        userId: String,
        limite: Int = 50
    ): Result<List<HistorialToma>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(HISTORIAL_COLLECTION)
                .orderBy("fechaToma", Query.Direction.DESCENDING)
                .limit(limite.toLong())
                .get()
                .await()

            val historial = snapshot.documents.mapNotNull { doc ->
                doc.toObject(HistorialToma::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "✅ ${historial.size} registros de historial obtenidos")
            Result.success(historial)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo historial: ${e.message}", e)
            Result.failure(e)
        }
    }

    // === SINCRONIZACIÓN ===

    // Sincronizar medicamento local con Firebase
    suspend fun sincronizarMedicamento(
        userId: String,
        medicamentoLocal: Medicamento
    ): Result<String> {
        return try {
            Log.d(TAG, "🔄 Iniciando sincronización de: ${medicamentoLocal.nombre}")
            Log.d(TAG, "UserId: $userId")
            Log.d(TAG, "Cantidad: ${medicamentoLocal.cantidad}, Horario: ${medicamentoLocal.horarioHoras}h")

            val medicamentoFirebase = MedicamentoFirebase.fromMedicamento(medicamentoLocal, userId)

            // Buscar si ya existe
            Log.d(TAG, "🔍 Buscando medicamento existente en Firebase...")
            val existente = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(MEDICAMENTOS_COLLECTION)
                .whereEqualTo("nombre", medicamentoLocal.nombre)
                .whereEqualTo("activo", true)
                .get()
                .await()

            if (existente.documents.isNotEmpty()) {
                // ✅ ACTUALIZAR EXISTENTE
                val docId = existente.documents[0].id
                Log.d(TAG, "📝 Medicamento ya existe en Firebase (ID: $docId), actualizando...")

                val updates = mutableMapOf<String, Any>(
                    "cantidad" to medicamentoLocal.cantidad,
                    "horarioHoras" to medicamentoLocal.horarioHoras,
                    "ultimaActualizacion" to Timestamp.now()
                )

                medicamentoLocal.horaInicio?.let {
                    updates["horaInicio"] = it
                }

                // ✅ ESPERAR RESULTADO DE LA ACTUALIZACIÓN
                val resultadoActualizacion = actualizarMedicamento(userId, docId, updates)

                resultadoActualizacion.fold(
                    onSuccess = {
                        Log.d(TAG, "✅ Medicamento actualizado en Firebase: $docId")
                        Result.success(docId)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error actualizando en Firebase: ${error.message}")
                        Result.failure(error)
                    }
                )
            } else {
                // ✅ CREAR NUEVO
                Log.d(TAG, "➕ Medicamento no existe, creando nuevo en Firebase...")

                val resultadoCreacion = crearMedicamento(userId, medicamentoFirebase)

                resultadoCreacion.fold(
                    onSuccess = { firebaseId ->
                        Log.d(TAG, "✅ Medicamento creado en Firebase: $firebaseId")
                        Result.success(firebaseId)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error creando en Firebase: ${error.message}")
                        Result.failure(error)
                    }
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción en sincronización: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Sincronizar todos los medicamentos locales
    suspend fun sincronizarTodos(
        userId: String,
        medicamentosLocales: List<Medicamento>
    ): Result<Int> {
        return try {
            Log.d(TAG, "")
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "🔄 SINCRONIZANDO TODOS LOS MEDICAMENTOS")
            Log.d(TAG, "UserId: $userId")
            Log.d(TAG, "Total a sincronizar: ${medicamentosLocales.size}")
            Log.d(TAG, "════════════════════════════════════════")

            var sincronizados = 0
            var errores = 0

            medicamentosLocales.forEachIndexed { index, medicamento ->
                Log.d(TAG, "")
                Log.d(TAG, "[${index + 1}/${medicamentosLocales.size}] Sincronizando: ${medicamento.nombre}")

                val resultado = sincronizarMedicamento(userId, medicamento)

                resultado.fold(
                    onSuccess = { firebaseId ->
                        sincronizados++
                        Log.d(TAG, "  ✅ Sincronizado: ${medicamento.nombre} (ID: $firebaseId)")
                    },
                    onFailure = { error ->
                        errores++
                        Log.e(TAG, "  ❌ Error: ${medicamento.nombre} - ${error.message}")
                    }
                )
            }

            Log.d(TAG, "")
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "✅ Sincronización completada")
            Log.d(TAG, "Exitosos: $sincronizados/${medicamentosLocales.size}")
            Log.d(TAG, "Errores: $errores")
            Log.d(TAG, "════════════════════════════════════════")
            Log.d(TAG, "")

            if (sincronizados > 0) {
                Result.success(sincronizados)
            } else {
                Result.failure(Exception("No se pudo sincronizar ningún medicamento"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en sincronización masiva: ${e.message}", e)
            Result.failure(e)
        }
    }
}