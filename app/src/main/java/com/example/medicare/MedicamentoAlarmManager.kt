package com.example.medicare

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import org.greenrobot.eventbus.EventBus
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

// === GESTIÓN DE ALARMAS ===
class MedicamentoAlarmManager(private val context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dbHelper = MedicamentosDBHelper(context)
    private val alarmIdCounter = AtomicInteger(1000)

    companion object {
        const val EXTRA_MEDICAMENTO_ID = "medicamento_id"
        const val EXTRA_RECORDATORIO_ID = "recordatorio_id"
        const val EXTRA_NOMBRE_MEDICAMENTO = "nombre_medicamento"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_FECHA_HORA_ORIGINAL = "fecha_hora_original"
        const val EXTRA_NUMERO_POSTERGACION = "numero_postergacion"
        const val AUTO_POSTPONE_DELAY = 30000L
        const val ESCALAMIENTO_START_DELAY = 15 * 60 * 1000L
    }

    fun programarRecordatoriosMedicamento(medicamento: Medicamento) {
        if (!medicamento.activo) return

        val horaInicio = medicamento.horaInicio ?: medicamento.fechaCreacion
        val intervaloPorHoras = medicamento.horarioHoras
        val ahora = System.currentTimeMillis()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = horaInicio

        if (horaInicio < ahora) {
            val tiempoTranscurrido = ahora - horaInicio
            val intervalosTranscurridos = (tiempoTranscurrido / (intervaloPorHoras * 60 * 60 * 1000)).toInt()
            calendar.add(Calendar.HOUR_OF_DAY, intervaloPorHoras * (intervalosTranscurridos + 1))
        }

        val maxRecordatorios = (30 * 24) / intervaloPorHoras

        for (i in 0 until maxRecordatorios) {
            val fechaHoraRecordatorio = calendar.timeInMillis
            if (fechaHoraRecordatorio < ahora) {
                calendar.add(Calendar.HOUR_OF_DAY, intervaloPorHoras)
                continue
            }

            val alarmId = alarmIdCounter.incrementAndGet()
            val recordatorioId = dbHelper.insertarRecordatorio(medicamento.id, fechaHoraRecordatorio, alarmId)

            if (recordatorioId > 0) {
                programarAlarma(medicamento, recordatorioId, fechaHoraRecordatorio, alarmId)
            }

            calendar.add(Calendar.HOUR_OF_DAY, intervaloPorHoras)
        }
    }

    private fun programarAlarma(medicamento: Medicamento, recordatorioId: Long, fechaHora: Long, alarmId: Int) {
        val intent = Intent(context, MedicamentoAlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEDICAMENTO_ID, medicamento.id)
            putExtra(EXTRA_RECORDATORIO_ID, recordatorioId)
            putExtra(EXTRA_NOMBRE_MEDICAMENTO, medicamento.nombre)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_FECHA_HORA_ORIGINAL, fechaHora)
            putExtra(EXTRA_NUMERO_POSTERGACION, 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fechaHora, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, fechaHora, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("MedicamentoAlarm", "Error programando alarma: ${e.message}")
        }
    }

    fun cancelarAlarma(alarmId: Int) {
        val intent = Intent(context, MedicamentoAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelarTodasAlarmasMedicamento(medicamentoId: Long) {
        val recordatorios = dbHelper.obtenerRecordatoriosPendientes()
        recordatorios.filter { it.medicamentoId == medicamentoId }
            .forEach { cancelarAlarma(it.alarmId) }
        dbHelper.eliminarRecordatoriosPorMedicamento(medicamentoId)
    }

    fun reprogramarRecordatorios() {
        val medicamentos = dbHelper.obtenerTodosMedicamentos()
        medicamentos.filter { it.activo }.forEach { programarRecordatoriosMedicamento(it) }
    }
}

// === RECEPTOR DE ALARMAS UNIFICADO ===
class MedicamentoAlarmReceiver : BroadcastReceiver() {

    private val tag = "MedicamentoAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "📢 Alarma recibida")

        val medicamentoId = intent.getLongExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, -1)
        val recordatorioId = intent.getLongExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, -1)
        val nombreMedicamento = intent.getStringExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO) ?: ""
        val alarmId = intent.getIntExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, -1)
        val fechaHoraOriginal = intent.getLongExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, System.currentTimeMillis())
        val numeroPostergacion = intent.getIntExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, 0)

        Log.d(tag, "Datos: medicamento=$nombreMedicamento, id=$medicamentoId, postergaciones=$numeroPostergacion")

        if (medicamentoId != -1L && recordatorioId != -1L) {
            val serviceIntent = Intent(context, MedicamentoAlarmService::class.java).apply {
                putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
                putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
                putExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO, nombreMedicamento)
                putExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, alarmId)
                putExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, fechaHoraOriginal)
                putExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, numeroPostergacion)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error iniciando servicio: ${e.message}")
            }
        }
    }
}

// === SERVICIO DE ALARMAS CON INTEGRACIÓN DE EMERGENCIA ===
class MedicamentoAlarmService : android.app.Service(), TextToSpeech.OnInitListener {

    private val tag = "MedicamentoAlarmService"

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var textToSpeech: TextToSpeech? = null
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private var autoPostponeRunnable: Runnable? = null
    private var escalamientoStartRunnable: Runnable? = null

    private var medicamentoId: Long = -1
    private var recordatorioId: Long = -1
    private var nombreMedicamento: String = ""
    private var alarmId: Int = -1
    private var fechaHoraOriginal: Long = 0
    private var numeroPostergacion: Int = 0
    private var escalamientoId: Long = -1

    // Referencia al agente de emergencia
    private var agenteEmergencia: AgenteEmergenciaComunicacion? = null

    companion object {
        const val CHANNEL_ID = "medicamento_alarmas"
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOMADO = "ACTION_TOMADO"
        const val ACTION_POSTERGAR = "ACTION_POSTERGAR"
        const val ACTION_CANCELAR = "ACTION_CANCELAR"
        const val AUTO_POSTPONE_DELAY = 30000L
        const val ESCALAMIENTO_START_DELAY = 15 * 60 * 1000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        textToSpeech = TextToSpeech(this, this)

        // Inicializar agente de emergencia
        agenteEmergencia = AgenteEmergenciaComunicacion(this)
        Log.d(tag, "✅ Agente de emergencia inicializado en servicio")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOMADO -> {
                marcarComoTomado()
                return START_NOT_STICKY
            }
            ACTION_POSTERGAR -> {
                postergarRecordatorio()
                return START_NOT_STICKY
            }
            ACTION_CANCELAR -> {
                cancelarAlarma()
                return START_NOT_STICKY
            }
            else -> {
                medicamentoId = intent?.getLongExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, -1) ?: -1
                recordatorioId = intent?.getLongExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, -1) ?: -1
                nombreMedicamento = intent?.getStringExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO) ?: ""
                alarmId = intent?.getIntExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, -1) ?: -1
                fechaHoraOriginal = intent?.getLongExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, System.currentTimeMillis()) ?: System.currentTimeMillis()
                numeroPostergacion = intent?.getIntExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, 0) ?: 0

                if (medicamentoId != -1L && recordatorioId != -1L) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    startAlarm()
                    mostrarPantallaAlarma()
                }
            }
        }
        return START_STICKY
    }

    private fun startAlarm() {
        startSound()
        startVibration()
        speakReminder()

        // Auto-postergar después de 30 segundos
        autoPostponeRunnable = Runnable {
            Log.d(tag, "⏰ Auto-postergando después de 30 segundos sin respuesta")
            postergarRecordatorioAutomatico()
        }
        handler.postDelayed(autoPostponeRunnable!!, AUTO_POSTPONE_DELAY)

        // Iniciar escalamiento si ya lleva muchas postergaciones
        if (numeroPostergacion >= 2) {
            iniciarEscalamiento()
        }
    }

    private fun iniciarEscalamiento() {
        Log.d(tag, "🚨 Iniciando proceso de escalamiento - Postergación #$numeroPostergacion")

        val dbHelper = MedicamentosDBHelper(this)
        escalamientoId = dbHelper.crearEscalamiento(medicamentoId, recordatorioId)

        if (escalamientoId > 0) {
            // Notificar al agente de emergencia
            agenteEmergencia?.procesarPostergacionMedicamento(
                medicamentoId = medicamentoId,
                nombreMedicamento = nombreMedicamento,
                fechaHoraOriginal = fechaHoraOriginal,
                numeroPostergacion = numeroPostergacion
            )

            Log.d(tag, "✅ Escalamiento creado con ID: $escalamientoId")
        }
    }

    private fun postergarRecordatorioAutomatico() {
        Log.d(tag, "🔄 Postergación automática - Count: ${numeroPostergacion + 1}")

        // Notificar al agente sobre la postergación automática
        agenteEmergencia?.procesarPostergacionMedicamento(
            medicamentoId = medicamentoId,
            nombreMedicamento = nombreMedicamento,
            fechaHoraOriginal = fechaHoraOriginal,
            numeroPostergacion = numeroPostergacion + 1
        )

        postergarRecordatorio()
    }

    private fun startSound() {
        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MedicamentoAlarmService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            isPlaying = true
        } catch (e: Exception) {
            Log.e(tag, "Error reproduciendo sonido: ${e.message}")
        }
    }

    private fun startVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error con vibración: ${e.message}")
        }
    }

    private fun speakReminder() {
        handler.postDelayed({
            val mensaje = "Es hora de tomar su medicamento $nombreMedicamento. Debe tomar una dosis."
            textToSpeech?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "medicamento_reminder")
        }, 1000)
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            vibrator?.cancel()
            autoPostponeRunnable?.let { handler.removeCallbacks(it) }
            escalamientoStartRunnable?.let { handler.removeCallbacks(it) }
        } catch (e: Exception) {
            Log.e(tag, "Error deteniendo alarma: ${e.message}")
        }
    }

    private fun mostrarPantallaAlarma() {
        val intent = Intent(this, AlarmOverlayActivity::class.java).apply {
            putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
            putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
            putExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO, nombreMedicamento)
            putExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, alarmId)
            putExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, fechaHoraOriginal)
            putExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, numeroPostergacion)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun marcarComoTomado() {
        Log.d(tag, "✅ Medicamento marcado como tomado: $nombreMedicamento")

        val dbHelper = MedicamentosDBHelper(this)
        dbHelper.marcarRecordatorioCompletado(recordatorioId)

        // Cancelar escalamiento si existe
        if (escalamientoId > 0) {
            dbHelper.marcarEscalamientoCompletado(escalamientoId)
            Log.d(tag, "Escalamiento $escalamientoId cancelado")
        }

        // Registrar en historial
        dbHelper.insertarHistorialToma(
            medicamentoId,
            fechaHoraOriginal,
            System.currentTimeMillis(),
            "tomado",
            1
        )

        // Actualizar cantidad
        val medicamentos = dbHelper.obtenerTodosMedicamentos()
        val medicamento = medicamentos.find { it.id == medicamentoId }
        medicamento?.let { med ->
            val nuevaCantidad = maxOf(0, med.cantidad - 1)
            dbHelper.actualizarCantidadMedicamento(medicamentoId, nuevaCantidad)

            val intent = Intent(MainActivity.ACTION_INVENTORY_UPDATED).apply {
                putExtra("medicamento_nombre", med.nombre)
                putExtra("nueva_cantidad", nuevaCantidad)
            }
            sendBroadcast(intent)

            if (nuevaCantidad == 0) {
                dbHelper.actualizarEstadoMedicamento(medicamentoId, false)
            }
        }

        stopAlarm()
        stopSelf()
    }

    private fun postergarRecordatorio() {
        Log.d(tag, "🔄 Postergando medicamento: $nombreMedicamento (Postergación #${numeroPostergacion + 1})")

        val dbHelper = MedicamentosDBHelper(this)
        val nuevaFechaHora = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutos
        val nuevaPostergacion = numeroPostergacion + 1

        dbHelper.postergarRecordatorio(recordatorioId, nuevaFechaHora)

        // Programar nueva alarma con contador incrementado
        val intent = Intent(this, MedicamentoAlarmReceiver::class.java).apply {
            putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
            putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
            putExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO, nombreMedicamento)
            putExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, alarmId)
            putExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, fechaHoraOriginal)
            putExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, nuevaPostergacion)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManagerSystem = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManagerSystem.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nuevaFechaHora, pendingIntent)
        } else {
            alarmManagerSystem.setExact(AlarmManager.RTC_WAKEUP, nuevaFechaHora, pendingIntent)
        }

        // Registrar en historial
        dbHelper.insertarHistorialToma(
            medicamentoId,
            fechaHoraOriginal,
            null,
            "postergado"
        )

        stopAlarm()
        stopSelf()
    }

    private fun cancelarAlarma() {
        stopAlarm()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordatorios de medicamentos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setBypassDnd(true)
                setShowBadge(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val tomadoIntent = Intent(this, MedicamentoAlarmService::class.java).apply {
            action = ACTION_TOMADO
            putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
            putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
        }
        val tomadoPendingIntent = PendingIntent.getService(
            this, alarmId * 10 + 1, tomadoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val postergarIntent = Intent(this, MedicamentoAlarmService::class.java).apply {
            action = ACTION_POSTERGAR
            putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
            putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
        }
        val postergarPendingIntent = PendingIntent.getService(
            this, alarmId * 10 + 2, postergarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overlayIntent = Intent(this, AlarmOverlayActivity::class.java).apply {
            putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
            putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
            putExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO, nombreMedicamento)
            putExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarmId * 10 + 3, overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("💊 Recordatorio de Medicamento")
            .setContentText("Es hora de tomar $nombreMedicamento")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_preferences, "✅ TOMADO", tomadoPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "⏰ POSTERGAR", postergarPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {
        stopAlarm()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        agenteEmergencia?.destruir()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}