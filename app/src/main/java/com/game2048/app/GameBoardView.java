package com.game2048.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.view.GestureDetectorCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameBoardView extends View {

    public interface OnMoveListener {
        void onMove(int direction);
    }

    private GameModel model;
    private Paint boardPaint, cellPaint, textPaint;
    private float cellSize, gap, cornerRadius;
    private OnMoveListener moveListener;
    private GestureDetectorCompat gestureDetector;

    // Animation state
    private boolean animating = false;
    private float slideProgress = 1f;
    private float popProgress = 1f;
    private int[][] prevBoardSnapshot;
    private List<GameModel.TileMove> slideMoves = new ArrayList<>();
    private Set<Long> movingSourceKeys = new HashSet<>();
    private Set<Long> mergeDestKeys = new HashSet<>();
    private int newTileRow = -1, newTileCol = -1;

    public GameBoardView(Context context) {
        super(context);
        init();
    }

    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameBoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boardPaint.setColor(getContext().getColor(R.color.board_bg));

        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        gestureDetector = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 80;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();
                float absDX = Math.abs(dX);
                float absDY = Math.abs(dY);

                if (absDX > absDY) {
                    if (absDX > SWIPE_THRESHOLD && Math.abs(vX) > SWIPE_VELOCITY_THRESHOLD) {
                        triggerMove(dX > 0 ? GameModel.DIRECTION_RIGHT : GameModel.DIRECTION_LEFT);
                        return true;
                    }
                } else {
                    if (absDY > SWIPE_THRESHOLD && Math.abs(vY) > SWIPE_VELOCITY_THRESHOLD) {
                        triggerMove(dY > 0 ? GameModel.DIRECTION_DOWN : GameModel.DIRECTION_UP);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    private void triggerMove(int direction) {
        if (moveListener == null || animating) return;

        // Snapshot current board before the move
        int size = model.getSize();
        int[][] curr = model.getBoard();
        prevBoardSnapshot = new int[size][size];
        for (int r = 0; r < size; r++) prevBoardSnapshot[r] = curr[r].clone();

        animating = true;
        slideProgress = 0f;

        // Trigger move — GameActivity updates score and calls invalidate() here
        moveListener.onMove(direction);

        // Collect animation data from model after move
        List<GameModel.TileMove> moves = model.getLastMoves();
        if (moves.isEmpty()) {
            animating = false;
            slideProgress = 1f;
            return;
        }

        slideMoves = new ArrayList<>(moves);
        movingSourceKeys = new HashSet<>();
        mergeDestKeys = new HashSet<>();
        for (GameModel.TileMove m : slideMoves) {
            movingSourceKeys.add(encodePos(m.fromRow, m.fromCol));
            if (m.isMerge) mergeDestKeys.add(encodePos(m.toRow, m.toCol));
        }
        newTileRow = model.getNewTileRow();
        newTileCol = model.getNewTileCol();
        popProgress = 0f;

        ValueAnimator slideAnim = ValueAnimator.ofFloat(0f, 1f);
        slideAnim.setDuration(120);
        slideAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        slideAnim.addUpdateListener(a -> {
            slideProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        slideAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                slideProgress = 1f;
                startPopAnimation();
            }
        });
        slideAnim.start();
    }

    private void startPopAnimation() {
        ValueAnimator popAnim = ValueAnimator.ofFloat(0f, 1f);
        popAnim.setDuration(160);
        popAnim.addUpdateListener(a -> {
            popProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        popAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                popProgress = 1f;
                animating = false;
                invalidate();
            }
        });
        popAnim.start();
    }

    public void setModel(GameModel model) {
        this.model = model;
        animating = false;
        slideProgress = 1f;
        popProgress = 1f;
        slideMoves = new ArrayList<>();
        movingSourceKeys = new HashSet<>();
        mergeDestKeys = new HashSet<>();
        invalidate();
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.moveListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        recalcDimensions(w, h);
    }

    private void recalcDimensions(int w, int h) {
        if (model == null) return;
        int size = model.getSize();
        float side = Math.min(w, h);
        gap = side * 0.03f;
        cellSize = (side - gap * (size + 1)) / size;
        cornerRadius = cellSize * 0.08f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (model == null) return;

        int size = model.getSize();
        float side = Math.min(getWidth(), getHeight());
        float boardLeft = (getWidth() - side) / 2f;
        float boardTop  = (getHeight() - side) / 2f;

        // Board background
        RectF boardRect = new RectF(boardLeft, boardTop, boardLeft + side, boardTop + side);
        boardPaint.setColor(getContext().getColor(R.color.board_bg));
        canvas.drawRoundRect(boardRect, cornerRadius * 1.5f, cornerRadius * 1.5f, boardPaint);

        // Empty cell slots
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                drawCell(canvas, 0, cellLeft(boardLeft, c), cellTop(boardTop, r), 1f);

        if (!animating) {
            drawBoardTiles(canvas, model.getBoard(), boardLeft, boardTop, -1, -1, 1f);
        } else if (slideProgress < 1f) {
            drawSlidePhase(canvas, size, boardLeft, boardTop);
        } else {
            drawPopPhase(canvas, size, boardLeft, boardTop);
        }
    }

    private void drawBoardTiles(Canvas canvas, int[][] board, float boardLeft, float boardTop,
                                int popR, int popC, float popScale) {
        int size = board.length;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int v = board[r][c];
                if (v == 0) continue;
                float scale = (r == popR && c == popC) ? popScale : 1f;
                drawCell(canvas, v, cellLeft(boardLeft, c), cellTop(boardTop, r), scale);
            }
        }
    }

    private void drawSlidePhase(Canvas canvas, int size, float boardLeft, float boardTop) {
        // Static tiles (not moving)
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (prevBoardSnapshot[r][c] == 0) continue;
                if (movingSourceKeys.contains(encodePos(r, c))) continue;
                drawCell(canvas, prevBoardSnapshot[r][c], cellLeft(boardLeft, c), cellTop(boardTop, r), 1f);
            }
        }
        // Animated tiles sliding toward destination
        for (GameModel.TileMove m : slideMoves) {
            int v = prevBoardSnapshot[m.fromRow][m.fromCol];
            float curLeft = lerp(cellLeft(boardLeft, m.fromCol), cellLeft(boardLeft, m.toCol), slideProgress);
            float curTop  = lerp(cellTop(boardTop,  m.fromRow), cellTop(boardTop,  m.toRow),  slideProgress);
            drawCell(canvas, v, curLeft, curTop, 1f);
        }
    }

    private void drawPopPhase(Canvas canvas, int size, float boardLeft, float boardTop) {
        float ps = computePopScale(popProgress);
        int[][] board = model.getBoard();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int v = board[r][c];
                if (v == 0) continue;
                boolean special = mergeDestKeys.contains(encodePos(r, c))
                        || (r == newTileRow && c == newTileCol);
                drawCell(canvas, v, cellLeft(boardLeft, c), cellTop(boardTop, r), special ? ps : 1f);
            }
        }
    }

    // Pop scale: 0 → 1.2 → 1.0
    private float computePopScale(float t) {
        if (t < 0.65f) return t / 0.65f * 1.2f;
        return 1.2f - (t - 0.65f) / 0.35f * 0.2f;
    }

    private float cellLeft(float boardLeft, int col) {
        return boardLeft + gap + col * (cellSize + gap);
    }

    private float cellTop(float boardTop, int row) {
        return boardTop + gap + row * (cellSize + gap);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static long encodePos(int r, int c) {
        return r * 100L + c;
    }

    private void drawCell(Canvas canvas, int value, float left, float top, float scale) {
        float cx = left + cellSize / 2f;
        float cy = top  + cellSize / 2f;
        float hs = cellSize / 2f * scale;

        RectF rect = new RectF(cx - hs, cy - hs, cx + hs, cy + hs);
        cellPaint.setColor(TileColors.getTileBackground(getContext(), value));
        canvas.drawRoundRect(rect, cornerRadius * scale, cornerRadius * scale, cellPaint);

        if (value != 0 && scale > 0.05f) {
            textPaint.setColor(TileColors.getTileTextColor(getContext(), value));
            textPaint.setTextSize(TileColors.getTileTextSize(value, cellSize) * scale);
            canvas.drawText(String.valueOf(value), cx,
                    cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    public void drawPreview(Canvas canvas, int size, int[][] sampleBoard, float left, float top, float boardSize) {
        float g = boardSize * 0.03f;
        float cs = (boardSize - g * (size + 1)) / size;
        float cr = cs * 0.08f;

        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setColor(getContext().getColor(R.color.board_bg));
        RectF boardRect = new RectF(left, top, left + boardSize, top + boardSize);
        canvas.drawRoundRect(boardRect, cr * 1.5f, cr * 1.5f, bp);

        Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tp.setTextAlign(Paint.Align.CENTER);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int value = (sampleBoard != null && r < sampleBoard.length && c < sampleBoard[r].length)
                        ? sampleBoard[r][c] : 0;
                float cl = left + g + c * (cs + g);
                float ct = top  + g + r * (cs + g);
                RectF cellRect = new RectF(cl, ct, cl + cs, ct + cs);
                cp.setColor(TileColors.getTileBackground(getContext(), value));
                canvas.drawRoundRect(cellRect, cr, cr, cp);

                if (value != 0) {
                    tp.setColor(TileColors.getTileTextColor(getContext(), value));
                    tp.setTextSize(TileColors.getTileTextSize(value, cs));
                    canvas.drawText(String.valueOf(value),
                            cl + cs / 2f,
                            ct + cs / 2f - (tp.descent() + tp.ascent()) / 2f,
                            tp);
                }
            }
        }
    }
}
