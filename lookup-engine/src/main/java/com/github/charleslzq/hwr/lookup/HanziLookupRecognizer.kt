package com.github.charleslzq.hwr.lookup

import android.graphics.PointF
import android.graphics.RectF
import com.github.charleslzq.hwr.lookup.support.Array2D
import com.github.charleslzq.hwr.lookup.support.CubicCurve2D
import com.github.charleslzq.hwr.view.Candidate
import com.github.charleslzq.hwr.view.CandidateBuilder
import com.github.charleslzq.hwr.view.HandWritingRecognizer
import com.github.charleslzq.hwr.view.HandWritingView
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import kotlin.math.*

data class MatchResultEntry(
        val content: String,
        val score: Double
)

class HanziLookupRecognizer(configString: String, val looseness: Double = DEFAULT_LOOSENESS) : HandWritingRecognizer {
    private val gson = GsonBuilder()
            .registerTypeAdapter(CharEntry::class.java, CharEntryDeserializer())
            .registerTypeAdapter(SubStrokesEntry::class.java, SubStrokesEntryDeserializer())
            .create()
    private val database: Database = gson.fromJson(configString, Database::class.java)
    var limit = 80

    override fun recognize(strokes: List<HandWritingView.Stroke>, candidateBuilder: CandidateBuilder): List<Candidate> {
        val hanziStrokes = strokes.map { HanziStroke(it) }
        val boundary = hanziStrokes.flatMap { it.points }.let { pointList ->
            RectF(
                    pointList.map { it.x }.min()!!,
                    pointList.map { it.y }.min()!!,
                    pointList.map { it.x }.max()!!,
                    pointList.map { it.y }.max()!!
            )
        }
        return candidateBuilder.buildSimple(match(hanziStrokes, boundary).map { it.content })
    }

    fun match(hanziStrokes: List<HanziStroke>, boundary: RectF): List<MatchResultEntry> = if (database.chars.isEmpty()) {
        emptyList()
    } else {
        val inputSubStrokes = hanziStrokes.flatMap { it.getSubStrokes(boundary) }
        val strokeRange = getStrokeRange(hanziStrokes.size).let {
            max(hanziStrokes.size - it, 1)..min(hanziStrokes.size + it, MAX_CHARACTER_STROKE_COUNT)
        }
        val subStrokeRange = getSubStrokeRange(inputSubStrokes.size).let {
            max(inputSubStrokes.size - it, 1)..min(inputSubStrokes.size + it, MAX_CHARACTER_SUB_STROKE_COUNT)
        }
        database.chars.filter {
            it.strokeCount in strokeRange && it.subStrokesCount in subStrokeRange
        }.map {
            var score = computeScore(inputSubStrokes, subStrokeRange, it)
            if (hanziStrokes.size == it.strokeCount && hanziStrokes.size < CORRECT_NUM_STROKES_CAP) {
                score += CORRECT_NUM_STROKES_BONUS * max(CORRECT_NUM_STROKES_CAP - hanziStrokes.size, 0) / CORRECT_NUM_STROKES_CAP
            }
            MatchResultEntry(it.char, score)
        }.sortedByDescending { it.score }.take(limit)
    }

    private fun computeScore(inputSubStrokes: List<HanziStroke.Sub>, subStrokeRange: IntRange, charEntry: HanziLookupRecognizer.CharEntry): Double {
        val scoreMatrix = Array2D(MAX_CHARACTER_SUB_STROKE_COUNT + 1, MAX_CHARACTER_SUB_STROKE_COUNT + 1) { x, y ->
            when {
                x == 0 -> -AVG_SUBSTROKE_LENGTH * SKIP_PENALTY_MULTIPLIER * y
                y == 0 -> -AVG_SUBSTROKE_LENGTH * SKIP_PENALTY_MULTIPLIER * x
                else -> 0.0

            }
        }
        inputSubStrokes.forEachIndexed { x, sub ->
            repeat(charEntry.subStrokesCount) { y ->
                var score = Double.NEGATIVE_INFINITY
                if (abs(x - y) <= (subStrokeRange.last - subStrokeRange.first) / 2) {
                    val charDirection = database.subStrokes.data[charEntry.indexBase + y * 3]
                    val charLength = database.subStrokes.data[charEntry.indexBase + y * 3 + 1]
                    val bCenter = database.subStrokes.data[charEntry.indexBase + y * 3 + 2]
                    val charCenter = PointF((bCenter and 0xf0).ushr(4).toFloat(), (bCenter and 0x0f).toFloat())
                    val skipScore = max(
                            scoreMatrix[x, y + 1] - (inputSubStrokes[x].length / 256 * SKIP_PENALTY_MULTIPLIER),
                            scoreMatrix[x + 1, y] - (charLength / 256 * SKIP_PENALTY_MULTIPLIER)
                    )
                    val matchScore = computeSubStrokeScore(inputSubStrokes[x].direction, inputSubStrokes[x].length, inputSubStrokes[x].center, charDirection, charLength, charCenter)
                    score = max(skipScore, scoreMatrix[x, y] + matchScore)
                }
                scoreMatrix[x + 1, y + 1] = score
            }
        }
        return scoreMatrix[inputSubStrokes.size, charEntry.subStrokesCount]
    }

    private fun computeSubStrokeScore(direction: Float, length: Float, center: PointF, charDirection: Int, charLength: Int, charCenter: PointF): Double {
        val dirScore = getDirScore(direction, charDirection, length)
        val lenScore = getLenScore(length, charLength)
        var score = dirScore * lenScore

        val dx = (center.x - charCenter.x).toInt()
        val dy = (center.y - charCenter.y).toInt()
        val closeness = POS_SCORE_TABLE[dx * dx + dy * dy]
        if (score > 0) {
            score *= closeness
        } else {
            score /= closeness
        }
        return score
    }

    private fun getDirScore(direction: Float, charDirection: Int, length: Float): Double {
        var score = DIR_SCORE_TABLE[abs(direction.toInt() - charDirection)]
        if (length < 64) {
            val bonuxMax = min(1.0, 1.0 - score)
            score += bonuxMax * (1 - length / 64)
        }
        return score
    }

    private fun getLenScore(length: Float, charLength: Int): Double {
        val ratio = if (length > charLength) {
            round((charLength shl 7) / length).toInt()
        } else {
            round((length.toInt() shl 7) / charLength.toDouble()).toInt()
        }
        return LEN_SCORE_TABLE[ratio]
    }

    private fun getStrokeRange(count: Int): Int = if (looseness == 0.0) {
        0
    } else if (looseness == 1.0) {
        MAX_CHARACTER_STROKE_COUNT
    } else {
        val ctrl1X = 0.35
        val ctrl1Y = count * 0.4
        val ctrl2X = 0.6
        val ctrl2Y = count.toDouble()
        val curve = CubicCurve2D(0.0, 0.0, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, 1.0, MAX_CHARACTER_STROKE_COUNT.toDouble())
        val t = curve.getFirstSolutionForX(looseness) ?: Double.NaN
        round(curve.getYOnCurve(t)).toInt()
    }

    private fun getSubStrokeRange(subStrokeCount: Int): Int = if (looseness == 1.0) {
        MAX_CHARACTER_SUB_STROKE_COUNT
    } else {
        val y0 = subStrokeCount * 0.25
        val ctrl1X = 0.4
        val ctrl1Y = 1.5 * y0
        val ctrl2X = 0.75
        val ctrl2Y = 1.5 * ctrl1Y
        val curve = CubicCurve2D(0.0, y0, ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, 1.0, MAX_CHARACTER_SUB_STROKE_COUNT.toDouble())
        val t = curve.getFirstSolutionForX(looseness) ?: Double.NaN
        round(curve.getYOnCurve(t)).toInt()
    }

    data class CharEntry(
            val char: String,
            val strokeCount: Int,
            val subStrokesCount: Int,
            val indexBase: Int
    )

    data class SubStrokesEntry(val data: List<Int>)

    data class Database(
            val chars: List<CharEntry>,
            @SerializedName("substrokes")
            val subStrokes: SubStrokesEntry
    )

    companion object {
        const val MAX_CHARACTER_STROKE_COUNT = 48
        const val MAX_CHARACTER_SUB_STROKE_COUNT = 64
        const val DEFAULT_LOOSENESS = 0.15
        const val AVG_SUBSTROKE_LENGTH = 0.33 // an average length (out of 1)
        const val SKIP_PENALTY_MULTIPLIER = 1.75 // penalty mulitplier for skipping a stroke
        const val CORRECT_NUM_STROKES_BONUS = 0.1 // max multiplier bonus if characters has the correct number of strokes
        const val CORRECT_NUM_STROKES_CAP = 10 // characters with more strokes than this will not be multiplied
        val DIR_SCORE_TABLE = CubicCurve2D(0.0, 1.0, 0.5, 1.0, 0.25, -2.0, 1.0, 1.0).initScoreTable(256)
        val LEN_SCORE_TABLE = CubicCurve2D(0.0, 0.0, 0.25, 1.0, 0.75, 1.0, 1.0, 1.0).initScoreTable(129)
        val POS_SCORE_TABLE = Array(451) {
            1 - sqrt(it.toDouble()) / 22
        }
    }
}

class CharEntryDeserializer : JsonDeserializer<HanziLookupRecognizer.CharEntry> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): HanziLookupRecognizer.CharEntry {
        val elements = json.asJsonArray
        return HanziLookupRecognizer.CharEntry(
                elements[0].asString,
                elements[1].asInt,
                elements[2].asInt,
                elements[3].asInt
        )
    }
}

class SubStrokesEntryDeserializer : JsonDeserializer<HanziLookupRecognizer.SubStrokesEntry> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): HanziLookupRecognizer.SubStrokesEntry {
        return HanziLookupRecognizer.SubStrokesEntry(decode(json.asString).toList())
    }

    private fun decode(base64: String): IntArray {
        var bufferLength = (base64.length * 0.75).roundToInt()
        val len = base64.length
        var p = 0
        var encoded1: Int
        var encoded2: Int
        var encoded3: Int
        var encoded4: Int

        if (base64[base64.length - 1] == '=') {
            bufferLength--
            if (base64[base64.length - 2] == '=') {
                bufferLength--
            }
        }

        val ints = IntArray(bufferLength)
        var i = 0
        while (i < len) {
            encoded1 = table[base64.charCodeAt(i)]
            encoded2 = table[base64.charCodeAt(i + 1)]
            encoded3 = table[base64.charCodeAt(i + 2)]
            encoded4 = table[base64.charCodeAt(i + 3)]

            ints[p++] = encoded1 shl 2 or (encoded2 shr 4)
            ints[p++] = encoded2 and 15 shl 4 or (encoded3 shr 2)
            ints[p++] = encoded3 and 3 shl 6 or (encoded4 and 63)
            i += 4
        }

        return ints
    }

    companion object {
        const val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val table = List(256) {
            0
        }.toMutableList().apply {
            repeat(chars.length) {
                set(chars.charCodeAt(it), it)
            }
        }.toList()
    }

}

fun String.charCodeAt(index: Int) = Character.codePointAt(this, index)