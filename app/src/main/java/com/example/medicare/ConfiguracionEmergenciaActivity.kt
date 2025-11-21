package com.example.medicare

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConfiguracionEmergenciaActivity : AppCompatActivity() {
    private lateinit var dbHelper: MedicamentosDBHelper
    private lateinit var agenteEmergencia: AgenteEmergenciaComunicacion

    // Views existentes
    private lateinit var etNombrePaciente: EditText
    private lateinit var etNombreContacto: EditText
    private lateinit var etTelefonoContacto: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnProbar: Button

    // Views nuevas para Telegram
    private lateinit var etBotToken: EditText
    private lateinit var etChatId: EditText
    private lateinit var btnProbarTelegram: Button
    private lateinit var tvEstadoTelegram: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = MedicamentosDBHelper(this)
        agenteEmergencia = AgenteEmergenciaComunicacion(this)

        createLayout()
        cargarConfiguracionExistente()
    }

    private fun createLayout() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // Título
        val titulo = TextView(this).apply {
            text = "⚙️ Configuración de Emergencia"
            textSize = 24f
            setTextColor(Color.parseColor("#1976D2"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
            typeface = Typeface.DEFAULT_BOLD
        }

        // === SECCIÓN: DATOS DEL PACIENTE ===
        val lblNombrePaciente = TextView(this).apply {
            text = "👤 Nombre del Paciente"
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 20, 0, 8)
            typeface = Typeface.DEFAULT_BOLD
        }

        etNombrePaciente = EditText(this).apply {
            hint = "Ingrese el nombre del paciente"
            textSize = 16f
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        // === SECCIÓN: CONTACTO DE EMERGENCIA ===
        val lblNombreContacto = TextView(this).apply {
            text = "👨‍👩‍👧‍👦 Nombre del Contacto de Emergencia"
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 20, 0, 8)
            typeface = Typeface.DEFAULT_BOLD
        }

        etNombreContacto = EditText(this).apply {
            hint = "Ej: María González (hija)"
            textSize = 16f
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        val lblTelefonoContacto = TextView(this).apply {
            text = "📞 Teléfono del Contacto"
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 20, 0, 8)
            typeface = Typeface.DEFAULT_BOLD
        }

        etTelefonoContacto = EditText(this).apply {
            hint = "Ej: +51 987 654 321"
            textSize = 16f
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_PHONE
        }

        // Información
        val infoText = TextView(this).apply {
            text = "ℹ️ Este contacto será notificado si no respondes a los recordatorios de medicamentos después de múltiples postergaciones."
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#E3F2FD"))
        }

        // === SECCIÓN: TELEGRAM ===
        val separador = View(this).apply {
            setBackgroundColor(Color.parseColor("#BDBDBD"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply { setMargins(0, 30, 0, 20) }
        }

        val lblTelegram = TextView(this).apply {
            text = "📱 Configuración de Telegram (Opcional)"
            textSize = 18f
            setTextColor(Color.parseColor("#1976D2"))
            setPadding(0, 10, 0, 10)
            typeface = Typeface.DEFAULT_BOLD
        }

        val infoTelegram = TextView(this).apply {
            text = """
Telegram permite recibir alertas instantáneas cuando no se toma un medicamento.

Para configurar:
1. Busca @BotFather en Telegram
2. Envía /newbot y sigue las instrucciones
3. Copia el Token que te da
4. Busca @userinfobot para obtener tu Chat ID
            """.trimIndent()
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            setPadding(20, 10, 20, 20)
            setBackgroundColor(Color.parseColor("#FFF3E0"))
        }

        val lblBotToken = TextView(this).apply {
            text = "🤖 Bot Token"
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 15, 0, 8)
            typeface = Typeface.DEFAULT_BOLD
        }

        etBotToken = EditText(this).apply {
            hint = "Ej: 123456789:ABCdefGHI..."
            textSize = 14f
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val lblChatId = TextView(this).apply {
            text = "💬 Chat ID"
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 15, 0, 8)
            typeface = Typeface.DEFAULT_BOLD
        }

        etChatId = EditText(this).apply {
            hint = "Ej: 987654321"
            textSize = 14f
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        btnProbarTelegram = Button(this).apply {
            text = "🔄 Probar Telegram"
            textSize = 14f
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(20, 15, 20, 15)
            setOnClickListener { probarConexionTelegram() }
        }

        tvEstadoTelegram = TextView(this).apply {
            text = ""
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }

        // === BOTONES PRINCIPALES ===
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }

        btnProbar = Button(this).apply {
            text = "🧪 PROBAR"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#FF9800"))
            setTextColor(Color.WHITE)
            setPadding(30, 20, 30, 20)
            setOnClickListener { probarConfiguracion() }
        }

        btnGuardar = Button(this).apply {
            text = "💾 GUARDAR"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setPadding(30, 20, 30, 20)
            setOnClickListener { guardarConfiguracion() }
        }

        val buttonParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ).apply { setMargins(10, 0, 10, 0) }

        buttonLayout.addView(btnProbar, buttonParams)
        buttonLayout.addView(btnGuardar, buttonParams)

        // Agregar todos los elementos
        layout.addView(titulo)
        layout.addView(lblNombrePaciente)
        layout.addView(etNombrePaciente)
        layout.addView(lblNombreContacto)
        layout.addView(etNombreContacto)
        layout.addView(lblTelefonoContacto)
        layout.addView(etTelefonoContacto)
        layout.addView(infoText)

        // Sección Telegram
        layout.addView(separador)
        layout.addView(lblTelegram)
        layout.addView(infoTelegram)
        layout.addView(lblBotToken)
        layout.addView(etBotToken)
        layout.addView(lblChatId)
        layout.addView(etChatId)
        layout.addView(btnProbarTelegram)
        layout.addView(tvEstadoTelegram)

        layout.addView(buttonLayout)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun cargarConfiguracionExistente() {
        val sharedPrefs = getSharedPreferences("medicare_config", Context.MODE_PRIVATE)
        val telegramPrefs = getSharedPreferences("emergencia_config", Context.MODE_PRIVATE)

        etNombrePaciente.setText(sharedPrefs.getString("nombre_paciente", ""))
        etNombreContacto.setText(sharedPrefs.getString("contacto_nombre", ""))
        etTelefonoContacto.setText(sharedPrefs.getString("contacto_telefono", ""))

        // Cargar config Telegram
        etBotToken.setText(telegramPrefs.getString("telegram_bot_token", ""))
        etChatId.setText(telegramPrefs.getString("telegram_chat_id", ""))

        // Mostrar estado si ya está configurado
        if (agenteEmergencia.estaConfiguradoTelegram()) {
            tvEstadoTelegram.text = "✅ Telegram configurado"
            tvEstadoTelegram.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    private fun guardarConfiguracion() {
        val nombrePaciente = etNombrePaciente.text.toString().trim()
        val nombreContacto = etNombreContacto.text.toString().trim()
        val telefonoContacto = etTelefonoContacto.text.toString().trim()
        val botToken = etBotToken.text.toString().trim()
        val chatId = etChatId.text.toString().trim()

        if (nombrePaciente.isEmpty()) {
            mostrarError("Por favor ingrese el nombre del paciente")
            return
        }

        if (nombreContacto.isEmpty()) {
            mostrarError("Por favor ingrese el nombre del contacto")
            return
        }

        if (telefonoContacto.isEmpty()) {
            mostrarError("Por favor ingrese el teléfono del contacto")
            return
        }

        if (!validarTelefono(telefonoContacto)) {
            mostrarError("Por favor ingrese un teléfono válido")
            return
        }

        // Guardar configuración básica
        dbHelper.guardarNombrePaciente(nombrePaciente)
        dbHelper.guardarContactoEmergencia(nombreContacto, telefonoContacto)

        // Guardar Telegram si tiene datos
        if (botToken.isNotEmpty() && chatId.isNotEmpty()) {
            agenteEmergencia.configurarTelegram(botToken, chatId)
        }

        // Iniciar servicio de monitoreo
        iniciarServicioMonitoreo()

        Toast.makeText(this, "✅ Configuración guardada correctamente", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun probarConexionTelegram() {
        val botToken = etBotToken.text.toString().trim()
        val chatId = etChatId.text.toString().trim()

        if (botToken.isEmpty() || chatId.isEmpty()) {
            mostrarError("Complete el Bot Token y Chat ID")
            return
        }

        tvEstadoTelegram.text = "🔄 Probando conexión..."
        tvEstadoTelegram.setTextColor(Color.parseColor("#2196F3"))
        btnProbarTelegram.isEnabled = false

        agenteEmergencia.configurarTelegram(botToken, chatId)

        agenteEmergencia.probarConexionTelegram { exito, mensaje ->
            runOnUiThread {
                btnProbarTelegram.isEnabled = true
                if (exito) {
                    tvEstadoTelegram.text = "✅ ¡Conexión exitosa! Revisa tu Telegram"
                    tvEstadoTelegram.setTextColor(Color.parseColor("#4CAF50"))
                    Toast.makeText(this, "✅ Mensaje de prueba enviado", Toast.LENGTH_SHORT).show()
                } else {
                    tvEstadoTelegram.text = "❌ Error: $mensaje"
                    tvEstadoTelegram.setTextColor(Color.parseColor("#F44336"))
                }
            }
        }
    }

    private fun iniciarServicioMonitoreo() {
        try {
            val intent = Intent(this, ServicioMonitorMedicamentosActualizado::class.java)
            startService(intent)
        } catch (e: Exception) {
            // Silenciar error si el servicio ya está corriendo
        }
    }

    private fun probarConfiguracion() {
        val nombreContacto = etNombreContacto.text.toString().trim()
        val telefonoContacto = etTelefonoContacto.text.toString().trim()

        if (nombreContacto.isEmpty() || telefonoContacto.isEmpty()) {
            mostrarError("Complete los datos del contacto para probar")
            return
        }

        if (!validarTelefono(telefonoContacto)) {
            mostrarError("Ingrese un teléfono válido para probar")
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("🧪 Probar Configuración")
        builder.setMessage("¿Qué desea probar?\n\n📱 Mensaje: Enviará un SMS de prueba\n📞 Llamada: Iniciará una llamada de prueba")

        builder.setPositiveButton("📱 Mensaje") { _, _ ->
            probarMensaje(nombreContacto, telefonoContacto)
        }

        builder.setNegativeButton("📞 Llamada") { _, _ ->
            probarLlamada(telefonoContacto)
        }

        builder.setNeutralButton("Cancelar", null)
        builder.show()
    }

    private fun probarMensaje(nombreContacto: String, telefono: String) {
        try {
            val mensaje = "🧪 PRUEBA de MediCare: Esta es una prueba del sistema de alertas médicas."
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$telefono")
                putExtra("sms_body", mensaje)
            }
            startActivity(intent)
        } catch (e: Exception) {
            mostrarError("Error al abrir mensajes: ${e.message}")
        }
    }

    private fun probarLlamada(telefono: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$telefono")
            }
            startActivity(intent)
        } catch (e: Exception) {
            mostrarError("Error al abrir marcador: ${e.message}")
        }
    }

    private fun validarTelefono(telefono: String): Boolean {
        val telefonoLimpio = telefono.replace(Regex("[^\\d+]"), "")
        return telefonoLimpio.length >= 9 && (telefonoLimpio.startsWith("+") || telefonoLimpio.matches(Regex("\\d+")))
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, "❌ $mensaje", Toast.LENGTH_LONG).show()
    }
}