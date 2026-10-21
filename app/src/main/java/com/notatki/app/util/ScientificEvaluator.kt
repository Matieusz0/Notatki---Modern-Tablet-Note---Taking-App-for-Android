package com.notatki.app.util

import kotlin.math.*

object ScientificEvaluator {

    fun evaluate(expression: String, isDegreeMode: Boolean = true): Result<Double> {
        return try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "${Math.PI}")
                .replace("pi", "${Math.PI}")
                .replace("e", "${Math.E}")
                .replace("√", "sqrt")
                .replace(" ", "")

            val result = Parser(sanitized, isDegreeMode).parse()
            if (result.isNaN() || result.isInfinite()) {
                Result.failure(IllegalArgumentException("Błąd matematyczny (np. dzielenie przez 0)"))
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0 && abs(value) < 1e12) {
            value.toLong().toString()
        } else {
            // Skracanie do 6 miejsc po przecinku
            val str = String.format(java.util.Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
            str
        }
    }

    private class Parser(private val str: String, private val isDegreeMode: Boolean) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            pos++
            ch = if (pos < str.length) str[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Niespodziewany znak: $ch")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Dzielenie przez 0")
                        x /= divisor
                    }
                    eat('%') -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return +parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'..'z') {
                while (ch in 'a'..'z') nextChar()
                val func = str.substring(startPos, pos)
                if (eat('(')) {
                    x = parseExpression()
                    eat(')')
                } else {
                    x = parseFactor()
                }
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> {
                        val angle = if (isDegreeMode) Math.toRadians(x) else x
                        sin(angle)
                    }
                    "cos" -> {
                        val angle = if (isDegreeMode) Math.toRadians(x) else x
                        cos(angle)
                    }
                    "tan" -> {
                        val angle = if (isDegreeMode) Math.toRadians(x) else x
                        tan(angle)
                    }
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    else -> throw RuntimeException("Nieznana funkcja: $func")
                }
            } else {
                throw RuntimeException("Nieoczekiwany symbol: $ch")
            }

            if (eat('^')) x = x.pow(parseFactor())

            return x
        }
    }
}
