package com.ejemplo.monitorcarga

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var txtEstado: TextView
    private lateinit var txtAmperaje: TextView
    private lateinit var txtVoltaje: TextView
    private lateinit var txtPotencia: TextView
    private lateinit var txtNivel: TextView
    private lateinit var txtTemperatura: TextView
    private lateinit var txtAlerta: TextView

    private lateinit var batteryManager: BatteryManager
    private val handler = Handler(Looper.getMainLooper())

    private val actualizador = object : Runnable {
        override fun run() {
            actualizarDatos()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtEstado = findViewById(R.id.txtEstado)
        txtAmperaje = findViewById(R.id.txtAmperaje)
        txtVoltaje = findViewById(R.id.txtVoltaje)
        txtPotencia = findViewById(R.id.txtPotencia)
        txtNivel = findViewById(R.id.txtNivel)
        txtTemperatura = findViewById(R.id.txtTemperatura)
        txtAlerta = findViewById(R.id.txtAlerta)

        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handler.post(actualizador)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        handler.removeCallbacks(actualizador)
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            actualizarDatos()
        }
    }

    private fun actualizarDatos() {
        val currentNowUa = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        )
        val soportaCorriente = currentNowUa != Int.MIN_VALUE

        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltajeMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltajeV = voltajeMv / 1000.0
        val nivel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val escala = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val nivelPct = (nivel * 100) / escala
        val temperaturaTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatura = temperaturaTenths / 10.0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val cargando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (cargando) {
            txtEstado.text = "Cargando"
            txtEstado.setTextColor(0xFF4CAF50.toInt())
        } else {
            txtEstado.text = "No está cargando"
            txtEstado.setTextColor(0xFFF44336.toInt())
        }

        if (soportaCorriente) {
            val currentMa = Math.abs(currentNowUa) / 1000.0
            txtAmperaje.text = String.format("%.0f mA", currentMa)

            val potenciaW = (currentMa / 1000.0) * voltajeV
            txtPotencia.text = String.format("Potencia: %.2f W", potenciaW)

            if (cargando && currentMa < 300) {
                txtAlerta.text = "Carga lenta detectada. Revisa el cable o cargador."
                txtAlerta.visibility = TextView.VISIBLE
            } else if (cargando && temperatura > 42) {
                txtAlerta.text = "Batería caliente (${temperatura}°C). Desconecta."
                txtAlerta.visibility = TextView.VISIBLE
            } else {
                txtAlerta.visibility = TextView.GONE
            }
        } else {
            txtAmperaje.text = "No soportado"
            txtPotencia.text = "Potencia: --"
            txtAlerta.text = "Este móvil no expone el amperaje real al sistema."
            txtAlerta.visibility = TextView.VISIBLE
        }

        txtVoltaje.text = "Voltaje: ${voltajeMv} mV"
        txtNivel.text = "Batería: $nivelPct %"
        txtTemperatura.text = "Temperatura: $temperatura °C"
    }
}
