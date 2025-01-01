// app/src/androidTest/java/com/lr/lrobozinsta/service/MainService.kt
package com.lr.lrobozinsta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import com.lr.lrobozinsta.data.Store
import com.lr.lrobozinsta.data.Item
import com.lr.lrobozinsta.utils.RootManager

class MainService : Service(), IMainService {
    private lateinit var device: UiDevice
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val active = AtomicBoolean(false)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: IMainService
            get() = this@MainService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        init()
    }

    private fun init() {
        try {
            device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            if (!RootManager.isRooted()) {
                throw Exception("Root access is required")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun start() {
        if (active.getAndSet(true)) return

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service Running")
            .setContentText("Monitoring for values...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)

        scope.launch {
            while (isActive && active.get()) {
                try {
                    scan()
                    delay(100)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun scan() {
        withContext(Dispatchers.IO) {
            try {
                val items = device.findObjects(By.text("$"))
                items.forEach { item ->
                    val txt = item.text
                    if (txt.startsWith("$")) {
                        process(txt, item.visibleBounds)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun process(txt: String, area: android.graphics.Rect) {
        Store.get().add(Item(
            txt = txt,
            time = System.currentTimeMillis(),
            area = area
        ))
    }

    override fun stop() {
        active.set(false)
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun isRunning(): Boolean {
        return active.get()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        private const val CHANNEL_ID = "ServiceChannel"
    }
}