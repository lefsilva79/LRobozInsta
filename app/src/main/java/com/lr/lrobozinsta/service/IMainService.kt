// Date (UTC): 2025-01-02 04:09:36
// Author: lefsilva79

package com.lr.lrobozinsta.service

import android.os.IInterface

interface IMainService {
    fun start()
    fun stop()
    fun setTargetValue(value: String)
    fun isRunning(): Boolean
    fun isStillSearching(): Boolean
    fun isInTargetApp(): Boolean
}