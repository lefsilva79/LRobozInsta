// app/src/main/java/com/lr/lrobozinsta/utils/RootManager.kt
package com.lr.lrobozinsta.utils

import java.io.BufferedReader
import java.io.InputStreamReader

object RootManager {
    fun isRooted(): Boolean {
        return try {
            Runtime.getRuntime().exec("su")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun executeAsRoot(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}