package com.lr.lrobozinsta.service

import android.graphics.Rect
import android.util.Log
import com.lr.lrobozinsta.data.Store
import com.lr.lrobozinsta.data.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

class SearchService {
    companion object {
        private const val TAG = "SearchService"
    }

    fun getCurrentPackageName(): String? {
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

    suspend fun searchForValues(targetValue: String, searching: AtomicBoolean): List<Pair<String, Rect>> {
        if (!searching.get()) {
            Log.d(TAG, "Busca já finalizada anteriormente, ignorando chamada")
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "\n----------------------------------------")
                Log.d(TAG, "===== INICIANDO BUSCA DE VALORES =====")
                Log.d(TAG, "Valor alvo definido: $targetValue")

                if (targetValue.isEmpty()) {
                    Log.e(TAG, "ERRO: Valor alvo não definido!")
                    return@withContext emptyList()
                }

                val targetValueInt = targetValue.toIntOrNull() ?: run {
                    Log.e(TAG, "ERRO: Valor alvo não é um número válido!")
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

                            // Nova lógica para extrair e comparar números
                            val numberRegex = "\\$(\\d+(?:\\.\\d+)?)".toRegex()
                            val numeroEncontrado = numberRegex.find(textoCompleto)?.groupValues?.get(1)

                            if (numeroEncontrado != null) {
                                // Converte o número encontrado para inteiro, removendo decimais
                                val valorEncontradoInt = numeroEncontrado.split(".")[0].toIntOrNull()

                                if (valorEncontradoInt != null && valorEncontradoInt >= targetValueInt) {
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

                                        Log.d(TAG, ">>> Valor maior ou igual encontrado: $textoCompleto (valor numérico: $valorEncontradoInt >= $targetValueInt)")
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

    fun executeRootCommand(command: String): String {
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
}