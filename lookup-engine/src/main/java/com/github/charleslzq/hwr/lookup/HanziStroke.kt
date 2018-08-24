package com.github.charleslzq.hwr.lookup

import android.graphics.PointF
import android.graphics.RectF
import com.github.charleslzq.hwr.view.HandWritingView
import kotlin.math.*

fun RectF.extend(point: PointF): Boolean {
    var changed = false
    if (point.x < left) {
        left = point.x
        changed = true
    } else if (point.x > right) {
        right = point.x
        changed = true
    }
    if (point.y < top) {
        top = point.y
        changed = true
    } else if (point.y > bottom) {
        bottom = point.y
        changed = true
    }
    return changed
}

fun PointF.distance(point: PointF): Float {
    val dx = x - point.x
    val dy = y - point.y
    return sqrt(dx * dx + dy * dy)
}

fun PointF.directionTo(point: PointF) = PI - atan2(y - point.y, x - point.x)

class HanziStroke
internal constructor(rawStroke: HandWritingView.Stroke, useHistoricalPoint: Boolean = false) {
    val points = rawStroke.points.filter { useHistoricalPoint || !it.isHistorical }.map { it.point }.toList()
    val pivots: List<Int>
        get() {
            val markers = List(points.size) { false }.toMutableList()
            var prevPtIx = 0
            var firstPtIx = 0
            var pivotPtIx = 1

            if (markers.isNotEmpty()) {
                markers[0] = true
            }
            if (markers.size > 2) {
                var localLength = points[firstPtIx].distance(points[pivotPtIx])
                var runningLength = localLength
                repeat(points.size - 2) {
                    val nextPoint = points[it + 2]
                    val pivotLength = nextPoint.distance(points[pivotPtIx])
                    localLength += pivotLength
                    runningLength += pivotLength

                    val distFromPrevious = nextPoint.distance(points[prevPtIx])
                    val distFromFirst = nextPoint.distance(points[firstPtIx])
                    if (localLength > MAX_LOCAL_LENGTH_RATIO * distFromPrevious ||
                            runningLength > MAX_RUNNING_LENGTH_RATIO * distFromFirst) {
                        if (markers[prevPtIx] && points[prevPtIx].distance(points[pivotPtIx]) < MIN_SEGMENT_LENGTH) {
                            markers[prevPtIx] = false
                        }
                        markers[pivotPtIx] = true
                        runningLength = pivotLength
                        firstPtIx = pivotPtIx
                    }
                    localLength = pivotLength
                    prevPtIx = pivotPtIx
                    pivotPtIx = it + 2
                }

                markers[pivotPtIx] = true
                if (markers[prevPtIx] && points[prevPtIx].distance(points[pivotPtIx]) < MIN_SEGMENT_LENGTH && prevPtIx != 0) {
                    markers[prevPtIx] = false
                }
            }

            return (0 until markers.size).filter { markers[it] }
        }

    fun getSubStrokes(boundary: RectF): List<Sub> {
        var prevIx = 0
        val result = mutableListOf<Sub>()
        for (index in pivots) {
            if (index != prevIx) {
                val direction = round(points[prevIx].directionTo(points[index]) * 256.0 / PI / 2.0).let {
                    if (it == 256.0) {
                        0.0
                    } else {
                        it
                    }
                }
                val normLength = round(getNormDist(boundary, points[prevIx], points[index]) * 255)
                val normCenter = getNormCenter(boundary, points[prevIx], points[index])
                result.add(Sub(
                        direction.toFloat(),
                        normLength,
                        PointF(round(normCenter.x * 15), round(normCenter.y * 15))
                ))
                prevIx = index
            }
        }
        return result
    }

    private fun getNormDist(boundary: RectF, pointA: PointF, pointB: PointF): Float {
        val normalizer = sqrt(2 * if (boundary.width() > boundary.height()) {
            boundary.width() * boundary.width()
        } else {
            boundary.height() * boundary.height()
        })
        return min(1f, pointA.distance(pointB) / normalizer)
    }

    private fun getNormCenter(boundary: RectF, pointA: PointF, pointB: PointF): PointF {
        var x = (pointA.x + pointB.x) / 2
        var y = (pointB.y + pointB.y) / 2
        val side: Float
        if (boundary.width() > boundary.height()) {
            side = boundary.width()
            x -= boundary.left
            y += (side - boundary.height()) / 2 - boundary.top
        } else {
            side = boundary.height()
            x += (side - boundary.width()) / 2 - boundary.left
            y -= boundary.top
        }
        return PointF(x / side, y / side)
    }

    data class Sub(val direction: Float, val length: Float, val center: PointF)

    companion object {
        const val MIN_SEGMENT_LENGTH = 12.5
        const val MAX_LOCAL_LENGTH_RATIO = 1.1
        const val MAX_RUNNING_LENGTH_RATIO = 1.09
    }
}