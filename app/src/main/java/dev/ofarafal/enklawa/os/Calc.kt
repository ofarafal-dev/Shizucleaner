package dev.ofarafal.enklawa.os

import java.math.BigDecimal
import java.math.MathContext

/** Prosty kalkulator wyrażeń: + − × ÷, nawiasy, minus jednoargumentowy, liczby dziesiętne. Zwraca null przy błędzie. */
object Calc {

    fun eval(input: String): Double? {
        val s = input.replace('×', '*').replace('÷', '/').replace('−', '-').replace(" ", "")
        if (s.isEmpty()) return null
        return try {
            val p = Parser(s)
            val v = p.expr()
            if (p.pos != s.length || v.isNaN() || v.isInfinite()) null else v
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun format(d: Double): String {
        if (d == Math.floor(d) && Math.abs(d) < 1e15) return d.toLong().toString()
        return BigDecimal(d).round(MathContext(12)).stripTrailingZeros().toPlainString()
    }

    private class Parser(val s: String) {
        var pos = 0

        fun expr(): Double {
            var v = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val r = term()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        fun term(): Double {
            var v = factor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                val op = s[pos++]
                val r = factor()
                v = if (op == '*') v * r else v / r
            }
            return v
        }

        fun factor(): Double {
            if (pos >= s.length) throw IllegalArgumentException("koniec")
            val c = s[pos]
            if (c == '-') { pos++; return -factor() }
            if (c == '+') { pos++; return factor() }
            if (c == '(') {
                pos++
                val v = expr()
                if (pos >= s.length || s[pos] != ')') throw IllegalArgumentException("nawias")
                pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("liczba")
            return s.substring(start, pos).toDouble()
        }
    }
}
