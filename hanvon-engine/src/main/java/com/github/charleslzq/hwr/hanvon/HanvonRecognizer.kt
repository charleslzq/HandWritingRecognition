package com.github.charleslzq.hwr.hanvon

import com.github.charleslzq.hwr.view.CandidateBuilder
import com.github.charleslzq.hwr.view.HandWritingRecognizer
import com.github.charleslzq.hwr.view.HandWritingView

sealed class HanvonRecognizer(
        protected val hanvonWritingApiAdapter: HanvonWritingApiAdapter,
        protected val ip: String,
        protected val key: String
)

class MultiHanvonRecognizer(
        hanvonWritingApiAdapter: HanvonWritingApiAdapter,
        ip: String,
        key: String
) : HanvonRecognizer(hanvonWritingApiAdapter, ip, key), HandWritingRecognizer {
    override fun recognize(strokes: List<HandWritingView.Stroke>, candidateBuilder: CandidateBuilder) =
            hanvonWritingApiAdapter.recognizeMultiSC(key, ip, candidateBuilder, strokes)
}

class SingleHanvonRecognizer(
        hanvonWritingApiAdapter: HanvonWritingApiAdapter,
        ip: String,
        key: String
) : HanvonRecognizer(hanvonWritingApiAdapter, ip, key), HandWritingRecognizer {
    override fun recognize(strokes: List<HandWritingView.Stroke>, candidateBuilder: CandidateBuilder) =
            hanvonWritingApiAdapter.recognizeSingleSC(key, ip, candidateBuilder, strokes)
}