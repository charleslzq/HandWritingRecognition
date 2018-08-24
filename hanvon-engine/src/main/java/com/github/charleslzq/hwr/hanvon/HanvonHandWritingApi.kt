package com.github.charleslzq.hwr.hanvon

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface HanvonHandWritingApi {
    @POST("/rt/ws/v1/hand/line")
    fun recognizeMultiSC(
            @Query("key") key: String,
            @Query("code") code: String,
            @Body data: MultiStrokesData
    ): Call<String>

    data class MultiStrokesData(
            val uid: String,
            val lang: String,
            val data: String
    )

    @POST("/rt/ws/v1/hand/single")
    fun recOrAssociateSingleSC(
            @Query("key") key: String,
            @Query("code") code: String,
            @Body data: StrokeData
    ): Call<String>

    data class StrokeData(
            val uid: String,
            val type: String,
            val data: String
    )
}