package com.github.charleslzq.hwr.hanvon

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


fun String.toSafeRetrofitUrl() = takeIf { endsWith("/") } ?: this+"/"

class HanvonApiHolder
@JvmOverloads
constructor(
        baseUrl: String,
        private val retrofitBuilder: (String) -> Retrofit = {
            Retrofit.Builder()
                    .baseUrl(it.toSafeRetrofitUrl())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
        }
) {
    var url = baseUrl
        set(value) {
            if (value != field) {
                retrofit = retrofitBuilder(value)
                handWritingApi = retrofit.create(HanvonHandWritingApi::class.java)
                field = value
            }
        }
    private var retrofit: Retrofit = retrofitBuilder(baseUrl)
    var handWritingApi: HanvonHandWritingApi = retrofit.create(HanvonHandWritingApi::class.java)
        private set
}