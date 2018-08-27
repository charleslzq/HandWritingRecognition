package com.github.charleslzq.hwr.view

import android.util.Log

object HWREngine {
    const val TAG = "HWREngine"
    private val builderRegistry = mutableMapOf<String, () -> HandWritingRecognizer>()
    private val recognizerRegistry = mutableMapOf<String, HandWritingRecognizer>()
    val engines: Set<String>
        get() = builderRegistry.keys

    @JvmOverloads
    fun register(name: String, autoInit: Boolean = true, build: () -> HandWritingRecognizer) {
        builderRegistry[name] = build
        if (autoInit) {
            initRecognizer(name)
        }
    }

    @JvmOverloads
    fun register(name: String, autoInit: Boolean = true, builder: RecognizerBuilder) = register(name, autoInit) {
        builder.build()
    }

    fun initRecognizer(name: String) {
        if (!recognizerRegistry.containsKey(name) && engines.contains(name)) {
            builderRegistry[name]!!().let {
                it.init()
                recognizerRegistry[name] = it
            }
        }
    }

    fun runRecognizer(name: String, candidateBuilder: CandidateBuilder, strokes: List<HandWritingView.Stroke>): List<Candidate> = when {
        recognizerRegistry.containsKey(name) -> recognizerRegistry[name]!!.recognize(strokes, candidateBuilder)
        builderRegistry.containsKey(name) -> builderRegistry[name]!!().let {
            it.init()
            recognizerRegistry[name] = it
            it.recognize(strokes, candidateBuilder)
        }
        else -> {
            Log.w(TAG, "Engine with name $name not found")
            emptyList()
        }
    }
}

interface HandWritingRecognizer {
    fun init() {}
    fun recognize(strokes: List<HandWritingView.Stroke>, candidateBuilder: CandidateBuilder): List<Candidate>
}

interface RecognizerBuilder {
    fun build(): HandWritingRecognizer
}