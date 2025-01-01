// Em app/src/main/java/com/lr/lrobozinsta/service/IMainService.kt
package com.lr.lrobozinsta.service

interface IMainService {
    fun start()
    fun stop()
    fun isRunning(): Boolean
}