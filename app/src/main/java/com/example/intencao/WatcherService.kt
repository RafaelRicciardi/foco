package com.example.intencao

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*

/**
 * - Tela bloqueada: encerra a sessão (e registra o tempo real).
 * - Tela desbloqueada: traz a tela de perguntas para frente.
 * - Fim do tempo: notificações insistentes até a tela ser bloqueada.
 */
class WatcherService : Service() {

    companion object {
        const val ACTION_START_TIMER = "com.example.intencao.START_TIMER"
        const val ACTION_CANCEL = "com.example.intencao.CANCEL"
        private const val CH_WATCH = "watch"
        private const val CH_NAG = "nag"
        private const val NAG_BASE_ID = 1000
        private const val NAG_INTERVAL_MS = 20_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var nagCount = 0
    private var scheduled = false
    private var appName = ""
    private val msgs = listOf(
        "Seu tempo acabou. Desligue o celular.",
        "Você disse que seria só isso. Bloqueie a tela.",
        "Ainda aqui? Guarde o celular.",
        "Respire. Coloque o celular de lado.",
        "O mundo lá fora está esperando.",
    )

    private val nagRunnable = object : Runnable {
        override fun run() {
            val open = PendingIntent.getActivity(this@WatcherService, 0,
                Intent(this@WatcherService, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val n = Notification.Builder(this@WatcherService, CH_NAG)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Hora de parar${if (appName.isNotEmpty()) " · $appName" else ""}")
                .setContentText(msgs[nagCount % msgs.size])
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            getSystemService(NotificationManager::class.java).notify(NAG_BASE_ID + nagCount, n)
            nagCount++
            handler.postDelayed(this, NAG_INTERVAL_MS)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> { Session.end(c); cancelSession() }
                Intent.ACTION_USER_PRESENT -> startActivity(Intent(c, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_WATCH, "Guardião", NotificationManager.IMPORTANCE_MIN))
        nm.createNotificationChannel(NotificationChannel(CH_NAG, "Hora de parar", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true); vibrationPattern = longArrayOf(0, 400, 200, 400)
        })
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT); addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_EXPORTED)
        else registerReceiver(receiver, f)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = Notification.Builder(this, CH_WATCH)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Intenção ativa").build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)

        when (intent?.action) {
            ACTION_CANCEL -> cancelSession()
            ACTION_START_TIMER -> { cancelSession(); scheduleFromSession() }
            else -> if (!scheduled) scheduleFromSession() // reinício do serviço
        }
        return START_STICKY
    }

    private fun scheduleFromSession() {
        val s = Session.current(this) ?: return
        appName = s.app
        scheduled = true
        handler.postDelayed(nagRunnable, (s.endsAt - System.currentTimeMillis()).coerceAtLeast(0))
    }

    private fun cancelSession() {
        handler.removeCallbacksAndMessages(null)
        scheduled = false
        val nm = getSystemService(NotificationManager::class.java)
        for (i in 0 until nagCount) nm.cancel(NAG_BASE_ID + i)
        nagCount = 0
    }

    override fun onDestroy() { unregisterReceiver(receiver); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
