// app/src/main/java/com/lr/lrobozinsta/MainActivity.kt
package com.lr.lrobozinsta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.lr.lrobozinsta.service.MainService
import com.lr.lrobozinsta.service.MainServiceConnection
import com.lr.lrobozinsta.data.Store
import com.lr.lrobozinsta.data.Item
import com.lr.lrobozinsta.utils.RootManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AlertDialog

class MainActivity : ComponentActivity() {
    private val serviceConnection = MainServiceConnection()
    private var hasRoot = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        checkRoot()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                notGranted.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun checkRoot() {
        hasRoot = RootManager.isRooted()
        if (!hasRoot) {
            showRootAlert()
        }
    }

    private fun showRootAlert() {
        AlertDialog.Builder(this)
            .setTitle("Root Required")
            .setMessage("This app requires root access to function properly")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (!hasRoot) {
                    finish()
                }
            }
            .setCancelable(false)
            .show()
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    private fun bindService() {
        val intent = Intent(this, MainService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    @Composable
    private fun MainContent() {
        val items by Store.get().items.collectAsState()
        var isRunning by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    serviceConnection.service?.let { service ->
                        if (isRunning) {
                            service.stop()
                        } else {
                            service.start()
                        }
                        isRunning = !isRunning
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasRoot
            ) {
                Text(if (isRunning) "Stop" else "Start")
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

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 123
    }
}