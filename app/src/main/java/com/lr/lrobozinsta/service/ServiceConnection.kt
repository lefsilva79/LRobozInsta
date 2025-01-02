package com.lr.lrobozinsta.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

class MainServiceConnection : ServiceConnection {
    var service: IMainService? = null
    var isBound = false
        private set

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Log.d("ServiceConnection", "Service connected")
        service = (binder as? MainService.LocalBinder)?.service
        isBound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d("ServiceConnection", "Service disconnected")
        isBound = false
        // NÃO limpar o service aqui
        // service = null
    }
}