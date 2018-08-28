package com.github.charleslzq.hwr;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.github.charleslzq.hwr.view.Candidate;
import com.github.charleslzq.hwr.view.HWREngine;
import com.github.charleslzq.hwr.view.HandWritingView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.text)
    TextView textView;
    @BindView(R.id.hwrView)
    HandWritingView handWritingView;
    @BindView(R.id.candidates)
    LinearLayout candidatesBar;
    @BindView(R.id.undoButton)
    Button undoButton;
    @BindView(R.id.redoButton)
    Button redoButton;
    @BindView(R.id.selectButton)
    Button selectButton;

    private List<String> engines = new ArrayList<>(HWREngine.getEngines());
    private PopupMenu engineSelector = new PopupMenu(selectButton.getContext(), selectButton);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        handWritingView.onCandidatesAvailable(new HandWritingView.ResultHandler() {
            @Override
            public void receive(List<Candidate> candidates) {
                updateButtonState();
                candidatesBar.removeAllViews();
                for (Candidate candidate : candidates) {
                    Button button = new Button(candidatesBar.getContext());
                    candidate.bind(button);
                    candidatesBar.addView(button);
                }
            }
        });
        handWritingView.onCandidateSelected(new HandWritingView.CandidateHandler() {
            @Override
            public void selected(String content) {
                textView.append(content);
                updateButtonState();
                if (handWritingView.getEngine().equals("hanvon-m") || handWritingView.getEngine().equals("lookup")) {
                    candidatesBar.removeAllViews();
                }
            }
        });
        engineSelector.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String newEngine = engines.get(item.getItemId());
                handWritingView.setEngine(newEngine);
                selectButton.setText("Engine: " + newEngine);
                return true;
            }
        });
        if (engines.contains(handWritingView.getEngine())) {
            selectButton.setText("Engine: " + handWritingView.getEngine());
        }
    }

    @OnClick(R.id.undoButton)
    public void undo() {
        handWritingView.undo();
        updateButtonState();
    }

    @OnClick(R.id.redoButton)
    public void redo() {
        handWritingView.redo();
        updateButtonState();
    }

    @OnClick(R.id.clearButton)
    public void clear() {
        handWritingView.reset();
        candidatesBar.removeAllViews();
        updateButtonState();
    }

    @OnClick(R.id.resetButton)
    public void reset() {
        textView.setText("");
        clear();
    }

    @OnClick(R.id.selectButton)
    public void select() {
        engineSelector.show();
    }

    private void updateButtonState() {
        undoButton.setEnabled(handWritingView.canUndo());
        redoButton.setEnabled(handWritingView.canRedo());
    }
}
