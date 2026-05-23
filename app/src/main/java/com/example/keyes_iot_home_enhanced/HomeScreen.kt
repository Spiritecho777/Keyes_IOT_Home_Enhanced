package com.example.keyestudioiotenhanced
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import java.net.Socket
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

@Composable
fun SmartHomeScreen(vm: SensorViewModel = viewModel()) {

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var ip by remember { mutableStateOf("192.168.27.94") }

    val isConnected by vm.isConnected.collectAsState()
    val sensors by vm.sensors.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (isConnected) vm.disconnect() else vm.connect(ip)
                }) {
                    Text(if (isConnected) "DISCONNECT" else "CONNECT")
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 8.dp)
                        .background(
                            if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("My Smart Home", style = MaterialTheme.typography.titleLarge)

            val devices = listOf(
                Triple("LED",     "a", "A"),
                Triple("Window",  "b", "B"),
                Triple("Music",   "c", "C"),
                Triple("Whistle", "d", "D"),
                Triple("Door",    "e", "E"),
                Triple("Fan",     "f", "F"),
            )

            LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                items(devices) { (label, cmdOn, cmdOff) ->
                    var on by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = label,
                            tint = if (on) Color(0xFFFFC107) else Color.Gray
                        )
                        Text(label)
                        Switch(
                            checked = on,
                            enabled = isConnected,
                            onCheckedChange = { checked ->
                                on = checked
                                vm.sendCommand(if (checked) cmdOn else cmdOff)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Sensors", style = MaterialTheme.typography.titleLarge)

            Column {
                SensorRow("Raindrop",    sensors.rainwater)
                SensorRow("Harmful gas", sensors.gas)
                SensorRow("Presence",    sensors.presence)
                SensorRow("Temperature", sensors.temperature)
                SensorRow("Humidity",    sensors.humidity)
            }
        }
    }
}

suspend fun testConnection(ip: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 80), 3000) // timeout 3s
            val out = socket.getOutputStream()
            // Envoyer une commande valide terminée par 's' (le délimiteur de l'ESP32)
            // 'x' n'est pas une commande connue mais elle sera lue et ignorée
            out.write("xs".toByteArray()) // 's' = terminateur de readStringUntil
            out.flush()
            Thread.sleep(200)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
fun SensorRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

suspend fun sendCommand(ip: String, cmd: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 80), 3000)
            val out = socket.getOutputStream()
            // Toujours terminer par 's' pour que readStringUntil('s') se débloque
            out.write("${cmd}s".toByteArray())
            out.flush()
            Thread.sleep(200)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
