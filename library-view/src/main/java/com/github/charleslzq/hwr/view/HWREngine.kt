package com.github.charleslzq.hwr.view

import android.util.Log

object HWREngine {
    const val TAG = "HWREngine"
    private val builderRegistry = mutableMapOf<String, () -> HandWritingRecognizer>()
    private val recognizerRegistry = mutableMapOf<String, HandWritingRecognizer>()
    val engines: Set<String>
        get() = builderRegistry.keys


    fun register(name: String, build: () -> HandWritingRecognizer) {
        builderRegistry[name] = build
    }

    fun register(name: String, builder: RecognizerBuilder) {
        builderRegistry[name] = {
            builder.build()
        }
    }

    fun runRecognizer(name: String, candidateBuilder: CandidateBuilder, strokes: List<HandWritingView.Stroke>): List<Candidate> = when {
        recognizerRegistry.containsKey(name) -> recognizerRegistry[name]!!.recognize(strokes, candidateBuilder)
        builderRegistry.containsKey(name) -> {
            recognizerRegistry[name] = builderRegistry[name]!!()
            recognizerRegistry[name]!!.recognize(strokes, candidateBuilder)
        }
        else -> {
            Log.w(TAG, "Engine with name $name not found")
            emptyList()
        }
    }
}

interface HandWritingRecognizer {
    fun recognize(strokes: List<HandWritingView.Stroke>, candidateBuilder: CandidateBuilder): List<Candidate>
}

interface RecognizerBuilder {
    fun build(): HandWritingRecognizer
}