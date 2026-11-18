package com.example.intencao

import android.content.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) c.startForegroundService(Intent(c, WatcherService::class.java))
    }
}
