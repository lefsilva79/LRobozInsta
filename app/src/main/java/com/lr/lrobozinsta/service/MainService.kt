package com.lr.lrobozinsta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.Notification
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import com.lr.lrobozinsta.data.Store

class MainService : Service(), IMainService {
    private val active = AtomicBoolean(false)
    private val searching = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val binder = LocalBinder()
    private var targetValue: String? = null
    private lateinit var searchService: SearchService

    companion object {
        private const val CHANNEL_ID = "ServiceChannel"
        private const val TARGET_PACKAGE = "com.lr.testvalues"
        private const val PACKAGE_CHECK_DELAY = 2000L
        private val TAG = "ServiceState"
    }

    inner class LocalBinder : Binder() {
        val service: IMainService
            get() = this@MainService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        searchService = SearchService()
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

    override fun setTargetValue(value: String) {
        targetValue = value
        searching.set(true)
        Log.d(TAG, "Novo valor alvo definido: $value - Busca reativada")
    }

    override fun start() {
        if (active.getAndSet(true)) return

        Log.d(TAG, "===== INICIANDO SERVIÇO =====")
        Log.d(TAG, "Target Package: $TARGET_PACKAGE")
        Log.d(TAG, "Target Value: $targetValue")

        searching.set(true)
        Store.get().clear()

        if (targetValue.isNullOrEmpty()) {
            Log.e(TAG, "ERRO: Valor alvo não definido!")
            return
        }

        startForeground(1, createNotification())

        scope.launch {
            while (isActive && searching.get()) {
                try {
                    val currentPackage = searchService.getCurrentPackageName()
                    Log.d(TAG, "App atual: $currentPackage vs Alvo: $TARGET_PACKAGE")

                    if (currentPackage == TARGET_PACKAGE) {
                        Log.d(TAG, "App alvo encontrado - buscando valores")
                        val values = searchService.searchForValues(targetValue!!, searching)

                        if (values.isNotEmpty()) {
                            Log.d(TAG, "Valores encontrados: ${values.size}")
                            // Não precisamos mais verificar se é exatamente igual
                            // Se há valores na lista, significa que encontramos números >= ao alvo
                            Log.d(TAG, ">>> VALOR ALVO OU MAIOR ENCONTRADO!")
                            searching.set(false)
                            active.set(false)
                            searchService.executeRootCommand("pkill -f uiautomator")
                            stopForeground(true)
                            stopSelf()
                            break
                        }

                        Log.d(TAG, "Valor alvo não encontrado - continuando")
                    } else {
                        Log.d(TAG, "App diferente - continuando busca")
                    }

                    delay(PACKAGE_CHECK_DELAY)
                } catch (e: Exception) {
                    Log.e(TAG, "ERRO NO LOOP: ${e.message}")
                    delay(PACKAGE_CHECK_DELAY)
                }
            }
            Log.d(TAG, "===== SERVIÇO FINALIZADO =====")
            Log.d(TAG, "Motivo: searching=${searching.get()}, isActive=$isActive")
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service Running")
            .setContentText("Procurando por: $$targetValue")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun stop() {
        Log.d(TAG, "===== PARANDO SERVIÇO MANUALMENTE =====")
        if (searching.get()) {
            Log.d(TAG, "Parando busca em andamento")
            searching.set(false)
            active.set(false)
            searchService.executeRootCommand("pkill -f uiautomator")
            stopForeground(true)
            stopSelf()
        } else {
            Log.d(TAG, "Serviço já estava parado")
        }
    }

    override fun isInTargetApp(): Boolean {
        val currentPackage = searchService.getCurrentPackageName()
        Log.d(TAG, "Checking if in target app - Current: $currentPackage, Target: $TARGET_PACKAGE")
        return currentPackage == TARGET_PACKAGE
    }

    override fun isRunning(): Boolean {
        return active.get()
    }

    override fun isStillSearching(): Boolean {
        val isStillSearching = searching.get()
        Log.d(TAG, "isStillSearching() chamado - retornando: $isStillSearching")
        return isStillSearching
    }

    override fun onBind(intent: Intent?): IBinder = binder
}