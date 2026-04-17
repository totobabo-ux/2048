package com.game2048.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class BoardPreviewView extends View {

    private int gridSize = 4;
    private int[][] sampleBoard;
    private Paint boardPaint, cellPaint, textPaint;

    private static final int[][][] SAMPLES = {
        {{16,8,4,4},{32,2,2,0},{8,16,0,0},{4,0,0,0}},
        {{32,16,8,4,2},{16,8,4,2,0},{8,4,2,0,0},{4,2,0,0,0},{2,0,0,0,0}},
        {{64,32,16,8,4,2},{32,16,8,4,2,0},{16,8,4,2,0,0},{8,4,2,0,0,0},{4,2,0,0,0,0},{2,0,0,0,0,0}},
        {{128,64,32,16,8,4,2},{64,32,16,8,4,2,0},{32,16,8,4,2,0,0},{16,8,4,2,0,0,0},{8,4,2,0,0,0,0},{4,2,0,0,0,0,0},{2,0,0,0,0,0,0}},
        {{256,128,64,32,16,8,4,2},{128,64,32,16,8,4,2,0},{64,32,16,8,4,2,0,0},{32,16,8,4,2,0,0,0},{16,8,4,2,0,0,0,0},{8,4,2,0,0,0,0,0},{4,2,0,0,0,0,0,0},{2,0,0,0,0,0,0,0}}
    };

    public BoardPreviewView(Context context) { super(context); init(); }
    public BoardPreviewView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public BoardPreviewView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        setGridSize(4);
    }

    public void setGridSize(int size) {
        this.gridSize = size;
        int idx = size - 4;
        sampleBoard = (idx >= 0 && idx < SAMPLES.length) ? SAMPLES[idx] : new int[size][size];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float side = Math.min(w, h) * 0.92f;
        float left = (w - side) / 2f, top = (h - side) / 2f;
        float gap = side * 0.03f;
        float cs = (side - gap * (gridSize + 1)) / gridSize;
        float cr = cs * 0.10f;

        boardPaint.setColor(getContext().getColor(R.color.board_bg));
        canvas.drawRoundRect(new RectF(left, top, left+side, top+side), cr*1.5f, cr*1.5f, boardPaint);

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                int value = (sampleBoard!=null && r<sampleBoard.length && c<sampleBoard[r].length) ? sampleBoard[r][c] : 0;
                float cl = left + gap + c*(cs+gap);
                float ct = top  + gap + r*(cs+gap);
                cellPaint.setColor(TileColors.getTileBackground(getContext(), value));
                canvas.drawRoundRect(new RectF(cl, ct, cl+cs, ct+cs), cr, cr, cellPaint);
                if (value != 0) {
                    textPaint.setColor(TileColors.getTileTextColor(getContext(), value));
                    textPaint.setTextSize(TileColors.getTileTextSize(value, cs));
                    canvas.drawText(String.valueOf(value), cl+cs/2f, ct+cs/2f-(textPaint.descent()+textPaint.ascent())/2f, textPaint);
                }
            }
        }
    }
}
