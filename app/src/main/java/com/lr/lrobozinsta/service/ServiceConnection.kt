// app/src/main/java/com/lr/lrobozinsta/service/ServiceConnection.kt
package com.lr.lrobozinsta.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

class MainServiceConnection : ServiceConnection {
    var service: IMainService? = null
    private var onConnected: (() -> Unit)? = null

    fun setOnConnectedListener(listener: () -> Unit) {
        onConnected = listener
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        service = (binder as? MainService.LocalBinder)?.service
        onConnected?.invoke()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }
}