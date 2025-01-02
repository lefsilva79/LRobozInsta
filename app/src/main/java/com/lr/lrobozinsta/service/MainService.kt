package com.lr.lrobozinsta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.ActivityManager
import android.app.Notification
import android.content.Context
import android.graphics.Rect
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import com.lr.lrobozinsta.data.Store
import com.lr.lrobozinsta.data.Item
import com.lr.lrobozinsta.utils.RootManager
import java.io.DataOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class MainService : Service(), IMainService {
    private val active = AtomicBoolean(false)
    private val searching = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val binder = LocalBinder()
    private var targetValue: String? = null

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
    }

    private fun getCurrentPackageName(): String? {
        val output = executeRootCommand("dumpsys window | grep mCurrentFocus")
        return try {
            val regex = "\\{.*?\\s+(\\S+)/".toRegex()
            val matchResult = regex.find(output)
            val packageName = matchResult?.groupValues?.get(1)
            Log.d(TAG, "Extracted package name: $packageName from output: $output")
            packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting package name: ${e.message}")
            null
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
            while (isActive && searching.get()) {  // Removido active.get() da condição
                try {
                    val currentPackage = getCurrentPackageName()
                    Log.d(TAG, "App atual: $currentPackage vs Alvo: $TARGET_PACKAGE")

                    if (currentPackage == TARGET_PACKAGE) {
                        Log.d(TAG, "App alvo encontrado - buscando valores")
                        val values = searchForValues()

                        if (values.isNotEmpty()) {
                            Log.d(TAG, "Valores encontrados: ${values.size}")
                            val targetFound = values.any { it.first == targetValue }
                            if (targetFound) {
                                Log.d(TAG, ">>> VALOR ALVO ENCONTRADO!")
                                searching.set(false)
                                active.set(false)
                                executeRootCommand("pkill -f uiautomator")
                                stopForeground(true)
                                stopSelf()
                                break
                            }
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

    // Date (UTC): 2025-01-02 04:30:45
// Author: lefsilva79

    private suspend fun searchForValues(): List<Pair<String, Rect>> {
        if (!searching.get()) {
            Log.d(TAG, "Busca já finalizada anteriormente, ignorando chamada")
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "\n----------------------------------------")
                Log.d(TAG, "===== INICIANDO BUSCA DE VALORES =====")
                Log.d(TAG, "Valor alvo definido: $targetValue")

                if (targetValue.isNullOrEmpty()) {
                    Log.e(TAG, "ERRO: Valor alvo não definido!")
                    return@withContext emptyList()
                }

                val values = mutableListOf<Pair<String, Rect>>()
                var totalValoresAnalisados = 0

                Log.d(TAG, "Verificando disponibilidade do UiAutomator...")
                val uiautomatorPath = executeRootCommand("which uiautomator") ?: ""
                if (uiautomatorPath.isBlank()) {
                    Log.e(TAG, "UiAutomator não encontrado!")
                    return@withContext emptyList()
                }

                executeRootCommand("chmod 777 /sdcard")

                val dumpCommand = StringBuilder()
                    .append("uiautomator dump /sdcard/window_dump.xml")
                    .append(" && ")
                    .append("cat /sdcard/window_dump.xml")
                    .toString()

                Log.d(TAG, "Executando comando UiAutomator: $dumpCommand")
                val xmlOutput = executeRootCommand(dumpCommand) ?: ""

                if (xmlOutput.isBlank()) {
                    Log.e(TAG, "Nenhum output do UiAutomator!")
                    return@withContext emptyList()
                }

                val nodeRegex = "<node[^>]*text=\"\\$[^\"]*\"[^>]*bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"[^>]*/>".toRegex()
                val matches = nodeRegex.findAll(xmlOutput)

                matches.forEach { match ->
                    try {
                        val textRegex = "text=\"(\\$[^\"]+)\"".toRegex()
                        val textMatch = textRegex.find(match.value)
                        val textoCompleto = textMatch?.groupValues?.get(1)

                        if (textoCompleto != null) {
                            totalValoresAnalisados++

                            val numberRegex = "\\$(\\d+)".toRegex()
                            val numeroEncontrado = numberRegex.find(textoCompleto)?.groupValues?.get(1)

                            if (numeroEncontrado == targetValue) {
                                val boundsRegex = "bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"[^>]*/>".toRegex()
                                val boundsMatch = boundsRegex.find(match.value)

                                if (boundsMatch != null) {
                                    val (left, top, right, bottom) = boundsMatch.destructured
                                    val bounds = Rect(
                                        left.toInt(),
                                        top.toInt(),
                                        right.toInt(),
                                        bottom.toInt()
                                    )

                                    val resultado = Pair(textoCompleto, bounds)
                                    values.add(resultado)

                                    Log.d(TAG, ">>> Valor alvo encontrado: $textoCompleto (parte inteira: $numeroEncontrado)")
                                    Log.d(TAG, ">>> Valor adicionado à lista com bounds: $bounds")

                                    Store.get().add(Item(
                                        txt = textoCompleto,
                                        time = System.currentTimeMillis(),
                                        area = bounds
                                    ))
                                    Log.d(TAG, ">>> Valor adicionado ao Store")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar nó: ${e.message}")
                    }
                }

                executeRootCommand("rm /sdcard/window_dump.xml")

                Log.d(TAG, "\n----------------------------------------")
                Log.d(TAG, "===== FINALIZANDO BUSCA DE VALORES =====")
                Log.d(TAG, "Total de valores analisados: $totalValoresAnalisados")
                Log.d(TAG, "Total de valores encontrados: ${values.size}")
                Log.d(TAG, "----------------------------------------\n")

                values
            } catch (e: Exception) {
                Log.e(TAG, "===== ERRO NA BUSCA DE VALORES =====")
                Log.e(TAG, "Erro: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTrace.joinToString("\n")}")
                emptyList()
            }
        }
    }

    override fun stop() {
        Log.d(TAG, "===== PARANDO SERVIÇO MANUALMENTE =====")
        if (searching.get()) {
            Log.d(TAG, "Parando busca em andamento")
            searching.set(false)  // Marca que não está mais procurando
            active.set(false)     // Sinaliza para parar o loop
            executeRootCommand("pkill -f uiautomator")  // Mata processo do uiautomator
            stopForeground(true)  // Remove notificação
            stopSelf()           // Para o serviço
        } else {
            Log.d(TAG, "Serviço já estava parado")
        }
    }

    private fun executeRootCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)

            Log.d(TAG, "Executing root command: $command")
            outputStream.writeBytes("$command\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val result = output.toString().trim()
            Log.d(TAG, "Root command output: $result")

            process.waitFor()
            process.destroy()

            result
        } catch (e: Exception) {
            Log.e(TAG, "Root command error: ${e.message}")
            ""
        }
    }

    override fun isInTargetApp(): Boolean {
        val currentPackage = getCurrentPackageName()
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