// Date (UTC): 2025-01-01 20:25:17
// Author: lefsilva79

package com.lr.lrobozinsta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import com.lr.lrobozinsta.service.MainService
import com.lr.lrobozinsta.service.MainServiceConnection
import com.lr.lrobozinsta.utils.RootManager
import com.lr.lrobozinsta.ui.MainScreen

class MainActivity : ComponentActivity() {
    private val serviceConnection = MainServiceConnection()
    private var hasRoot = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        checkRoot()

        setContent {
            MaterialTheme {
                MainScreen(
                    hasRoot = hasRoot,
                    serviceConnection = serviceConnection
                )
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

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 123
    }
}