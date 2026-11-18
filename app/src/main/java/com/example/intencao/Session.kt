package com.example.intencao

import android.content.Context

/** Sessão de uso em andamento. Termina ao bloquear a tela ou ao tocar em "Encerrar". */
data class Session(val app: String, val pkg: String, val why: String, val start: Long, val plannedMin: Int) {
    val endsAt: Long get() = start + plannedMin * 60_000L

    companion object {
        private fun prefs(c: Context) = c.getSharedPreferences("session", Context.MODE_PRIVATE)

        fun current(c: Context): Session? {
            val sp = prefs(c)
            val app = sp.getString("app", null) ?: return null
            return Session(app, sp.getString("pkg", "") ?: "", sp.getString("why", "") ?: "",
                sp.getLong("start", 0L), sp.getInt("planned", 0))
        }

        fun start(c: Context, s: Session) {
            end(c)
            prefs(c).edit().putString("app", s.app).putString("pkg", s.pkg).putString("why", s.why)
                .putLong("start", s.start).putInt("planned", s.plannedMin).apply()
        }

        fun end(c: Context) {
            val s = current(c) ?: return
            val actual = ((System.currentTimeMillis() - s.start) / 1000).toInt().coerceIn(0, 4 * 3600)
            UseLog.add(c, s, actual)
            prefs(c).edit().clear().apply()
        }
    }
}
