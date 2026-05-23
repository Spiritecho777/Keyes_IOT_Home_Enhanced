package com.example.keyestudioiotenhanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

data class SensorData(
    val rainwater: String = "--",
    val gas: String = "--",
    val presence: String = "--",
    val temperature: String = "--",
    val humidity: String = "--"
)

class SensorViewModel : ViewModel() {

    private val _sensors = MutableStateFlow(SensorData())
    val sensors: StateFlow<SensorData> = _sensors

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var socket: Socket? = null
    private var readJob: Job? = null

    fun connect(ip: String) {
        readJob?.cancel()
        readJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                socket = Socket()
                socket!!.connect(InetSocketAddress(ip, 80), 3000)
                socket!!.soTimeout = 10000 // 10s timeout, l'ESP envoie toutes les 1s
                _isConnected.value = true

                // Envoyer un premier message pour débloquer readStringUntil sur l'ESP32
                socket!!.getOutputStream().write("xs".toByteArray())
                socket!!.getOutputStream().flush()

                val inputStream = socket!!.getInputStream()
                val fields = mutableListOf<String>()
                val current = StringBuilder()

                while (true) {
                    val b = try {
                        inputStream.read()
                    } catch (e: java.net.SocketTimeoutException) {
                        continue
                    }
                    if (b == -1) break

                    val c = b.toChar()

                    if (c.isDigit()) {
                        current.append(c)
                    } else if (c == ',') {
                        fields.add(current.toString())
                        current.clear()

                        if (fields.size == 4) {
                            // On lit le 5ème champ : max 2 chiffres pour humidité (0-99)
                            val hum = StringBuilder()
                            var next = inputStream.read()
                            if (next != -1 && next.toChar().isDigit()) {
                                hum.append(next.toChar())
                                // Lire le 2ème chiffre seulement si hum < 10 jusque là
                                // ou si c'est encore un chiffre ET que ça reste <= 99
                                val peek = inputStream.read()
                                if (peek != -1 && peek.toChar().isDigit()) {
                                    val twoDigit = (hum.toString() + peek.toChar()).toInt()
                                    if (twoDigit <= 99) {
                                        hum.append(peek.toChar())
                                    }
                                    // sinon on ignore peek (c'est le début de la trame suivante)
                                }
                            }
                            fields.add(hum.toString())

                            val raw = fields.joinToString(",")
                            android.util.Log.d("ESP32", "RAW: '$raw'")
                            parseSensorData(raw)
                            fields.clear()
                            current.clear()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
                _isConnected.value = false
                _sensors.value = SensorData()
            }
        }
    }

    fun disconnect() {
        readJob?.cancel()
        socket?.close()
        socket = null
        _isConnected.value = false
        _sensors.value = SensorData()
    }

    fun sendCommand(cmd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                socket?.getOutputStream()?.let {
                    it.write("${cmd}s".toByteArray())
                    it.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Format reçu : "Rainwater,gas,pir,temperature,humidity"
    // Exemple     : "4095,0,1,24,60"
    private fun parseSensorData(raw: String): Boolean {
        val parts = raw.trim().split(",")
        if (parts.size != 5) return false

        val rainRaw = parts[0].toIntOrNull() ?: return false
        val gasVal  = parts[1].toIntOrNull() ?: return false
        val pirVal  = parts[2].toIntOrNull() ?: return false
        val temp    = parts[3].toIntOrNull() ?: return false
        val hum     = parts[4].toIntOrNull() ?: return false

        if (rainRaw !in 0..4095) return false
        if (gasVal !in 0..1) return false
        if (pirVal !in 0..1) return false
        if (temp !in 0..50) return false
        if (hum !in 0..99) return false

        val rainPercent = ((rainRaw / 4095.0) * 100).toInt()  // supprime le 100 -

        _sensors.value = SensorData(
            rainwater   = "$rainPercent%",
            gas         = if (gasVal == 0) "OK" else "⚠ Détecté",  // inversé
            presence    = if (pirVal == 0) "✔ Présence" else "Aucune",  // inversé
            temperature = "${temp}°C",
            humidity    = "${hum}%"
        )
        return true
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}