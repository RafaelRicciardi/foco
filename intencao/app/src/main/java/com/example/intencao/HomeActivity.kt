package com.example.intencao

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

/**
 * Tela inicial do celular. Não mostra ícones nem lista de apps:
 * só as três perguntas (ou o estado da sessão em andamento).
 */
class HomeActivity : Activity() {

    private val apps = HashMap<String, String>() // nome -> pacote
    private val handler = Handler(Looper.getMainLooper())
    private val ink = Color.parseColor("#222222")
    private val bg = Color.parseColor("#F2F2F2")

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        startForegroundService(Intent(this, WatcherService::class.java)) // garante o guardião ativo
        render()
    }

    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    private fun render() {
        handler.removeCallbacksAndMessages(null)
        val s = Session.current(this)
        val content = if (s == null) questions() else running(s)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(24), dp(72), dp(24), dp(24))
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer())
        }
        setContentView(page)
    }

    private fun label(t: String, size: Float = 18f, top: Int = 24) = TextView(this).apply {
        text = t; textSize = size; setTextColor(ink); setPadding(0, dp(top), 0, dp(6))
    }

    // ---------- As três perguntas ----------
    private fun questions(): View {
        apps.clear()
        val pm = packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .forEach {
                val pkg = it.activityInfo.packageName
                if (pkg != packageName) apps[it.loadLabel(pm).toString()] = pkg
            }

        val why = EditText(this).apply {
            hint = "Escreva o motivo"; minLines = 2
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        val app = AutoCompleteTextView(this).apply {
            hint = "Digite o nome do app"; threshold = 1; setSingleLine()
            setAdapter(ArrayAdapter(this@HomeActivity, android.R.layout.simple_dropdown_item_1line, apps.keys.sorted()))
        }
        val minutes = EditText(this).apply {
            hint = "Minutos (ex.: 10)"; inputType = InputType.TYPE_CLASS_NUMBER
        }
        val go = Button(this).apply {
            text = "Abrir"
            setOnClickListener {
                val reason = why.text.toString().trim()
                val name = app.text.toString().trim()
                val pkg = apps[name]
                val min = minutes.text.toString().toIntOrNull() ?: 0
                when {
                    reason.isEmpty() -> why.error = "Responda com sinceridade"
                    pkg == null -> app.error = "Escolha um app da lista"
                    min <= 0 -> minutes.error = "Defina um tempo"
                    else -> begin(Session(name, pkg, reason, System.currentTimeMillis(), min))
                }
            }
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("Por que você está usando o celular?", top = 0)); addView(why)
            addView(label("Qual aplicativo você vai usar?")); addView(app)
            addView(label("Por quanto tempo?")); addView(minutes)
            addView(Space(this@HomeActivity), LinearLayout.LayoutParams(1, dp(28)))
            addView(go)
        }
        return ScrollView(this).apply { addView(box) }
    }

    private fun begin(s: Session) {
        Session.start(this, s)
        startForegroundService(Intent(this, WatcherService::class.java).setAction(WatcherService.ACTION_START_TIMER))
        launch(s.pkg)
    }

    private fun launch(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

    // ---------- Sessão em andamento ----------
    private fun running(s: Session): View {
        val remaining = label("", 40f, 32)
        val tick = object : Runnable {
            override fun run() {
                val left = (s.endsAt - System.currentTimeMillis()) / 1000
                if (left > 0) {
                    remaining.text = "%02d:%02d".format(left / 60, left % 60)
                } else {
                    remaining.text = "Tempo esgotado.\nDesligue o celular."
                    remaining.textSize = 26f
                }
                handler.postDelayed(this, 1000)
            }
        }
        tick.run()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("Sessão em andamento", 24f, 0))
            addView(label("App: ${s.app}", 18f, 12))
            addView(label("Motivo: ${s.why}", 16f, 4))
            addView(remaining)
            addView(Space(this@HomeActivity), LinearLayout.LayoutParams(1, dp(24)))
            addView(Button(this@HomeActivity).apply { text = "Voltar ao app"; setOnClickListener { launch(s.pkg) } })
            addView(Button(this@HomeActivity).apply {
                text = "Encerrar sessão"
                setOnClickListener {
                    Session.end(this@HomeActivity)
                    startService(Intent(this@HomeActivity, WatcherService::class.java)
                        .setAction(WatcherService.ACTION_CANCEL))
                    render()
                }
            })
        }
    }

    private fun footer(): View {
        fun link(t: String, target: Class<*>) = TextView(this).apply {
            text = t; textSize = 14f; setTextColor(Color.GRAY); gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { startActivity(Intent(this@HomeActivity, target)) }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(link("Estatísticas", StatsActivity::class.java), LinearLayout.LayoutParams(0, -2, 1f))
            addView(link("Configurações", SetupActivity::class.java), LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}
}
