package com.example.intencao

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : Activity() {

    private lateinit var box: LinearLayout
    private val ink = Color.parseColor("#222222")
    private val accent = Color.parseColor("#3D6B8E")

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun tv(t: String, size: Float = 16f, color: Int = ink, top: Int = 0) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); setPadding(0, dp(top), 0, dp(4))
    }

    private fun fmt(sec: Int): String = when {
        sec >= 3600 -> "${sec / 3600}h ${(sec % 3600) / 60}min"
        sec >= 60 -> "${sec / 60}min"
        else -> "${sec}s"
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Hoje" to 1, "7 dias" to 7, "30 dias" to 30, "Tudo" to 0).forEach { (t, d) ->
            chips.addView(Button(this).apply { text = t; textSize = 12f; setOnClickListener { render(d) } },
                LinearLayout.LayoutParams(0, -2, 1f))
        }
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#F2F2F2"))
            addView(tv("Estatísticas", 26f)); addView(chips); addView(box)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        render(7)
    }

    private fun bar(fraction: Float): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; weightSum = 1f
        addView(View(this@StatsActivity).apply { setBackgroundColor(accent) },
            LinearLayout.LayoutParams(0, dp(10), fraction.coerceIn(0.02f, 1f)))
    }

    private fun render(days: Int) {
        box.removeAllViews()
        var from = 0L
        if (days > 0) {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.add(Calendar.DAY_OF_YEAR, -(days - 1))
            from = c.timeInMillis
        }
        val list = UseLog.all(this).filter { it.start >= from }
        if (list.isEmpty()) { box.addView(tv("Sem sessões neste período.", top = 24)); return }

        val total = list.sumOf { it.actualSec }
        val plannedSec = list.sumOf { it.planned * 60 }
        val onTime = list.count { it.actualSec <= it.planned * 60 + 60 }

        box.addView(tv("Resumo", 20f, top = 20))
        box.addView(tv("Sessões: ${list.size}"))
        box.addView(tv("Tempo total de uso: ${fmt(total)}"))
        box.addView(tv("Média por sessão: ${fmt(total / list.size)}"))
        box.addView(tv("Tempo planejado: ${fmt(plannedSec)}"))
        box.addView(tv("Encerradas dentro do tempo: $onTime de ${list.size} (${onTime * 100 / list.size}%)"))

        box.addView(tv("Tempo por app", 20f, top = 24))
        val byApp = list.groupBy { it.app }.map { (a, l) -> Triple(a, l.sumOf { it.actualSec }, l.size) }
            .sortedByDescending { it.second }
        val max = (byApp.firstOrNull()?.second ?: 1).coerceAtLeast(1)
        byApp.forEach { (a, sec, n) ->
            box.addView(tv("$a — ${fmt(sec)} ($n ${if (n == 1) "sessão" else "sessões"})", 15f, top = 8))
            box.addView(bar(sec.toFloat() / max))
        }

        box.addView(tv("Sessões por hora do dia", 20f, top = 24))
        val hours = IntArray(24)
        val cal = Calendar.getInstance()
        list.forEach { cal.timeInMillis = it.start; hours[cal.get(Calendar.HOUR_OF_DAY)]++ }
        val hmax = hours.max().coerceAtLeast(1)
        val chart = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        hours.forEach { n ->
            val h = if (n == 0) dp(2) else (dp(100) * n / hmax).coerceAtLeast(dp(6))
            chart.addView(View(this).apply {
                setBackgroundColor(if (n == 0) Color.LTGRAY else accent)
            }, LinearLayout.LayoutParams(0, h, 1f).apply { setMargins(1, 0, 1, 0) })
        }
        box.addView(chart, LinearLayout.LayoutParams(-1, dp(100)))
        val axis = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("0h", "6h", "12h", "18h").forEach {
            axis.addView(tv(it, 12f, Color.GRAY), LinearLayout.LayoutParams(0, -2, 1f))
        }
        box.addView(axis)

        box.addView(tv("Últimos motivos", 20f, top = 24))
        val df = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        list.sortedByDescending { it.start }.take(10).forEach {
            box.addView(tv("${df.format(Date(it.start))} · ${it.app} · ${fmt(it.actualSec)}", 13f, Color.GRAY, 10))
            box.addView(tv(it.why, 16f))
        }
    }
}
