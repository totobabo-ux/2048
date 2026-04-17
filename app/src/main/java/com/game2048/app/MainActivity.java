package com.game2048.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private static final int[] SIZES = {4, 5, 6, 7, 8};
    private int currentSizeIndex = 0;

    private BoardPreviewView boardPreviewView;
    private TextView tvDifficulty;
    private ImageButton btnPrev, btnNext;
    private MaterialButton btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boardPreviewView = findViewById(R.id.boardPreviewView);
        tvDifficulty     = findViewById(R.id.tvDifficulty);
        btnPrev          = findViewById(R.id.btnPrevDifficulty);
        btnNext          = findViewById(R.id.btnNextDifficulty);
        btnStart         = findViewById(R.id.btnStart);

        // Restore last selected size
        SharedPreferences prefs = getSharedPreferences("game2048", MODE_PRIVATE);
        currentSizeIndex = prefs.getInt("sizeIndex", 0);
        updateDisplay();

        btnPrev.setOnClickListener(v -> {
            currentSizeIndex = (currentSizeIndex - 1 + SIZES.length) % SIZES.length;
            updateDisplay();
            animateArrow(v, -1);
        });

        btnNext.setOnClickListener(v -> {
            currentSizeIndex = (currentSizeIndex + 1) % SIZES.length;
            updateDisplay();
            animateArrow(v, 1);
        });

        btnStart.setOnClickListener(v -> {
            prefs.edit().putInt("sizeIndex", currentSizeIndex).apply();
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("gridSize", SIZES[currentSizeIndex]);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void updateDisplay() {
        int size = SIZES[currentSizeIndex];
        tvDifficulty.setText(size + "x" + size);
        boardPreviewView.setGridSize(size);
    }

    private void animateArrow(View v, int dir) {
        v.animate()
                .translationX(dir * 12f)
                .setDuration(80)
                .withEndAction(() -> v.animate().translationX(0).setDuration(80).start())
                .start();
    }
}
