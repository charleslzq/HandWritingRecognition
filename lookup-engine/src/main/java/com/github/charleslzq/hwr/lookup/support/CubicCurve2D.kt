package com.github.charleslzq.hwr.lookup.support

import kotlin.math.min

data class CubicCurve2D(
        val x1: Double,
        val y1: Double,
        val ctrlX1: Double,
        val ctrlY1: Double,
        val ctrlX2: Double,
        val ctrlY2: Double,
        val x2: Double,
        val y2: Double
) {
    val cubicAx: Double
        get() = x2 - x1 - cubicBx - cubicCx
    val cubixAy: Double
        get() = y2 - y1 - cubicBy - cubixCy
    val cubicBx: Double
        get() = 3 * (ctrlX2 - ctrlX1) - cubicCx
    val cubicBy: Double
        get() = 3 * (ctrlY2 - ctrlY1) - cubixCy
    val cubicCx: Double
        get() = 3 * (ctrlX1 - x1)
    val cubixCy: Double
        get() = 3 * (ctrlY1 - y1)

    fun solveForX(x: Double): List<Double> {
        val a = cubicAx
        val b = cubicBx
        val c = cubicCx
        val d = x1 - x
        val f = ((3.0 * c / a) - (b * b / (a * a))) / 3.0
        val g = ((2.0 * b * b * b / (a * a * a)) - (9.0 * b * c / (a * a)) + (27.0 * d / a)) / 27.0
        val h = (g * g / 4.0) + (f * f * f / 27.0)

        return if (h > 0) {
            val u = 0 - g
            val r = (u / 2) + (Math.pow(h, 0.5))
            val s6 = (Math.pow(r, 0.333333333333333333333333333))
            val s8 = s6
            val t8 = (u / 2) - (Math.pow(h, 0.5))
            val v7 = (Math.pow((0 - t8), 0.33333333333333333333))
            val v8 = (v7)
            listOf((s8 - v8) - (b / (3 * a)))
        } else if (f == 0.0 && g == 0.0 && h == 0.0) {
            listOf(-Math.pow(d / a, 1.0 / 3.0))
        } else {
            val i = Math.sqrt((g * g / 4.0) - h)
            val j = Math.pow(i, 1.0 / 3.0)
            val k = Math.acos(-g / (2 * i))
            val l = j * -1.0
            val m = Math.cos(k / 3.0)
            val n = Math.sqrt(3.0) * Math.sin(k / 3.0)
            val p = (b / (3.0 * a)) * -1.0
            listOf(2.0 * j * Math.cos(k / 3.0) - (b / (3.0 * a)), l * (m + n) + p, l * (m - n) + p)
        }
    }

    fun getYOnCurve(t: Double): Double =
            cubixAy * t * t * t + cubicBy * t * t + cubixCy * t + y1

    fun getFirstSolutionForX(x: Double): Double? =
            solveForX(x).firstOrNull {
                it in -0.00000001..1.00000001
            }?.let {
                when {
                    it < 0 -> 0.0
                    it in 0.0..1.0 -> it
                    else -> 1.0
                }
            }

    fun initScoreTable(numSample: Int): Array<Double> {
        val range = x2 - x1
        var x = x1
        val xInc = range / numSample
        return Array(numSample) {
            val t = getFirstSolutionForX(min(x, x2)) ?: Double.NaN
            x += xInc
            getYOnCurve(t)
        }
    }
}