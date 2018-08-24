package com.github.charleslzq.hwr

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.widget.Button
import android.widget.PopupMenu
import com.github.charleslzq.hwr.view.HWREngine
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hwrView.onCandidateSelected {
            text.append(it)
            updateButtonState()
            if (hwrView.engine == "hanvon-m" || hwrView.engine == "lookup") {
                candidates.removeAllViews()
            }
        }
        hwrView.onCandidatesAvailable {
            updateButtonState()
            candidates.removeAllViews()
            it.forEach {
                Button(candidates.context).apply {
                    it.bind(this)
                }.also {
                    candidates.addView(it)
                }
            }
        }
        undoButton.setOnClickListener {
            hwrView.undo()
            updateButtonState()
        }
        redoButton.setOnClickListener {
            hwrView.redo()
            updateButtonState()
        }
        clearButton.setOnClickListener {
            hwrView.reset()
            candidates.removeAllViewsInLayout()
            updateButtonState()
        }
        resetButton.setOnClickListener {
            text.text = ""
            hwrView.reset()
            candidates.removeAllViews()
            updateButtonState()
        }

        val engines = Array(HWREngine.engines.size) { "" }
        val engineSelector = PopupMenu(buttonPanel.context, selectButton).apply {
            HWREngine.engines.forEachIndexed { index, name ->
                menu.add(Menu.NONE, index, Menu.NONE, name)
                engines[index] = name
            }
            setOnMenuItemClickListener {
                hwrView.engine = engines[it.itemId]
                selectButton.text = "Engine: ${hwrView.engine}"
                true
            }
        }
        selectButton.setOnClickListener {
            engineSelector.show()
        }
        if (HWREngine.engines.contains(hwrView.engine)) {
            selectButton.text = "Engine: ${hwrView.engine}"
        }
    }

    private fun updateButtonState() {
        undoButton.isEnabled = hwrView.canUndo()
        redoButton.isEnabled = hwrView.canRedo()
    }
}
