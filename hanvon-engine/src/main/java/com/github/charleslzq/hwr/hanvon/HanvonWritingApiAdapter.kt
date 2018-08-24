package com.github.charleslzq.hwr.hanvon

import android.graphics.PointF
import android.util.Base64
import android.util.Log
import com.github.charleslzq.hwr.view.Candidate
import com.github.charleslzq.hwr.view.CandidateBuilder
import com.github.charleslzq.hwr.view.HandWritingView
import com.google.gson.Gson
import retrofit2.Call
import java.nio.charset.Charset

class HanvonWritingApiAdapter(
        private val hanvonHandWritingApi: HanvonHandWritingApi,
        private val useHistorical: Boolean = false
) {
    private val gson = Gson()

    fun recognizeMultiSC(key: String, ip: String, candidateBuilder: CandidateBuilder, strokes: List<HandWritingView.Stroke>): List<Candidate> {
        val pointArray = strokes.toPointArray(true)
        val request = hanvonHandWritingApi.recognizeMultiSC(
                key,
                MULTI_CHNS_CODE,
                HanvonHandWritingApi.MultiStrokesData(
                        ip,
                        "chns",
                        gson.toJson(pointArray)
                )
        )
        return try {
            request.executeAndParse()?.let {
                candidateBuilder.buildSimple(it.getWords())
            } ?: emptyList()
        } catch (e: Throwable) {
            Log.e(TAG, "Error happen when accessing Hanvon web api", e)
            emptyList()
        }
    }

    fun recognizeSingleSC(key: String, ip: String, candidateBuilder: CandidateBuilder, strokes: List<HandWritingView.Stroke>): List<Candidate> {
        val pointArray = strokes.toPointArray()
        val request = hanvonHandWritingApi.recOrAssociateSingleSC(
                key,
                SING_CHNS_CODE,
                HanvonHandWritingApi.StrokeData(
                        ip,
                        "1",
                        gson.toJson(pointArray)
                )
        )
        return try {
            request.executeAndParse()?.let {
                candidateBuilder.buildAssociatable(it.getCharacters()) { selected ->
                    associate(key, ip, candidateBuilder, selected)
                }
            } ?: emptyList()
        } catch (e: Throwable) {
            Log.e(TAG, "Error happen when accessing Hanvon web api", e)
            emptyList()
        }
    }

    private fun associate(key: String, ip: String, candidateBuilder: CandidateBuilder, char: String): List<Candidate> {
        val unicode = Character.codePointAt(char, char.lastIndex)
        val request = hanvonHandWritingApi.recOrAssociateSingleSC(
                key,
                SING_CHNS_CODE,
                HanvonHandWritingApi.StrokeData(
                        ip,
                        "2",
                        unicode.toString()
                )
        )
        return try {
            request.executeAndParse()?.let { result ->
                candidateBuilder.buildAssociatable(result.getCharacters()) { selected ->
                    associate(key, ip, candidateBuilder, selected)
                }
            } ?: emptyList()
        } catch (e: Throwable) {
            Log.e(TAG, "Error happen when accessing Hanvon web api", e)
            emptyList()
        }
    }

    private fun Call<String>.executeAndParse() = execute().body()?.let {
        val data = String(Base64.decode(it, Base64.DEFAULT), Charset.forName("UTF-8"))
        gson.fromJson(data, HanvonHWRResult::class.java)
    }

    private fun List<HandWritingView.Stroke>.toPointArray(multi: Boolean = false) = flatMap { stroke ->
        stroke.points.let {
            if (multi) {
                it.toMutableList().apply {
                    add(HandWritingView.StrokePoint(PointF(-1f, 0f), false))
                }
            } else {
                it
            }
        }.filter {
            useHistorical || !it.isHistorical
        }
    }.let {
        it.toMutableList().apply {
            if (multi) {
                add(HandWritingView.StrokePoint(PointF(-1f, -1f), false))
            } else {
                add(HandWritingView.StrokePoint(PointF(-1f, 0f), false))
            }
        }
    }.flatMap { listOf(it.point.x.toInt(), it.point.y.toInt()) }

    data class HanvonHWRResult(
            val code: Int,
            val result: String
    ) {
        fun getCharacters(): List<String> =
                result.split(",".toRegex()).dropLastWhile { it.isEmpty() }.map {
                    it.toInt().toChar().toString()
                }

        fun getWords(): List<String> =
                result.split(",0,".toRegex()).dropLastWhile { it.isEmpty() }.map { wordRawString ->
                    wordRawString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.map { charCodeString ->
                        charCodeString.toInt().toChar()
                    }.joinToString("")
                }
    }

    companion object {
        const val TAG = "HanvonWritingApiAdapter"
        const val MULTI_CHNS_CODE = "d4b92957-78ed-4c52-a004-ac3928b054b5"
        const val SING_CHNS_CODE = "83b798e7-cd10-4ce3-bd56-7b9e66ace93d"
    }

}