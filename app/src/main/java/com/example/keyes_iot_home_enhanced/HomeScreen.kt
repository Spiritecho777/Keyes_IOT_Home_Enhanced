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

@Composable
fun SmartHomeScreen() {

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isConnected = remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            var ip by remember { mutableStateOf("192.168.27.94") }

            Row(verticalAlignment = Alignment.CenterVertically) {

                TextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    modifier = Modifier.weight(1f)
                )

                Button(onClick = {
                    scope.launch {
                        val ok = testConnection(ip)
                        if (ok) {
                            isConnected.value = true
                            snackbarHostState.showSnackbar("Connecté à $ip")
                        } else {
                            isConnected.value = false
                            snackbarHostState.showSnackbar("Impossible de se connecter")
                        }
                    }
                }) {
                    Text("CONNECT")
                }

                // Indicateur visuel
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 8.dp)
                        .background(
                            if (isConnected.value) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("My Smart Home", style = MaterialTheme.typography.titleLarge)

            LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                items(listOf("LED", "Window", "Music", "Whistle", "Door", "Fan")) { device ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lightbulb, contentDescription = device)
                        Text(device)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Smart Lift", style = MaterialTheme.typography.titleLarge)

            Column {
                SensorRow("Raindrop")
                SensorRow("Harmful gas")
                SensorRow("Presence")
                SensorRow("Temperature")
                SensorRow("Humidity")
            }
        }
    }
}

suspend fun testConnection(ip: String): Boolean {
    return try {
        val socket = Socket(ip, 80)
        val out = socket.getOutputStream()
        out.write("x".toByteArray())
        out.flush()
        Thread.sleep(500) // laisse le temps à l’ESP32 de répondre
        socket.close()
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun SensorRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text("Value")
    }
}

suspend fun sendCommand(ip: String, cmd: String): Boolean {
    return try {
        val socket = Socket(ip, 80)
        val out = socket.getOutputStream()
        out.write(cmd.toByteArray())
        out.flush()
        socket.close()
        true
    } catch (e: Exception) {
        false
    }
}
