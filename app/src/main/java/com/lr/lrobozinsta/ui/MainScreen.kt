// Date (UTC): 2025-01-02 03:49:58
// Author: lefsilva79

package com.lr.lrobozinsta.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.lr.lrobozinsta.service.MainServiceConnection
import com.lr.lrobozinsta.data.Store
import com.lr.lrobozinsta.data.Item
import com.lr.lrobozinsta.utils.NotificationHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    hasRoot: Boolean,
    serviceConnection: MainServiceConnection,
    context: Context = LocalContext.current
) {
    var numberValue by remember { mutableStateOf("") }
    val items by Store.get().items.collectAsState()
    var isServiceRunning by remember { mutableStateOf(false) }
    var isInCorrectApp by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val notificationHelper = remember { NotificationHelper(context) }

    LaunchedEffect(serviceConnection.service) {
        while (true) {
            val stillSearching = serviceConnection.service?.isStillSearching() ?: false
            Log.d("SearchState", """
                ===== DEBUG SEARCH STATE =====
                Still Searching: $stillSearching
                Is Service Running: ${serviceConnection.service?.isRunning()}
                Current isSearching: $isSearching
                Current isServiceRunning: $isServiceRunning
                =============================
            """.trimIndent())

            isSearching = stillSearching
            if (!stillSearching) {
                isServiceRunning = false
                Log.d("SearchState", "Not searching anymore, setting isServiceRunning to false")
            }
            delay(200)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            serviceConnection.service?.let { service ->
                val running = service.isRunning()
                if (isServiceRunning != running) {
                    Log.d("ButtonState", "=== MUDANÇA DE ESTADO DOS BOTÕES ===")
                    Log.d("ButtonState", "Service running: $isServiceRunning -> $running")
                    isServiceRunning = running
                }
                isInCorrectApp = service.isInTargetApp()
            }
            delay(500)
        }
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$",
                    style = TextStyle(
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
                OutlinedTextField(
                    value = numberValue,
                    onValueChange = { newValue ->
                        if (!isSearching) {
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                numberValue = newValue
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 24.sp),
                    enabled = !isSearching
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        serviceConnection.service?.let { service ->
                            Log.d("ButtonState", "Start button clicked")
                            service.setTargetValue(numberValue)
                            service.start()
                            isServiceRunning = true
                            isSearching = true
                            Log.d("ButtonState", "Service started, isSearching: $isSearching")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasRoot && numberValue.isNotEmpty() && !isSearching
                ) {
                    Text(
                        text = "Start",
                        style = TextStyle(fontSize = 18.sp)
                    )
                }

                Button(
                    onClick = {
                        serviceConnection.service?.let { service ->
                            Log.d("ButtonState", "Stop button clicked")
                            service.stop()
                            isServiceRunning = false
                            isSearching = false
                            Log.d("ButtonState", "Service stopped, isSearching: $isSearching")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isServiceRunning && isSearching,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Stop",
                        style = TextStyle(fontSize = 18.sp)
                    )
                }
            }

            if (isServiceRunning) {
                Text(
                    text = if (!isInCorrectApp)
                        "Aguardando abertura do app TestValues..."
                    else "Detectando: $ $numberValue",
                    style = TextStyle(
                        fontSize = 18.sp,
                        color = if (!isInCorrectApp)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items) { item ->
                    ItemCard(item)
                }
            }
        }
    }
}

@Composable
private fun ItemCard(item: Item) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = item.txt,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = formatTimestamp(item.time),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}