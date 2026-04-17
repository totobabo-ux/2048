package com.game2048.app;

import android.content.Context;
import android.graphics.Color;

public class TileColors {

    public static int getTileBackground(Context ctx, int value) {
        switch (value) {
            case 0:    return ctx.getColor(R.color.cell_empty);
            case 2:    return ctx.getColor(R.color.cell_2);
            case 4:    return ctx.getColor(R.color.cell_4);
            case 8:    return ctx.getColor(R.color.cell_8);
            case 16:   return ctx.getColor(R.color.cell_16);
            case 32:   return ctx.getColor(R.color.cell_32);
            case 64:   return ctx.getColor(R.color.cell_64);
            case 128:  return ctx.getColor(R.color.cell_128);
            case 256:  return ctx.getColor(R.color.cell_256);
            case 512:  return ctx.getColor(R.color.cell_512);
            case 1024: return ctx.getColor(R.color.cell_1024);
            case 2048: return ctx.getColor(R.color.cell_2048);
            default:   return ctx.getColor(R.color.cell_super);
        }
    }

    public static int getTileTextColor(Context ctx, int value) {
        if (value <= 4) return ctx.getColor(R.color.text_dark);
        return ctx.getColor(R.color.text_light);
    }

    public static float getTileTextSize(int value, float cellSize) {
        float base;
        if (value < 100)       base = 0.40f;
        else if (value < 1000) base = 0.33f;
        else if (value < 10000)base = 0.27f;
        else                   base = 0.22f;
        return cellSize * base;
    }
}
