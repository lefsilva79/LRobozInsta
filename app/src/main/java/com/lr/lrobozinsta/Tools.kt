package com.lr.lrobozinsta.utils

object Tools {
    fun hasRoot(): Boolean {
        return try {
            Runtime.getRuntime().exec("su")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun run(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            ""
        }
    }
}