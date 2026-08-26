package it.tugaia56.obsidian.xposed.utils;

import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;

/**
 * "Chip di sfondo" riusabile (pieno/contorno/misto + colore/bordo/angoli/margine/padding) —
 * stessa logica reale di StatusbarClock.updateChip() (la prima istanza di questo feature,
 * lasciata invariata), estratta qui cosi altri hook (es. Intestazione QS: chip orologio +
 * chip data) possono riusarla senza duplicare lettura prefs e disegno per ogni istanza.
 * Le chiavi prefs seguono sempre lo stesso schema: "&lt;prefix&gt;_switch/_style/..." — vedi
 * {@code ClockChipStyleFragment}/{@code QsHeaderChipStyleFragment} per l'editor UI.
 */
public class ChipStyleHelper {

    private ChipStyleHelper() {}

    public static void apply(View view, Context context, String prefix) {
        if (view == null || Xprefs == null) return;

        boolean on = Xprefs.getBoolean(prefix + "_switch", false);
        if (!on) {
            view.setBackground(null);
            view.setPadding(0, 0, 0, 0);
            applyMargins(view, context, 0, 0, 0, 0);
            return;
        }

        int style             = parseIntSafe(Xprefs.getString(prefix + "_style", "0"), 0);
        boolean fillAccent    = Xprefs.getBoolean(prefix + "_fill_accent", true);
        int fillColorPref     = Xprefs.getInt(prefix + "_color", Color.WHITE);
        boolean strokeAccent  = Xprefs.getBoolean(prefix + "_stroke_accent", true);
        int strokeColorPref   = Xprefs.getInt(prefix + "_stroke_color", Color.WHITE);
        int strokeWidthDp     = Xprefs.getInt(prefix + "_stroke_width", 2);
        boolean roundCorners  = Xprefs.getBoolean(prefix + "_round_corners", false);
        int cornerDp          = Xprefs.getInt(prefix + "_corner", 14);
        int marginTop         = Xprefs.getInt(prefix + "_margin_top", 0);
        int marginLeft        = Xprefs.getInt(prefix + "_margin_left", 0);
        int marginRight       = Xprefs.getInt(prefix + "_margin_right", 0);
        int marginBottom      = Xprefs.getInt(prefix + "_margin_bottom", 0);
        int padTop            = Xprefs.getInt(prefix + "_padding_top", 0);
        int padLeft           = Xprefs.getInt(prefix + "_padding_left", 0);
        int padRight          = Xprefs.getInt(prefix + "_padding_right", 0);
        int padBottom         = Xprefs.getInt(prefix + "_padding_bottom", 0);

        int accent = systemAccentColor(context);
        int fillColor   = fillAccent   ? accent : fillColorPref;
        int strokeColor = strokeAccent ? accent : strokeColorPref;

        int fill = Color.TRANSPARENT;
        int strokeWidthPx = 0;
        switch (style) {
            case 1 -> strokeWidthPx = dp2px(context, strokeWidthDp); // contorno
            case 2 -> { fill = fillColor; strokeWidthPx = dp2px(context, strokeWidthDp); } // misto
            default -> fill = fillColor; // pieno
        }
        float corner = roundCorners ? dp2px(context, cornerDp) : 0f;
        view.setBackground(tintBlockedChip(fill, strokeColor, strokeWidthPx, corner));
        view.setPadding(dp2px(context, padLeft), dp2px(context, padTop),
                dp2px(context, padRight), dp2px(context, padBottom));
        applyMargins(view, context, marginTop, marginLeft, marginRight, marginBottom);
    }

    private static void applyMargins(View view, Context context, int topDp, int leftDp, int rightDp, int bottomDp) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams mlp)) return;
        mlp.setMargins(dp2px(context, leftDp), dp2px(context, topDp),
                dp2px(context, rightDp), dp2px(context, bottomDp));
        view.setLayoutParams(mlp);
    }

    private static int systemAccentColor(Context context) {
        try { return context.getColor(android.R.color.system_accent1_600); }
        catch (Throwable t) { return Color.WHITE; }
    }

    /** GradientDrawable che blocca il tint/colorFilter di sistema di OOS (come in
     *  DstDialogStyle), altrimenti OOS sovrascrive il colore scelto dall'utente. */
    private static GradientDrawable tintBlockedChip(int fill, int strokeColor, int strokeWidth,
                                                      float cornerRadius) {
        GradientDrawable d = new GradientDrawable() {
            @Override public void setTint(int tintColor) { /* block OOS tint */ }
            @Override public void setTintList(ColorStateList tint) { /* block OOS tint */ }
            @Override public void setTintMode(PorterDuff.Mode tintMode) { /* block */ }
            @Override public void setColorFilter(ColorFilter cf) { /* block OOS colorFilter */ }
            @Override public void setColorFilter(int color, PorterDuff.Mode mode) { /* block */ }
        };
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setCornerRadius(cornerRadius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    private static int dp2px(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Throwable t) { return def; }
    }
}
