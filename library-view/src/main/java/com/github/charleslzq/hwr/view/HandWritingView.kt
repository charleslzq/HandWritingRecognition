package com.github.charleslzq.hwr.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.github.charleslzq.hanzi.support.UndoSupport
import com.github.charleslzq.hwr.view.support.runOnCompute
import com.github.charleslzq.hwr.view.support.runOnUI
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject

class HandWritingView
@JvmOverloads
constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyle: Int = 0
) : ImageView(context, attributeSet, defStyle) {
    private val strokes: UndoSupport<Stroke> = UndoSupport()
    private var currentStroke: Stroke? = null
    private val publisher = PublishSubject.create<List<Candidate>>()
    private val candidateBuilder = CandidateBuilder(object : CandidatePublisher {
        override fun publishCandidates(candidates: List<Candidate>) {
            publish {
                candidates.map { rawCandidate ->
                    Candidate(rawCandidate.content) {
                        onSelect(it)
                        rawCandidate.select()
                    }
                }
            }
        }
    })
    private var onSelect: (String) -> Unit = {}
    var engine: String = ""
        set(value) {
            HWREngine.initRecognizer(value)
            field = value
        }
    val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 3f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    init {
        attributeSet?.let {
            context.obtainStyledAttributes(it, R.styleable.HandWritingView, defStyle, 0).apply {
                engine = getString(R.styleable.HandWritingView_engine)
                recycle()
            }
        }
        setOnTouchListener(StrokeListener {
            currentStroke = it
            if (it.finished) {
                strokes.done(it)
                currentStroke = null
                recognize()
            }
            invalidate()
        })
    }

    fun canUndo() = strokes.canUndo()

    fun canRedo() = strokes.canRedo()

    fun undo() {
        if (canUndo()) {
            strokes.undo()
            recognize()
            invalidate()
        }
    }

    fun redo() {
        if (canRedo()) {
            strokes.redo()
            recognize()
            invalidate()
        }
    }

    fun reset() {
        strokes.reset()
        currentStroke = null
        invalidate()
    }

    fun onCandidatesAvailable(handler: (List<Candidate>) -> Unit) {
        publisher.observeOn(AndroidSchedulers.mainThread()).subscribe(handler)
    }

    fun onCandidatesAvailable(resultHandler: ResultHandler) = onCandidatesAvailable {
        resultHandler.receive(it)
    }

    fun onCandidateSelected(handler: (String) -> Unit) {
        onSelect = {
            runOnUI {
                handler(it)
            }
        }
    }

    fun onCandidateSelected(candidateHandler: CandidateHandler) {
        onSelect = {
            runOnUI {
                candidateHandler.selected(it)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        strokes.doneList().forEach {
            canvas.drawPath(it.path, paint)
        }
        currentStroke?.let {
            canvas.drawPath(it.path, paint)
        }
    }

    private fun recognize() = publish {
        HWREngine.runRecognizer(engine, candidateBuilder, strokes.doneList().toList()).map { rawCandidate ->
            Candidate(rawCandidate.content) {
                reset()
                onSelect(it)
                rawCandidate.select()
            }
        }
    }

    private fun publish(generateData: () -> List<Candidate>) = runOnCompute {
        publisher.onNext(generateData())
    }

    class Stroke {
        val points = mutableListOf<StrokePoint>()
        var finished = false
            private set
        val path = Path()

        @JvmOverloads
        fun addPoint(x: Float, y: Float, isHistorical: Boolean = false, isLast: Boolean = false) {
            if (!finished) {
                if (path.isEmpty) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                points.add(StrokePoint(PointF(x, y), isHistorical))
                if (isLast) {
                    finished = true
                }
            }
        }
    }

    data class StrokePoint(
            val point: PointF,
            val isHistorical: Boolean
    )

    class StrokeListener(
            val onStroke: (Stroke) -> Unit
    ) : View.OnTouchListener {
        private var currentStroke = Stroke()

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            val isLast = motionEvent.action == MotionEvent.ACTION_UP
            repeat(motionEvent.historySize) {
                currentStroke.addPoint(
                        motionEvent.getHistoricalX(it),
                        motionEvent.getHistoricalY(it),
                        true,
                        false
                )
            }
            currentStroke.addPoint(motionEvent.x, motionEvent.y, false, isLast)
            onStroke(currentStroke)
            if (isLast) {
                currentStroke = Stroke()
            }
            return true
        }
    }

    interface ResultHandler {
        fun receive(candidates: List<Candidate>)
    }

    interface CandidateHandler {
        fun selected(content: String)
    }

    interface CandidatePublisher {
        fun publishCandidates(candidates: List<Candidate>)
    }
}