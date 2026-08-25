package it.tugaia56.obsidian.utils;

import android.graphics.Color;

public class ColorUtils {

    /**
     * Adjust a color (same algorithm as OC's ColorUtils.adjustColor):
     *  -100..+100  → each RGB channel scaled by (1 + amount/100), matching OC behaviour
     *  1000..1255  → alpha override: alpha = (amount - 1000), RGB kept from color
     */
    public static int adjustColor(int color, int amount) {
        if (amount >= 1000) {
            int alpha = Math.min(255, amount - 1000);
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
        float percent = amount;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        r = (int) Math.min(255, Math.max(0, r + (r * percent / 100)));
        g = (int) Math.min(255, Math.max(0, g + (g * percent / 100)));
        b = (int) Math.min(255, Math.max(0, b + (b * percent / 100)));
        return Color.argb(Color.alpha(color), r, g, b);
    }

    /** Multiply the alpha channel by factor (0..1). */
    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.min(255, (int) (Color.alpha(color) * factor));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /** Darken a color for pressed state: HSV value multiplied by factor. */
    public static int adjustColorForPressed(int baseColor, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(baseColor, hsv);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * factor));
        return Color.HSVToColor(hsv);
    }

    public static String toHex(int color) {
        return String.format("#%08X", color);
    }
}
