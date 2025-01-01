// Current Date and Time (UTC): 2025-01-01 19:46:15
// Current User's Login: lefsilva79

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
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val binder = LocalBinder()
    private var targetValue: String? = null
    private var isSearching = true

    companion object {
        private const val CHANNEL_ID = "ServiceChannel"
        private const val TARGET_PACKAGE = "com.lr.testvalues"
        private const val PACKAGE_CHECK_DELAY = 2000L
        private const val TAG = "MainService"
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

    // Date (UTC): 2025-01-01 22:11:05
// Author: lefsilva79

    override fun setTargetValue(value: String) {
        targetValue = value
        isSearching = true  // Reset da busca ao definir novo valor
        Log.d(TAG, "Novo valor alvo definido: $value - Busca reativada")
    }

    // Date (UTC): 2025-01-01 21:10:15
// Author: lefsilva79

    override fun start() {
        if (active.getAndSet(true)) return

        Log.d(TAG, "===== INICIANDO SERVIÇO =====")
        Log.d(TAG, "Target Package definido: $TARGET_PACKAGE")
        Log.d(TAG, "Target Value definido: $targetValue")

        // Limpa o Store antes de iniciar nova busca
        Store.get().clear()
        Log.d(TAG, "Store limpo para nova busca")

        // Verifica se tem valor alvo definido
        if (targetValue.isNullOrEmpty()) {
            Log.e(TAG, "ERRO: Valor alvo não definido!")
            return
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service Running")
            .setContentText("Procurando por: $$targetValue")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)

        scope.launch {
            while (isActive && active.get()) {
                try {
                    Log.d(TAG, "===== NOVO CICLO DE VERIFICAÇÃO =====")
                    val currentPackage = getCurrentPackageName()
                    Log.d(TAG, "App atual detectado: $currentPackage")
                    Log.d(TAG, "App alvo esperado: $TARGET_PACKAGE")
                    Log.d(TAG, "Comparação direta: ${currentPackage == TARGET_PACKAGE}")

                    if (currentPackage == TARGET_PACKAGE) {
                        Log.d(TAG, "===== APP ALVO ENCONTRADO =====")
                        delay(500)

                        val doubleCheck = getCurrentPackageName()
                        Log.d(TAG, "Verificação dupla - App atual: $doubleCheck")

                        if (doubleCheck == TARGET_PACKAGE) {
                            Log.d(TAG, "Iniciando busca por valores...")
                            val values = searchForValues()
                            Log.d(TAG, "Retorno da busca - quantidade de valores: ${values.size}")

                            if (values.isNotEmpty()) {
                                val finalCheck = getCurrentPackageName()
                                Log.d(TAG, "Verificação final antes de processar - App atual: $finalCheck")

                                if (finalCheck == TARGET_PACKAGE) {
                                    values.forEach { (text, bounds) ->
                                        Store.get().add(Item(
                                            txt = text,
                                            time = System.currentTimeMillis(),
                                            area = bounds
                                        ))
                                    }
                                } else {
                                    Log.d(TAG, "ERRO: App mudou durante processamento!")
                                }
                            }
                        } else {
                            Log.d(TAG, "ERRO: App mudou antes da busca!")
                        }
                    } else {
                        Log.d(TAG, "Aguardando app TestValues... (Atual: $currentPackage)")
                    }

                    delay(PACKAGE_CHECK_DELAY)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro no loop principal: ${e.message}")
                    e.printStackTrace()
                    delay(PACKAGE_CHECK_DELAY)
                }
            }
        }
    }

    // Date (UTC): 2025-01-01 21:59:41
    // Author: lefsilva79

    private suspend fun searchForValues(): List<Pair<String, Rect>> {
        if (!isSearching) {
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

                // Verifica UiAutomator
                Log.d(TAG, "Verificando disponibilidade do UiAutomator...")
                executeRootCommand("which uiautomator")?.let { uiautomatorPath ->
                    if (uiautomatorPath.isBlank()) {
                        Log.e(TAG, "UiAutomator não encontrado!")
                        return@withContext emptyList()
                    }
                }

                // Prepara dump
                executeRootCommand("chmod 777 /sdcard")

                val dumpCommand = StringBuilder()
                    .append("uiautomator dump /sdcard/window_dump.xml")
                    .append(" && ")
                    .append("cat /sdcard/window_dump.xml")
                    .toString()

                Log.d(TAG, "Executando comando UiAutomator: $dumpCommand")
                val output = executeRootCommand(dumpCommand)

                if (output.isNullOrBlank()) {
                    Log.e(TAG, "Nenhum output do UiAutomator!")
                    return@withContext emptyList()
                }

                val nodeRegex = "<node[^>]*text=\"\\$[^\"]*\"[^>]*bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"[^>]*/>".toRegex()
                val matches = nodeRegex.findAll(output)

                matches.firstNotNullOfOrNull { match ->
                    try {
                        val textRegex = "text=\"(\\$[^\"]+)\"".toRegex()
                        val textMatch = textRegex.find(match.value)
                        val textoCompleto = textMatch?.groupValues?.get(1)

                        if (textoCompleto != null) {
                            totalValoresAnalisados++

                            val numberRegex = "\\$(\\d+)".toRegex()
                            val numeroEncontrado = numberRegex.find(textoCompleto)?.groupValues?.get(1)

                            if (numeroEncontrado == targetValue) {
                                Log.d(TAG, "----------------------------------------")
                                Log.d(TAG, ">>> MATCH! Valor alvo encontrado: $textoCompleto (parte inteira: $numeroEncontrado)")

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

                                    Log.d(TAG, ">>> Valor adicionado à lista com bounds: $bounds")

                                    Store.get().add(Item(
                                        txt = textoCompleto,
                                        time = System.currentTimeMillis(),
                                        area = bounds
                                    ))
                                    Log.d(TAG, ">>> Valor adicionado ao Store")

                                    // Desativa a busca e encerra o processo
                                    isSearching = false
                                    executeRootCommand("pkill -f uiautomator")
                                    Log.d(TAG, ">>> Processo uiautomator encerrado após encontrar valor")

                                    return@withContext values
                                }
                            }
                        }
                        null
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar nó: ${e.message}")
                        null
                    }
                }

                // Limpa o arquivo
                executeRootCommand("rm /sdcard/window_dump.xml")

                Log.d(TAG, "\n----------------------------------------")
                Log.d(TAG, "===== FINALIZANDO BUSCA DE VALORES =====")
                Log.d(TAG, "Total de valores analisados: $totalValoresAnalisados")
                Log.d(TAG, "Valor encontrado: ${values.firstOrNull()?.first ?: "Nenhum"}")
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
        Log.d(TAG, "Service stopping...")
        active.set(false)
        targetValue = null  // Limpa o valor alvo
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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

    override fun onBind(intent: Intent?): IBinder = binder
}
