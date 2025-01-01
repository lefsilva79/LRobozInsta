// Date (UTC): 2024-01-01 18:45:28
// Author: lefsilva79

package com.lr.lrobozinsta.ui

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Current Date and Time (UTC): 2025-01-01 19:00:50
// Current User's Login: lefsilva79

@Composable
fun MainScreen(
    hasRoot: Boolean,
    serviceConnection: MainServiceConnection,
    context: Context = LocalContext.current
) {
    var numberValue by remember { mutableStateOf("") }
    val items by Store.get().items.collectAsState()
    var isRunning by remember { mutableStateOf(false) }
    val notificationHelper = remember { NotificationHelper(context) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Input de números com símbolo "$"
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
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            numberValue = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 24.sp)
                )
            }

            Button(
                onClick = {
                    serviceConnection.service?.let { service ->
                        if (isRunning) {
                            service.stop()
                        } else {
                            service.start()
                            if (numberValue.isNotEmpty()) {
                                notificationHelper.showValueDetectedNotification(numberValue)
                            }
                        }
                        isRunning = !isRunning
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasRoot && numberValue.isNotEmpty()
            ) {
                Text(
                    text = if (isRunning) "Stop" else "Start",
                    style = TextStyle(fontSize = 18.sp)
                )
            }

            // Status de detecção
            if (isRunning && numberValue.isNotEmpty()) {
                Text(
                    text = "Detectando: $ $numberValue",
                    style = TextStyle(
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
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