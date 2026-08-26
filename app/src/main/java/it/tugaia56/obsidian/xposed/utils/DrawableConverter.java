package it.tugaia56.obsidian.xposed.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.Nullable;

/**
 * Bitmap filter helpers for the lockscreen album cover feature — trimmed port of
 * OC's DrawableConverter (only the pieces the album-art filters actually use).
 */
public class DrawableConverter {

    private DrawableConverter() {}

    @Nullable
    public static Bitmap drawableToBitmap(@Nullable Drawable drawable) {
        if (drawable == null) return null;
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();

        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(Math.max(w, 1), Math.max(h, 1), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width = bmpOriginal.getWidth();
        int height = bmpOriginal.getHeight();
        try {
            bmpOriginal = RGB565toARGB888(bmpOriginal);
        } catch (Exception ignored) {}

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(bmpOriginal, new Rect(0, 0, width, height), new Rect(0, 0, width, height), paint);
        return bmpGrayscale;
    }

    /** Tints the artwork with {@code color} (used by the "Colore Accento" filter). */
    public static Bitmap getColoredBitmap(Drawable d, int color) {
        if (d == null) return null;
        Bitmap colorBitmap = ((BitmapDrawable) d).getBitmap();
        Bitmap grayscaleBitmap = toGrayscale(colorBitmap);
        Paint pp = new Paint();
        pp.setAntiAlias(true);
        pp.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        Canvas cc = new Canvas(grayscaleBitmap);
        Rect rect = new Rect(0, 0, grayscaleBitmap.getWidth(), grayscaleBitmap.getHeight());
        cc.drawBitmap(grayscaleBitmap, rect, rect, pp);
        return grayscaleBitmap;
    }

    public static Bitmap getGrayscaleBlurredImage(Context context, Bitmap image, float radius) {
        return toGrayscale(getBlurredImage(context, image, radius));
    }

    public static Bitmap getBlurredImage(Context context, Bitmap image, float radius) {
        try {
            image = RGB565toARGB888(image);
        } catch (Exception ignored) {}

        Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript renderScript = RenderScript.create(context);
        Allocation blurInput = Allocation.createFromBitmap(renderScript, image);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(Math.min(radius, 25f)); // must be 0 < r <= 25
        blur.forEach(blurOutput);
        blurOutput.copyTo(bitmap);
        renderScript.destroy();

        return bitmap;
    }

    private static Bitmap RGB565toARGB888(Bitmap img) {
        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }
}
