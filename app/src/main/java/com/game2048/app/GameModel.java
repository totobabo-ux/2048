package com.game2048.app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameModel {

    public static final int DIRECTION_UP    = 0;
    public static final int DIRECTION_DOWN  = 1;
    public static final int DIRECTION_LEFT  = 2;
    public static final int DIRECTION_RIGHT = 3;

    private static final int MAX_UNDO_STEPS = 10;

    public static class TileMove {
        public final int fromRow, fromCol, toRow, toCol;
        public final boolean isMerge;
        public TileMove(int fr, int fc, int tr, int tc, boolean merge) {
            fromRow = fr; fromCol = fc; toRow = tr; toCol = tc; isMerge = merge;
        }
    }

    private static class UndoState {
        final int[][] board;
        final int score;
        UndoState(int[][] board, int score) { this.board = board; this.score = score; }
    }

    private int size;
    private int[][] board;
    private int score;
    private int bestScore;
    private boolean gameOver;
    private boolean won;
    private boolean continueAfterWin;
    private Random random;

    private ArrayDeque<UndoState> undoStack = new ArrayDeque<>();
    private List<TileMove> lastMoves = new ArrayList<>();
    private int newTileRow = -1;
    private int newTileCol = -1;

    public GameModel(int size, int bestScore) {
        this.size = size;
        this.bestScore = bestScore;
        this.random = new Random();
        newGame();
    }

    public void newGame() {
        board = new int[size][size];
        score = 0;
        gameOver = false;
        won = false;
        continueAfterWin = false;
        undoStack = new ArrayDeque<>();
        lastMoves = new ArrayList<>();
        newTileRow = -1;
        newTileCol = -1;
        addRandomTile();
        addRandomTile();
    }

    private void addRandomTile() {
        List<int[]> empty = getEmptyCells();
        if (empty.isEmpty()) return;
        int[] cell = empty.get(random.nextInt(empty.size()));
        board[cell[0]][cell[1]] = random.nextInt(10) < 9 ? 2 : 4;
        newTileRow = cell[0];
        newTileCol = cell[1];
    }

    private List<int[]> getEmptyCells() {
        List<int[]> list = new ArrayList<>();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board[r][c] == 0) list.add(new int[]{r, c});
        return list;
    }

    public boolean move(int direction) {
        savePrevState();
        lastMoves = new ArrayList<>();
        newTileRow = -1;
        newTileCol = -1;
        boolean moved = false;
        switch (direction) {
            case DIRECTION_UP:    moved = moveUp();    break;
            case DIRECTION_DOWN:  moved = moveDown();  break;
            case DIRECTION_LEFT:  moved = moveLeft();  break;
            case DIRECTION_RIGHT: moved = moveRight(); break;
        }
        if (moved) {
            addRandomTile();
            if (score > bestScore) bestScore = score;
            if (!continueAfterWin && hasWon()) won = true;
            if (!canMove()) gameOver = true;
        } else {
            // Move didn't happen — discard the snapshot we just pushed
            undoStack.poll();
        }
        return moved;
    }

    private void savePrevState() {
        int[][] snap = new int[size][size];
        for (int r = 0; r < size; r++) snap[r] = board[r].clone();
        undoStack.push(new UndoState(snap, score));
        // Trim oldest entries beyond the limit
        while (undoStack.size() > MAX_UNDO_STEPS) undoStack.removeLast();
    }

    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        UndoState state = undoStack.pop();
        board = state.board;
        score = state.score;
        gameOver = false;
        won = false;
        lastMoves = new ArrayList<>();
        return true;
    }

    public void shuffle() {
        savePrevState();
        lastMoves = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board[r][c] != 0) values.add(board[r][c]);

        board = new int[size][size];
        for (int val : values) {
            List<int[]> empty = getEmptyCells();
            if (empty.isEmpty()) break;
            int[] cell = empty.get(random.nextInt(empty.size()));
            board[cell[0]][cell[1]] = val;
        }
        gameOver = false;
        if (!canMove()) gameOver = true;
    }

    private boolean moveLeft() {
        boolean moved = false;
        for (int r = 0; r < size; r++) {
            int[] result = mergeLineTracked(board[r], r, false, false);
            if (!arrayEquals(board[r], result)) moved = true;
            board[r] = result;
        }
        return moved;
    }

    private boolean moveRight() {
        boolean moved = false;
        for (int r = 0; r < size; r++) {
            int[] rev = reverse(board[r]);
            int[] result = mergeLineTracked(rev, r, false, true);
            result = reverse(result);
            if (!arrayEquals(board[r], result)) moved = true;
            board[r] = result;
        }
        return moved;
    }

    private boolean moveUp() {
        boolean moved = false;
        for (int c = 0; c < size; c++) {
            int[] col = getColumn(c);
            int[] result = mergeLineTracked(col, c, true, false);
            if (!arrayEquals(col, result)) moved = true;
            setColumn(c, result);
        }
        return moved;
    }

    private boolean moveDown() {
        boolean moved = false;
        for (int c = 0; c < size; c++) {
            int[] col = getColumn(c);
            int[] rev = reverse(col);
            int[] result = mergeLineTracked(rev, c, true, true);
            result = reverse(result);
            if (!arrayEquals(col, result)) moved = true;
            setColumn(c, result);
        }
        return moved;
    }

    private int[] mergeLineTracked(int[] line, int lineIdx, boolean isColumn, boolean reversed) {
        int[] vals = new int[size];
        int[] origPos = new int[size];
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (line[i] != 0) {
                vals[count] = line[i];
                origPos[count] = reversed ? (size - 1 - i) : i;
                count++;
            }
        }

        int[] merged = new int[size];
        int[] primarySrc = new int[size];
        int[] secondarySrc = new int[size];
        for (int i = 0; i < size; i++) { primarySrc[i] = -1; secondarySrc[i] = -1; }

        int mp = 0;
        boolean justMerged = false;
        for (int i = 0; i < count; i++) {
            if (!justMerged && mp > 0 && merged[mp - 1] == vals[i]) {
                merged[mp - 1] *= 2;
                score += merged[mp - 1];
                secondarySrc[mp - 1] = origPos[i];
                justMerged = true;
            } else {
                merged[mp] = vals[i];
                primarySrc[mp] = origPos[i];
                mp++;
                justMerged = false;
            }
        }

        for (int destSlot = 0; destSlot < size; destSlot++) {
            if (merged[destSlot] == 0) continue;
            int destPos = reversed ? (size - 1 - destSlot) : destSlot;

            int src = primarySrc[destSlot];
            if (src >= 0 && src != destPos) {
                int fr = isColumn ? src : lineIdx;
                int fc = isColumn ? lineIdx : src;
                int tr = isColumn ? destPos : lineIdx;
                int tc = isColumn ? lineIdx : destPos;
                lastMoves.add(new TileMove(fr, fc, tr, tc, false));
            }

            int src2 = secondarySrc[destSlot];
            if (src2 >= 0) {
                int fr = isColumn ? src2 : lineIdx;
                int fc = isColumn ? lineIdx : src2;
                int tr = isColumn ? destPos : lineIdx;
                int tc = isColumn ? lineIdx : destPos;
                lastMoves.add(new TileMove(fr, fc, tr, tc, true));
            }
        }

        return merged;
    }

    private int[] getColumn(int c) {
        int[] col = new int[size];
        for (int r = 0; r < size; r++) col[r] = board[r][c];
        return col;
    }

    private void setColumn(int c, int[] col) {
        for (int r = 0; r < size; r++) board[r][c] = col[r];
    }

    private int[] reverse(int[] arr) {
        int[] r = new int[arr.length];
        for (int i = 0; i < arr.length; i++) r[i] = arr[arr.length - 1 - i];
        return r;
    }

    private boolean arrayEquals(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private boolean canMove() {
        if (!getEmptyCells().isEmpty()) return true;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int v = board[r][c];
                if (r + 1 < size && board[r + 1][c] == v) return true;
                if (c + 1 < size && board[r][c + 1] == v) return true;
            }
        }
        return false;
    }

    private boolean hasWon() {
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (board[r][c] == 2048) return true;
        return false;
    }

    public void continueGame() {
        continueAfterWin = true;
        won = false;
    }

    // ── Persistence helpers ──────────────────────────────────────────────────

    public int[] flatBoard() {
        int[] flat = new int[size * size];
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                flat[r * size + c] = board[r][c];
        return flat;
    }

    // Serialise undo stack as "score:v0,v1,...|score:v0,v1,..." (oldest → newest).
    public String serializeUndoStack() {
        if (undoStack.isEmpty()) return "";
        Object[] arr = undoStack.toArray(); // arr[0]=newest, arr[last]=oldest
        StringBuilder sb = new StringBuilder();
        for (int i = arr.length - 1; i >= 0; i--) {
            UndoState s = (UndoState) arr[i];
            if (i < arr.length - 1) sb.append('|');
            sb.append(s.score).append(':');
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (r > 0 || c > 0) sb.append(',');
                    sb.append(s.board[r][c]);
                }
            }
        }
        return sb.toString();
    }

    // Deserialise and rebuild undo stack. Call after restoreState().
    public void deserializeUndoStack(String data) {
        undoStack = new ArrayDeque<>();
        if (data == null || data.isEmpty()) return;
        String[] states = data.split("\\|");
        for (String entry : states) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            int s = Integer.parseInt(entry.substring(0, colon));
            String[] parts = entry.substring(colon + 1).split(",");
            int[][] b = new int[size][size];
            int idx = 0;
            for (int r = 0; r < size; r++)
                for (int c = 0; c < size; c++)
                    if (idx < parts.length) b[r][c] = Integer.parseInt(parts[idx++]);
            undoStack.push(new UndoState(b, s)); // push = newest at front
        }
    }

    public void restoreState(int[] flatBoard, int score,
                             boolean gameOver, boolean won, boolean continueAfterWin) {
        this.score = score;
        this.gameOver = gameOver;
        this.won = won;
        this.continueAfterWin = continueAfterWin;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                board[r][c] = flatBoard[r * size + c];
        undoStack = new ArrayDeque<>();
        lastMoves = new ArrayList<>();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getSize()                 { return size; }
    public int[][] getBoard()            { return board; }
    public int getScore()                { return score; }
    public int getBestScore()            { return bestScore; }
    public boolean isGameOver()          { return gameOver; }
    public boolean isWon()               { return won; }
    public boolean isContinueAfterWin()  { return continueAfterWin; }
    public boolean canUndo()             { return !undoStack.isEmpty(); }
    public int undoCount()               { return undoStack.size(); }
    public List<TileMove> getLastMoves() { return lastMoves; }
    public int getNewTileRow()           { return newTileRow; }
    public int getNewTileCol()           { return newTileCol; }
}
