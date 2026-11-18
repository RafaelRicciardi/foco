package com.example.intencao

import android.content.Context
import java.io.File

data class Entry(val start: Long, val planned: Int, val actualSec: Int, val app: String, val why: String)

/** Registro em arquivo: início(ms) \t minutos planejados \t segundos reais \t app \t motivo */
object UseLog {
    private fun file(c: Context) = File(c.filesDir, "registro.tsv")

    fun add(c: Context, s: Session, actualSec: Int) {
        fun clean(t: String) = t.replace('\n', ' ').replace('\t', ' ')
        file(c).appendText("${s.start}\t${s.plannedMin}\t$actualSec\t${clean(s.app)}\t${clean(s.why)}\n")
    }

    fun all(c: Context): List<Entry> {
        val f = file(c)
        if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull { line ->
            val p = line.split('\t', limit = 5)
            if (p.size < 5) null else try {
                Entry(p[0].toLong(), p[1].toInt(), p[2].toInt(), p[3], p[4])
            } catch (e: Exception) { null }
        }
    }
}
