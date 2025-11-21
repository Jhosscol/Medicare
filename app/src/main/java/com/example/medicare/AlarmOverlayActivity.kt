package com.example.medicare

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmOverlayActivity : Activity() {

    private var medicamentoId: Long = -1
    private var recordatorioId: Long = -1
    private var nombreMedicamento: String = ""
    private var alarmId: Int = -1
    private var fechaHoraOriginal: Long = 0
    private var numeroPostergacion: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar para mostrar sobre pantalla de bloqueo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Obtener datos del intent
        medicamentoId = intent.getLongExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, -1)
        recordatorioId = intent.getLongExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, -1)
        nombreMedicamento = intent.getStringExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO) ?: ""
        alarmId = intent.getIntExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, -1)
        fechaHoraOriginal = intent.getLongExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, System.currentTimeMillis())
        numeroPostergacion = intent.getIntExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, 0)

        createAlarmLayout()
    }

    private fun createAlarmLayout() {
        // Layout principal
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 80)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        // Card contenedor
        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.WHITE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 24f
            }
        }

        // Ícono de pastilla
        val iconoPastilla = TextView(this).apply {
            text = "💊"
            textSize = 80f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        // Título
        val titulo = TextView(this).apply {
            text = "Es hora de tomar su medicamento"
            textSize = 28f
            setTextColor(Color.parseColor("#2C3E50"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 0f, 2f, Color.parseColor("#40000000"))
        }

        // Nombre del medicamento
        val nombreMed = TextView(this).apply {
            text = nombreMedicamento
            textSize = 48f
            setTextColor(Color.parseColor("#E74C3C"))
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#FFEBEE"))
            setShadowLayer(3f, 0f, 3f, Color.parseColor("#60000000"))
        }

        // Espacio
        val espacio1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 40
            )
        }

        // Mensaje de dosis
        val mensaje = TextView(this).apply {
            text = "Dosis a tomar:"
            textSize = 24f
            setTextColor(Color.parseColor("#34495E"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        // Dosis
        val dosis = TextView(this).apply {
            text = "1 unidad"
            textSize = 36f
            setTextColor(Color.parseColor("#27AE60"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 0f, 2f, Color.parseColor("#40000000"))
        }

        // Mostrar contador de postergaciones si hay
        val postergacionInfo = if (numeroPostergacion > 0) {
            TextView(this).apply {
                text = "⚠️ Postergado $numeroPostergacion ${if (numeroPostergacion == 1) "vez" else "veces"}"
                textSize = 18f
                setTextColor(Color.parseColor("#E67E22"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 20)
            }
        } else null

        // Separador
        val separador = View(this).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 3
            ).apply {
                setMargins(0, 20, 0, 40)
            }
        }

        // Layout para botones
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        // Botón TOMADO
        val btnTomado = Button(this).apply {
            text = "✓ YA TOMÉ MI MEDICAMENTO"
            textSize = 26f
            setBackgroundColor(Color.parseColor("#27AE60"))
            setTextColor(Color.WHITE)
            setPadding(40, 50, 40, 50)
            typeface = Typeface.DEFAULT_BOLD
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 8f
                translationZ = 8f
            }
            setOnClickListener {
                alpha = 0.7f
                postDelayed({ alpha = 1f }, 100)
                marcarComoTomado()
            }
        }

        // Espacio entre botones
        val espacio2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 30
            )
        }

        // Botón POSTERGAR
        val btnPostergar = Button(this).apply {
            text = "⏱ RECORDAR EN 5 MINUTOS"
            textSize = 24f
            setBackgroundColor(Color.parseColor("#3498DB"))
            setTextColor(Color.WHITE)
            setPadding(40, 45, 40, 45)
            typeface = Typeface.DEFAULT_BOLD
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 8f
                translationZ = 8f
            }
            setOnClickListener {
                alpha = 0.7f
                postDelayed({ alpha = 1f }, 100)
                postergarRecordatorio()
            }
        }

        // Parámetros para botones
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Agregar elementos al card
        cardContainer.addView(iconoPastilla)
        cardContainer.addView(titulo)
        cardContainer.addView(nombreMed)
        cardContainer.addView(espacio1)
        cardContainer.addView(mensaje)
        cardContainer.addView(dosis)
        postergacionInfo?.let { cardContainer.addView(it) }
        cardContainer.addView(separador)

        // Agregar botones
        buttonLayout.addView(btnTomado, buttonParams)
        buttonLayout.addView(espacio2)
        buttonLayout.addView(btnPostergar, buttonParams)

        cardContainer.addView(buttonLayout)

        // Parámetros para el card
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(30, 30, 30, 30)
        }

        layout.addView(cardContainer, cardParams)
        setContentView(layout)
    }

    private fun marcarComoTomado() {
        val intent = Intent(this, MedicamentoAlarmService::class.java).apply {
            action = MedicamentoAlarmService.ACTION_TOMADO
            putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
            putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
            putExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO, nombreMedicamento)
            putExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, alarmId)
            putExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, fechaHoraOriginal)
            putExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, numeroPostergacion)
        }
        startService(intent)
        finish()
    }

    private fun postergarRecordatorio() {
        val intent = Intent(this, MedicamentoAlarmService::class.java).apply {
            action = MedicamentoAlarmService.ACTION_POSTERGAR
            putExtra(MedicamentoAlarmManager.EXTRA_MEDICAMENTO_ID, medicamentoId)
            putExtra(MedicamentoAlarmManager.EXTRA_RECORDATORIO_ID, recordatorioId)
            putExtra(MedicamentoAlarmManager.EXTRA_NOMBRE_MEDICAMENTO, nombreMedicamento)
            putExtra(MedicamentoAlarmManager.EXTRA_ALARM_ID, alarmId)
            putExtra(MedicamentoAlarmManager.EXTRA_FECHA_HORA_ORIGINAL, fechaHoraOriginal)
            putExtra(MedicamentoAlarmManager.EXTRA_NUMERO_POSTERGACION, numeroPostergacion)
        }
        startService(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No permitir cerrar con botón atrás
        // La alarma se debe responder obligatoriamente
    }
}