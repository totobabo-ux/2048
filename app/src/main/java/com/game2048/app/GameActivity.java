package com.game2048.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.button.MaterialButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_GRID_SIZE = "gridSize";

    // Bundle / SharedPreferences keys
    private static final String KEY_BOARD        = "board";
    private static final String KEY_UNDO_STACK   = "undoStack";
    private static final String KEY_SCORE        = "score";
    private static final String KEY_GAME_OVER    = "gameOver";
    private static final String KEY_WON          = "won";
    private static final String KEY_CONTINUE     = "continueAfterWin";
    private static final String KEY_HAS_SAVED    = "hasSavedGame";

    private GameBoardView gameBoardView;
    private TextView tvScore, tvBestScore, tvGameOverText;
    private ImageButton btnBack, btnUndo, btnShuffle;
    private FrameLayout gameOverOverlay;
    private MaterialButton btnContinue, btnNewGame;

    private GameModel model;
    private int gridSize = 4;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        prefs = getSharedPreferences("game2048", MODE_PRIVATE);
        gridSize = getIntent().getIntExtra("gridSize", 4);

        gameBoardView   = findViewById(R.id.gameBoardView);
        tvScore         = findViewById(R.id.tvScore);
        tvBestScore     = findViewById(R.id.tvBestScore);
        btnBack         = findViewById(R.id.btnBack);
        btnUndo         = findViewById(R.id.btnUndo);
        btnShuffle      = findViewById(R.id.btnShuffle);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);
        tvGameOverText  = findViewById(R.id.tvGameOverText);
        btnContinue     = findViewById(R.id.btnContinue);
        btnNewGame      = findViewById(R.id.btnNewGame);

        int bestScore = prefs.getInt("bestScore_" + gridSize, 0);
        model = new GameModel(gridSize, bestScore);

        // Restore from Bundle first (configuration change), then SharedPreferences (process death)
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_BOARD)) {
            restoreFromBundle(savedInstanceState);
        } else if (prefs.getBoolean(KEY_HAS_SAVED + "_" + gridSize, false)) {
            restoreFromPrefs();
        }

        gameBoardView.setModel(model);

        gameBoardView.setOnMoveListener(direction -> {
            if (model.isGameOver() || model.isWon()) return;
            boolean moved = model.move(direction);
            if (moved) {
                updateUI();
                saveBestScore();
                checkGameState();
            }
        });

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnUndo.setOnClickListener(v -> {
            if (model.undo()) {
                hideOverlay();
                updateUI();
                animateButton(v);
            }
        });

        btnShuffle.setOnClickListener(v -> {
            model.shuffle();
            hideOverlay();
            updateUI();
            gameBoardView.invalidate();
            animateButton(v);
        });

        btnNewGame.setOnClickListener(v -> startNewGame());

        btnContinue.setOnClickListener(v -> {
            model.continueGame();
            hideOverlay();
        });

        updateUI();
        checkGameState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveToBundle(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveToPrefs();
    }

    // --- State save helpers ---

    private void saveToBundle(Bundle b) {
        b.putIntArray(KEY_BOARD, model.flatBoard());
        b.putString(KEY_UNDO_STACK, model.serializeUndoStack());
        b.putInt(KEY_SCORE, model.getScore());
        b.putBoolean(KEY_GAME_OVER, model.isGameOver());
        b.putBoolean(KEY_WON, model.isWon());
        b.putBoolean(KEY_CONTINUE, model.isContinueAfterWin());
    }

    private void saveToPrefs() {
        String suffix = "_" + gridSize;
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean(KEY_HAS_SAVED + suffix, true);
        e.putString(KEY_BOARD + suffix, flatToString(model.flatBoard()));
        e.putString(KEY_UNDO_STACK + suffix, model.serializeUndoStack());
        e.putInt(KEY_SCORE + suffix, model.getScore());
        e.putBoolean(KEY_GAME_OVER + suffix, model.isGameOver());
        e.putBoolean(KEY_WON + suffix, model.isWon());
        e.putBoolean(KEY_CONTINUE + suffix, model.isContinueAfterWin());
        e.apply();
    }

    private void clearSavedPrefs() {
        prefs.edit().putBoolean(KEY_HAS_SAVED + "_" + gridSize, false).apply();
    }

    // --- State restore helpers ---

    private void restoreFromBundle(Bundle b) {
        int[] flat = b.getIntArray(KEY_BOARD);
        model.restoreState(flat, b.getInt(KEY_SCORE),
                b.getBoolean(KEY_GAME_OVER), b.getBoolean(KEY_WON), b.getBoolean(KEY_CONTINUE));
        model.deserializeUndoStack(b.getString(KEY_UNDO_STACK, ""));
    }

    private void restoreFromPrefs() {
        String suffix = "_" + gridSize;
        String boardStr = prefs.getString(KEY_BOARD + suffix, null);
        if (boardStr == null) return;
        model.restoreState(stringToFlat(boardStr), prefs.getInt(KEY_SCORE + suffix, 0),
                prefs.getBoolean(KEY_GAME_OVER + suffix, false),
                prefs.getBoolean(KEY_WON + suffix, false),
                prefs.getBoolean(KEY_CONTINUE + suffix, false));
        model.deserializeUndoStack(prefs.getString(KEY_UNDO_STACK + suffix, ""));
    }

    // --- Serialization ---

    private static String flatToString(int[] flat) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < flat.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(flat[i]);
        }
        return sb.toString();
    }

    private static int[] stringToFlat(String s) {
        String[] parts = s.split(",");
        int[] flat = new int[parts.length];
        for (int i = 0; i < parts.length; i++) flat[i] = Integer.parseInt(parts[i]);
        return flat;
    }

    // --- Game logic ---

    private void startNewGame() {
        int best = prefs.getInt("bestScore_" + gridSize, 0);
        model = new GameModel(gridSize, best);
        gameBoardView.setModel(model);
        clearSavedPrefs();
        hideOverlay();
        updateUI();
    }

    private void updateUI() {
        tvScore.setText(String.valueOf(model.getScore()));
        tvBestScore.setText(String.valueOf(model.getBestScore()));
        gameBoardView.invalidate();
    }

    private void saveBestScore() {
        prefs.edit().putInt("bestScore_" + gridSize, model.getBestScore()).apply();
    }

    private void checkGameState() {
        if (model.isWon()) {
            showOverlay(true);
        } else if (model.isGameOver()) {
            showOverlay(false);
        }
    }

    private void showOverlay(boolean won) {
        tvGameOverText.setText(won ? getString(R.string.you_win) : getString(R.string.game_over));
        btnContinue.setVisibility(won ? View.VISIBLE : View.GONE);
        gameOverOverlay.setVisibility(View.VISIBLE);
        gameOverOverlay.setAlpha(0f);
        gameOverOverlay.animate().alpha(1f).setDuration(300).start();
    }

    private void hideOverlay() {
        gameOverOverlay.setVisibility(View.GONE);
    }

    private void animateButton(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.85f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.85f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
