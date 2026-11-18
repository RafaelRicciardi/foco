package com.example.intencao

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class SetupActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.parseColor("#F2F2F2"))
        }
        fun text(t: String, size: Float) = TextView(this).apply {
            text = t; textSize = size; setTextColor(Color.parseColor("#222222")); setPadding(0, 0, 0, pad / 2)
        }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }

        root.addView(text("Intenção · Configurações", 24f))
        root.addView(text("Faça os passos na ordem. O passo 4 transforma o app na tela inicial do celular " +
                "(sem ícones). Para voltar ao normal, escolha outra tela inicial no mesmo menu.", 15f))

        root.addView(btn("1. Permitir notificações") {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        root.addView(btn("2. Permitir exibir sobre outros apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(btn("3. Desativar otimização de bateria") {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        })
        root.addView(btn("4. Definir como tela inicial") {
            startForegroundService(Intent(this, WatcherService::class.java))
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        })
        root.addView(btn("Ver estatísticas") { startActivity(Intent(this, StatsActivity::class.java)) })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
