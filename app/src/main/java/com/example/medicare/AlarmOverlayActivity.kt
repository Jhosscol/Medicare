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
        // Layout principal con gradiente oscuro
        val layout = android.widget.RelativeLayout(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#0A1628")) // Azul oscuro
            setPadding(0, 0, 0, 0)
        }
    
        // ScrollView para contenido
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }
    
        val contentLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 60, 24, 40)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
    
        // === HEADER SUPERIOR ===
        val headerLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 30)
        }
    
        // Ícono de reloj animado
        val iconoReloj = android.widget.TextView(this).apply {
            text = "⏰"
            textSize = 36f
            setPadding(0, 0, 15, 0)
        }
    
        val tituloHeader = android.widget.TextView(this).apply {
            text = "RECORDATORIO"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#00D9FF")) // Cian brillante
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.parseColor("#8000D9FF"))
        }
    
        headerLayout.addView(iconoReloj)
        headerLayout.addView(tituloHeader)
    
        // === CARD PRINCIPAL DEL MEDICAMENTO ===
        val medicamentoCard = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(35, 40, 35, 40)
            
            // Fondo con gradiente simulado (colores superpuestos)
            setBackgroundColor(android.graphics.Color.parseColor("#1A2942")) // Azul gris oscuro
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 16f
                translationZ = 16f
            }
        }
    
        // Barra de estado superior de la card
        val statusBar = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 25)
        }
    
        val statusIndicator = android.widget.TextView(this).apply {
            text = "● ACTIVO"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#00FF88")) // Verde neón
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 0f, 0f, android.graphics.Color.parseColor("#8000FF88"))
        }
    
        val horaActual = android.widget.TextView(this).apply {
            text = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#7A8A99"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = android.view.Gravity.END
            }
            gravity = android.view.Gravity.END
        }
    
        statusBar.addView(statusIndicator)
        statusBar.addView(horaActual)
    
        // Ícono grande de medicamento con fondo
        val iconoContainer = android.widget.FrameLayout(this).apply {
            setPadding(0, 0, 0, 25)
        }
    
        val iconoBackground = android.widget.TextView(this).apply {
            text = "⬤"
            textSize = 120f
            setTextColor(android.graphics.Color.parseColor("#2A3F5F")) // Fondo del círculo
            gravity = android.view.Gravity.CENTER
        }
    
        val iconoPastilla = android.widget.TextView(this).apply {
            text = "💊"
            textSize = 64f
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
    
        iconoContainer.addView(iconoBackground)
        iconoContainer.addView(iconoPastilla)
    
        // Etiqueta "Medicamento"
        val etiquetaMed = android.widget.TextView(this).apply {
            text = "MEDICAMENTO"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#7A8A99"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 10)
            letterSpacing = 0.15f
        }
    
        // NOMBRE DEL MEDICAMENTO - Destacado
        val nombreMed = android.widget.TextView(this).apply {
            text = nombreMedicamento
            textSize = 42f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(20, 0, 20, 20)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(10f, 0f, 4f, android.graphics.Color.parseColor("#80000000"))
        }
    
        // Separador con gradiente
        val separador1 = android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#00D9FF"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                120,
                4
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setMargins(0, 15, 0, 25)
            }
        }
    
        // === INFORMACIÓN DE DOSIS ===
        val dosisContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(25, 25, 25, 25)
            setBackgroundColor(android.graphics.Color.parseColor("#0F1F35"))
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 4f
            }
        }
    
        val dosisLabel = android.widget.TextView(this).apply {
            text = "DOSIS A TOMAR"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#7A8A99"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 15)
            letterSpacing = 0.1f
        }
    
        val dosisValor = android.widget.TextView(this).apply {
            text = "1 unidad"
            textSize = 38f
            setTextColor(android.graphics.Color.parseColor("#00FF88")) // Verde neón
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(12f, 0f, 0f, android.graphics.Color.parseColor("#8000FF88"))
        }
    
        dosisContainer.addView(dosisLabel)
        dosisContainer.addView(dosisValor)
    
        // Agregar todo al card principal
        medicamentoCard.addView(statusBar)
        medicamentoCard.addView(iconoContainer)
        medicamentoCard.addView(etiquetaMed)
        medicamentoCard.addView(nombreMed)
        medicamentoCard.addView(separador1)
        medicamentoCard.addView(dosisContainer)
    
        // === ESPACIO ===
        val espacio1 = android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                40
            )
        }
    
        // === BOTONES DE ACCIÓN ===
        
        // Botón TOMADO - Principal
        val btnTomado = android.widget.Button(this).apply {
            text = "✓ YA LO TOMÉ"
            textSize = 24f
            setBackgroundColor(android.graphics.Color.parseColor("#00D98E")) // Verde cian
            setTextColor(android.graphics.Color.WHITE)
            setPadding(40, 55, 40, 55)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 12f
                translationZ = 12f
            }
            
            setOnClickListener {
                animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                marcarComoTomado()
            }
        }
    
        // Espacio entre botones
        val espacio2 = android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }
    
        // Botón POSTERGAR - Secundario
        val btnPostergar = android.widget.Button(this).apply {
            text = "⏱ RECORDAR EN 5 MIN"
            textSize = 20f
            setBackgroundColor(android.graphics.Color.parseColor("#2A3F5F")) // Azul oscuro
            setTextColor(android.graphics.Color.parseColor("#00D9FF"))
            setPadding(40, 45, 40, 45)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = 8f
                translationZ = 8f
            }
            
            setOnClickListener {
                animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                postergarRecordatorio()
            }
        }
    
        // Mensaje informativo abajo
        val infoMensaje = android.widget.TextView(this).apply {
            text = "Toque una opción para continuar"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#5A6A79"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }
    
        // === ENSAMBLAR TODO ===
        contentLayout.addView(headerLayout)
        contentLayout.addView(medicamentoCard)
        contentLayout.addView(espacio1)
        contentLayout.addView(btnTomado)
        contentLayout.addView(espacio2)
        contentLayout.addView(btnPostergar)
        contentLayout.addView(infoMensaje)
    
        scrollView.addView(contentLayout)
        layout.addView(scrollView)
    
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
