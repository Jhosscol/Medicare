package com.example.medicare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// === CLASES DE EVENTOS ===
data class MedicamentoPostergadoEvent(
    val medicamentoId: Long,
    val nombreMedicamento: String,
    val tiempoPostergado: Int,
    val fechaHoraOriginal: Long,
    val numeroPostergacion: Int
)

data class MedicamentoNoTomadoEvent(
    val medicamentoId: Long,
    val nombreMedicamento: String,
    val fechaHoraOriginal: Long,
    val tiempoTranscurrido: Int
)

data class ContactoEmergenciaEvent(
    val contactoNombre: String,
    val contactoTelefono: String,
    val tipoAccion: String // "MENSAJE" o "LLAMADA"
)

data class ConfiguracionTelegram(
    val botToken: String,
    val chatId: String
)

// === AGENTE DE EMERGENCIA ===
class AgenteEmergenciaComunicacion(private val context: Context) {

    private val tag = "AgenteEmergencia"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var configuracionTelegram: ConfiguracionTelegram? = null
    private val dbHelper = MedicamentosDBHelper(context)

    init {
        try {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }
            Log.d(tag, "✅ Agente de Emergencia inicializado correctamente")

            // Cargar configuración de Telegram si existe
            cargarConfiguracionTelegram()

        } catch (e: Exception) {
            Log.e(tag, "❌ ERROR al inicializar Agente de Emergencia", e)
        }
    }

    // === CARGAR CONFIGURACIÓN GUARDADA ===
    private fun cargarConfiguracionTelegram() {
        try {
            val prefs = context.getSharedPreferences("emergencia_config", Context.MODE_PRIVATE)
            val botToken = prefs.getString("telegram_bot_token", null)
            val chatId = prefs.getString("telegram_chat_id", null)

            if (!botToken.isNullOrEmpty() && !chatId.isNullOrEmpty()) {
                configuracionTelegram = ConfiguracionTelegram(botToken, chatId)
                Log.d(tag, "✅ Configuración de Telegram cargada")
            } else {
                Log.w(tag, "⚠️ Telegram no configurado")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error cargando config Telegram: ${e.message}")
        }
    }

    // === MANEJO DE EVENTOS ===

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMedicamentoPostergado(event: MedicamentoPostergadoEvent) {
        Log.d(tag, "📋 Evento recibido: Medicamento postergado")
        Log.i(tag, "💊 Medicamento: ${event.nombreMedicamento}")
        Log.i(tag, "⏱️ Tiempo postergado: ${event.tiempoPostergado} minutos")
        Log.i(tag, "🔄 Postergación número: ${event.numeroPostergacion}")

        when {
            event.numeroPostergacion >= 4 -> {
                Log.e(tag, "🆘 ACTIVANDO PROTOCOLO CRÍTICO: Llamada de emergencia")
                enviarMensajeEmergenciaCritico(event)
                realizarLlamadaEmergencia(event)
            }
            event.numeroPostergacion >= 3 -> {
                Log.w(tag, "⚠️ ACTIVANDO PROTOCOLO: Mensaje de emergencia")
                enviarMensajeEmergencia(event)
            }
            else -> {
                Log.d(tag, "📊 Postergación #${event.numeroPostergacion} - Continuando monitoreo")
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMedicamentoNoTomado(event: MedicamentoNoTomadoEvent) {
        Log.d(tag, "📋 Evento recibido: Medicamento no tomado")
        Log.i(tag, "💊 Medicamento: ${event.nombreMedicamento}")
        Log.i(tag, "⏱️ Tiempo transcurrido: ${event.tiempoTranscurrido} minutos")

        if (event.tiempoTranscurrido >= 20) {
            Log.w(tag, "⚠️ ACTIVANDO PROTOCOLO: Mensaje por medicamento no tomado")
            enviarMensajeEmergenciaMedicamentoNoTomado(event)
        }
    }

    // === CONFIGURACIÓN ===

    fun configurarTelegram(botToken: String, chatId: String) {
        try {
            configuracionTelegram = ConfiguracionTelegram(botToken, chatId)

            // Guardar en SharedPreferences
            val prefs = context.getSharedPreferences("emergencia_config", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("telegram_bot_token", botToken)
                .putString("telegram_chat_id", chatId)
                .apply()

            Log.d(tag, "✅ Telegram configurado y guardado")

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "✅ Telegram configurado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Error al configurar Telegram", e)
        }
    }

    fun estaConfiguradoTelegram(): Boolean {
        return configuracionTelegram != null
    }

    // === OBTENCIÓN DE CONTACTO DE EMERGENCIA ===

    private fun obtenerContactoEmergencia(): ContactoEmergencia? {
        Log.d(tag, "🔍 Buscando contacto de emergencia...")

        // Primero intentar desde la base de datos local
        val contactoLocal = dbHelper.obtenerContactoEmergencia()
        if (contactoLocal != null) {
            Log.d(tag, "✅ Contacto encontrado en BD: ${contactoLocal.nombre}")
            return contactoLocal
        }

        // Si no hay en BD, buscar en contactos del teléfono
        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1",
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nombre = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val telefono = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                    Log.d(tag, "✅ Contacto favorito encontrado: $nombre")
                    return ContactoEmergencia(nombre, telefono.replace("\\s".toRegex(), ""))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Error al obtener contacto de emergencia", e)
        }

        Log.e(tag, "❌ No se encontró contacto de emergencia")
        return null
    }

    // === ENVÍO DE MENSAJES POR TELEGRAM ===

    private fun enviarMensajeEmergencia(event: MedicamentoPostergadoEvent) {
        Log.d(tag, "📤 Preparando mensaje de emergencia...")

        val contacto = obtenerContactoEmergencia()
        val nombrePaciente = dbHelper.obtenerNombrePaciente() ?: "El paciente"

        val fechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(event.fechaHoraOriginal))

        val mensaje = """
🚨 *ALERTA MEDICAMENTO* 🚨

$nombrePaciente no ha tomado su medicamento:

💊 *Medicamento:* ${event.nombreMedicamento}
⏰ *Hora programada:* $fechaHora
⏱️ *Tiempo transcurrido:* ${event.tiempoPostergado} minutos
📊 *Postergaciones:* ${event.numeroPostergacion}

Por favor, contacte al paciente para verificar su estado.

${if (contacto != null) "📞 Contacto de emergencia: ${contacto.nombre}" else ""}
        """.trimIndent()

        enviarMensajeTelegram(mensaje)

        contacto?.let {
            EventBus.getDefault().post(ContactoEmergenciaEvent(it.nombre, it.telefono, "MENSAJE"))
        }
    }

    private fun enviarMensajeEmergenciaCritico(event: MedicamentoPostergadoEvent) {
        Log.e(tag, "🆘 Enviando mensaje CRÍTICO...")

        val nombrePaciente = dbHelper.obtenerNombrePaciente() ?: "El paciente"

        val mensaje = """
🆘 *EMERGENCIA CRÍTICA* 🆘

⚠️ *SITUACIÓN URGENTE*

$nombrePaciente NO ha tomado su medicamento después de múltiples intentos:

💊 *Medicamento:* ${event.nombreMedicamento}
⏱️ *Sin tomar por:* ${event.tiempoPostergado} minutos
📊 *Postergaciones:* ${event.numeroPostergacion}

☎️ *LLAMADA AUTOMÁTICA INICIÁNDOSE*

🚨 CONTACTE INMEDIATAMENTE AL PACIENTE 🚨
        """.trimIndent()

        enviarMensajeTelegram(mensaje)
    }

    private fun enviarMensajeEmergenciaMedicamentoNoTomado(event: MedicamentoNoTomadoEvent) {
        Log.d(tag, "📤 Mensaje por medicamento no tomado...")

        val nombrePaciente = dbHelper.obtenerNombrePaciente() ?: "El paciente"
        val fechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(event.fechaHoraOriginal))

        val mensaje = """
⚠️ *MEDICAMENTO NO TOMADO* ⚠️

$nombrePaciente no ha confirmado la toma de su medicamento:

💊 *Medicamento:* ${event.nombreMedicamento}
⏰ *Hora programada:* $fechaHora
⏱️ *Tiempo sin tomar:* ${event.tiempoTranscurrido} minutos

Por favor, verifique el estado del paciente.
        """.trimIndent()

        enviarMensajeTelegram(mensaje)
    }

    private fun enviarMensajeTelegram(mensaje: String) {
        val config = configuracionTelegram
        if (config == null) {
            Log.e(tag, "❌ Telegram no configurado - no se puede enviar mensaje")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "⚠️ Configure Telegram para recibir alertas", Toast.LENGTH_LONG).show()
            }
            return
        }

        Log.d(tag, "🚀 Enviando mensaje a Telegram...")

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.telegram.org/bot${config.botToken}/sendMessage"

                val requestBody = FormBody.Builder()
                    .add("chat_id", config.chatId)
                    .add("text", mensaje)
                    .add("parse_mode", "Markdown")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(tag, "✅ Mensaje de Telegram enviado exitosamente")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "✅ Alerta enviada", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(tag, "❌ Error Telegram: ${response.code} - ${response.body?.string()}")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "❌ Error enviando alerta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "❌ Error de red: ${e.message}")
            } catch (e: Exception) {
                Log.e(tag, "❌ Error inesperado: ${e.message}")
            }
        }
    }

    // === LLAMADAS DE EMERGENCIA ===

    private fun realizarLlamadaEmergencia(event: MedicamentoPostergadoEvent) {
        Log.e(tag, "🆘 INICIANDO LLAMADA DE EMERGENCIA")

        val contacto = obtenerContactoEmergencia()
        if (contacto == null) {
            Log.e(tag, "❌ No hay contacto de emergencia configurado")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "🆘 Configure un contacto de emergencia", Toast.LENGTH_LONG).show()
            }
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contacto.telefono}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            Log.d(tag, "☎️ Llamando a: ${contacto.telefono}")
            context.startActivity(intent)

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "☎️ Llamando a ${contacto.nombre}", Toast.LENGTH_LONG).show()
            }

            EventBus.getDefault().post(ContactoEmergenciaEvent(contacto.nombre, contacto.telefono, "LLAMADA"))

        } catch (e: Exception) {
            Log.e(tag, "❌ Error al realizar llamada: ${e.message}")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "❌ Error al llamar", Toast.LENGTH_LONG).show()
            }
        }
    }

    // === MÉTODOS PÚBLICOS PARA INTEGRACIÓN ===

    fun procesarPostergacionMedicamento(
        medicamentoId: Long,
        nombreMedicamento: String,
        fechaHoraOriginal: Long,
        numeroPostergacion: Int
    ) {
        Log.d(tag, "🔄 Procesando postergación...")
        Log.i(tag, "💊 Nombre: $nombreMedicamento, Postergación #$numeroPostergacion")

        val tiempoTranscurrido = ((System.currentTimeMillis() - fechaHoraOriginal) / (1000 * 60)).toInt()

        val event = MedicamentoPostergadoEvent(
            medicamentoId = medicamentoId,
            nombreMedicamento = nombreMedicamento,
            tiempoPostergado = tiempoTranscurrido,
            fechaHoraOriginal = fechaHoraOriginal,
            numeroPostergacion = numeroPostergacion
        )

        EventBus.getDefault().post(event)
    }

    fun procesarMedicamentoNoTomado(
        medicamentoId: Long,
        nombreMedicamento: String,
        fechaHoraOriginal: Long
    ) {
        Log.d(tag, "⚠️ Procesando medicamento no tomado...")

        val tiempoTranscurrido = ((System.currentTimeMillis() - fechaHoraOriginal) / (1000 * 60)).toInt()

        val event = MedicamentoNoTomadoEvent(
            medicamentoId = medicamentoId,
            nombreMedicamento = nombreMedicamento,
            fechaHoraOriginal = fechaHoraOriginal,
            tiempoTranscurrido = tiempoTranscurrido
        )

        EventBus.getDefault().post(event)
    }

    // Método para probar la conexión de Telegram
    fun probarConexionTelegram(callback: (Boolean, String) -> Unit) {
        val config = configuracionTelegram
        if (config == null) {
            callback(false, "Telegram no configurado")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.telegram.org/bot${config.botToken}/sendMessage"
                val mensaje = "✅ Prueba de conexión MediCare - ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"

                val requestBody = FormBody.Builder()
                    .add("chat_id", config.chatId)
                    .add("text", mensaje)
                    .build()

                val request = Request.Builder().url(url).post(requestBody).build()

                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            callback(true, "Conexión exitosa")
                        } else {
                            callback(false, "Error: ${response.code}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, "Error: ${e.message}")
                }
            }
        }
    }

    fun destruir() {
        try {
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
            coroutineScope.cancel()
            Log.d(tag, "✅ Agente de Emergencia destruido")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error al destruir Agente: ${e.message}")
        }
    }
}

// === SERVICIO DE MONITOREO ===
class ServicioMonitorMedicamentosActualizado : Service() {

    private val tag = "ServicioMonitor"
    private lateinit var agenteEmergencia: AgenteEmergenciaComunicacion
    private lateinit var dbHelper: MedicamentosDBHelper
    private lateinit var handler: Handler
    private val intervaloChequeo = 2 * 60 * 1000L // 2 minutos

    private val runnable = object : Runnable {
        override fun run() {
            Log.d(tag, "🔍 Verificando medicamentos...")
            verificarMedicamentosEmergencia()
            handler.postDelayed(this, intervaloChequeo)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "🚀 Iniciando ServicioMonitor...")

        try {
            agenteEmergencia = AgenteEmergenciaComunicacion(this)
            dbHelper = MedicamentosDBHelper(this)
            handler = Handler(Looper.getMainLooper())

            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }

            handler.post(runnable)
            Log.d(tag, "✅ Servicio iniciado correctamente")

        } catch (e: Exception) {
            Log.e(tag, "❌ Error al iniciar servicio", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onContactoEmergencia(event: ContactoEmergenciaEvent) {
        Log.d(tag, "📞 Acción: ${event.tipoAccion} a ${event.contactoNombre}")
        Toast.makeText(this, "🚨 ${event.tipoAccion}: ${event.contactoNombre}", Toast.LENGTH_SHORT).show()
    }

    private fun verificarMedicamentosEmergencia() {
        try {
            val recordatoriosEmergencia = dbHelper.obtenerRecordatoriosParaEmergencia()
            val tiempoActual = System.currentTimeMillis()

            Log.d(tag, "📊 Recordatorios pendientes: ${recordatoriosEmergencia.size}")

            for (recordatorio in recordatoriosEmergencia) {
                val tiempoTranscurrido = ((tiempoActual - recordatorio.fechaOriginal) / (1000 * 60)).toInt()

                Log.d(tag, "💊 ${recordatorio.nombreMedicamento}: $tiempoTranscurrido min, ${recordatorio.numeroPostergaciones} postergaciones")

                when {
                    tiempoTranscurrido >= 60 && recordatorio.numeroPostergaciones >= 4 -> {
                        Log.e(tag, "🆘 PROTOCOLO CRÍTICO para ${recordatorio.nombreMedicamento}")
                        agenteEmergencia.procesarPostergacionMedicamento(
                            recordatorio.medicamentoId,
                            recordatorio.nombreMedicamento,
                            recordatorio.fechaOriginal,
                            recordatorio.numeroPostergaciones
                        )
                        dbHelper.marcarNotificacionEnviada(recordatorio.id)
                    }
                    tiempoTranscurrido >= 20 && !recordatorio.notificacionEnviada -> {
                        Log.w(tag, "⚠️ Alerta para ${recordatorio.nombreMedicamento}")
                        if (recordatorio.numeroPostergaciones > 0) {
                            agenteEmergencia.procesarPostergacionMedicamento(
                                recordatorio.medicamentoId,
                                recordatorio.nombreMedicamento,
                                recordatorio.fechaOriginal,
                                recordatorio.numeroPostergaciones
                            )
                        } else {
                            agenteEmergencia.procesarMedicamentoNoTomado(
                                recordatorio.medicamentoId,
                                recordatorio.nombreMedicamento,
                                recordatorio.fechaOriginal
                            )
                        }
                        dbHelper.marcarNotificacionEnviada(recordatorio.id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Error verificando: ${e.message}")
        }
    }

    override fun onDestroy() {
        Log.d(tag, "🔴 Deteniendo ServicioMonitor...")
        try {
            handler.removeCallbacks(runnable)
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
            agenteEmergencia.destruir()
        } catch (e: Exception) {
            Log.e(tag, "Error al detener: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}