package com.github.charleslzq.hwr.view

import android.widget.Button
import com.github.charleslzq.hwr.view.support.runOnCompute

class Candidate
internal constructor
(
        val content: String,
        private val onSelect: (String) -> Unit = {}
) {
    fun select() {
        onSelect(content)
    }

    fun bind(button: Button) {
        button.text = content
        button.setOnClickListener {
            select()
        }
    }
}

class CandidateBuilder(
        private val candidatePublisher: HandWritingView.CandidatePublisher
) {
    fun buildSimple(strings: List<String>) = strings.map { Candidate(it) }

    fun buildAssociatable(strings: List<String>, associate: (String) -> List<Candidate>) = strings.map { char ->
        Candidate(char) {
            runOnCompute {
                candidatePublisher.publishCandidates(associate(it))
            }
        }
    }
}
