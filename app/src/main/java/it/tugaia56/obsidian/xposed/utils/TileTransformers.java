package it.tugaia56.obsidian.xposed.utils;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;

/**
 * Porting di OC's it.dhd.oxygencustomizer.xposed.utils.viewpager.* (18 PageTransformer).
 * Interfaccia LOCALE (non androidx.viewpager.widget.ViewPager.PageTransformer) apposta:
 * il chiamante (QsTilesCustomizeMod) itera i figli e calcola transformPos a mano via
 * reflection, come faceva già OC — non serve mai passare questi oggetti dentro una vera
 * ViewPager, quindi zero dipendenza nuova nel build.
 *
 * Indice = posizione nell'array qs_tile_transitions_entries (0-based, NON i case 1-18 di
 * OC che erano 1-based) — get(int) sotto fa la mappatura giusta una volta sola qui.
 */
public final class TileTransformers {

    public interface Transformer {
        void transformPage(View page, float position);
    }

    public static abstract class Base implements Transformer {
        protected abstract void onTransform(View view, float position);

        @Override public final void transformPage(View page, float position) {
            onPreTransform(page, position);
            onTransform(page, position);
        }

        protected boolean hideOffscreenPages() { return true; }
        protected boolean isPagingEnabled() { return false; }

        protected void onPreTransform(View page, float position) {
            float width = page.getWidth();
            page.setRotationX(0f);
            page.setRotationY(0f);
            page.setRotation(0f);
            page.setScaleX(1f);
            page.setScaleY(1f);
            page.setPivotX(0f);
            page.setPivotY(0f);
            page.setTranslationY(0f);
            page.setTranslationX(isPagingEnabled() ? 0f : (-width) * position);
            if (hideOffscreenPages()) {
                page.setAlpha((position > -1f && position < 1f) ? 1f : 0f);
            } else {
                page.setAlpha(1f);
            }
        }

        protected static float min(float val, float min) { return Math.max(val, min); }
    }

    public static final class CubeInTransformer extends Base {
        protected void onTransform(View v, float p) {
            v.setPivotX(p > 0f ? 0 : v.getWidth());
            v.setPivotY(0f);
            v.setRotationY(-90f * p);
        }
        @Override protected boolean isPagingEnabled() { return true; }
    }

    public static final class CubeOutTransformer extends Base {
        protected void onTransform(View v, float p) {
            v.setPivotX(p < 0f ? v.getWidth() : 0f);
            v.setPivotY(v.getHeight() * 0.5f);
            v.setRotationY(90f * p);
        }
        @Override protected boolean isPagingEnabled() { return true; }
    }

    public static final class AccordionTransformer extends Base {
        protected void onTransform(View v, float p) {
            v.setPivotX(p < 0f ? 0 : v.getWidth());
            v.setScaleX(p < 0f ? 1f + p : 1f - p);
        }
    }

    public static final class BackgroundToForegroundTransformer extends Base {
        protected void onTransform(View v, float p) {
            float f = p >= 0f ? Math.abs(1f - p) : 1f;
            float scale = min(f, 0.5f);
            v.setScaleX(scale); v.setScaleY(scale);
            v.setPivotX(v.getWidth() * 0.5f); v.setPivotY(v.getHeight() * 0.5f);
            v.setTranslationX(p < 0f ? v.getWidth() * p : (-v.getWidth()) * p * 0.25f);
        }
    }

    public static final class DepthPageTransformer extends Base {
        private static final float MIN_SCALE = 0.75f;
        protected void onTransform(View v, float p) {
            if (p <= 0f) {
                v.setTranslationX(0f); v.setScaleX(1f); v.setScaleY(1f);
            } else if (p <= 1f) {
                float scale = MIN_SCALE + (0.25f * (1f - Math.abs(p)));
                v.setAlpha(1f - p);
                v.setPivotY(0.5f * v.getHeight());
                v.setTranslationX(v.getWidth() * (-p));
                v.setScaleX(scale); v.setScaleY(scale);
            }
        }
        @Override protected boolean isPagingEnabled() { return true; }
    }

    public static final class FadeTransformer extends Base {
        public void onTransform(View v, float p) {
            v.setPivotX(p < 0f ? v.getWidth() : 0f);
            v.setPivotY(v.getHeight() * 0.5f);
            v.setRotationY(20f * p);
            float norm = Math.abs(Math.abs(p) - 1f);
            float scale = (norm / 2f) + 0.5f;
            v.setScaleX(scale); v.setScaleY(scale);
        }
        @Override protected boolean isPagingEnabled() { return true; }
    }

    public static final class ForegroundToBackgroundTransformer extends Base {
        protected void onTransform(View v, float p) {
            float f = p <= 0f ? Math.abs(1f + p) : 1f;
            float scale = min(f, 0.5f);
            v.setScaleX(scale); v.setScaleY(scale);
            v.setPivotX(v.getWidth() * 0.5f); v.setPivotY(v.getHeight() * 0.5f);
            v.setTranslationX(p > 0f ? v.getWidth() * p : (-v.getWidth()) * p * 0.25f);
        }
    }

    public static final class RotateDownTransformer extends Base {
        private static final float ROT_MOD = -15f;
        protected void onTransform(View v, float p) {
            v.setPivotX(0.5f * v.getWidth());
            v.setPivotY(v.getHeight());
            v.setRotation(ROT_MOD * p * -1.25f);
        }
        @Override protected boolean isPagingEnabled() { return true; }
    }

    public static final class RotateUpTransformer extends Base {
        private static final float ROT_MOD = -15f;
        protected void onTransform(View v, float p) {
            v.setPivotX(0.5f * v.getWidth());
            v.setPivotY(0f);
            v.setTranslationX(0f);
            v.setRotation(ROT_MOD * p);
        }
        @Override protected boolean isPagingEnabled() { return true; }
    }

    public static final class StackTransformer extends Base {
        protected void onTransform(View v, float p) {
            v.setTranslationX(p >= 0f ? (-v.getWidth()) * p : 0f);
        }
    }

    public static final class TabletTransformer extends Base {
        private static final Camera OFFSET_CAMERA = new Camera();
        private static final Matrix OFFSET_MATRIX = new Matrix();
        private static final float[] OFFSET_TEMP = new float[2];

        protected void onTransform(View v, float p) {
            float rotation = (p < 0f ? 30f : -30f) * Math.abs(p);
            v.setTranslationX(offsetXForRotation(rotation, v.getWidth(), v.getHeight()));
            v.setPivotX(v.getWidth() * 0.5f);
            v.setPivotY(0f);
            v.setRotationY(rotation);
        }

        private static float offsetXForRotation(float degrees, int width, int height) {
            OFFSET_MATRIX.reset();
            OFFSET_CAMERA.save();
            OFFSET_CAMERA.rotateY(Math.abs(degrees));
            OFFSET_CAMERA.getMatrix(OFFSET_MATRIX);
            OFFSET_CAMERA.restore();
            OFFSET_MATRIX.preTranslate(-width * 0.5f, -height * 0.5f);
            OFFSET_MATRIX.postTranslate(width * 0.5f, height * 0.5f);
            OFFSET_TEMP[0] = width; OFFSET_TEMP[1] = height;
            OFFSET_MATRIX.mapPoints(OFFSET_TEMP);
            return (degrees > 0f ? 1f : -1f) * (width - OFFSET_TEMP[0]);
        }
    }

    public static final class ZoomInTransformer extends Base {
        protected void onTransform(View v, float p) {
            float scale = p < 0f ? p + 1f : Math.abs(1f - p);
            v.setScaleX(scale); v.setScaleY(scale);
            v.setPivotX(v.getWidth() * 0.5f); v.setPivotY(v.getHeight() * 0.5f);
            v.setAlpha((p >= -1f && p <= 1f) ? 1f - (scale - 1f) : 0f);
        }
    }

    public static final class ZoomOutTransformer extends Base {
        protected void onTransform(View v, float p) {
            float scale = 1f + Math.abs(p);
            v.setScaleX(scale); v.setScaleY(scale);
            v.setPivotX(v.getWidth() * 0.5f); v.setPivotY(v.getHeight() * 0.5f);
            v.setAlpha((p < -1f || p > 1f) ? 0f : 1f - (scale - 1f));
            if (p == -1f) v.setTranslationX(v.getWidth() * -1f);
        }
    }

    public static final class ZoomOutSlideTransformer extends Base {
        private static final float MIN_ALPHA = 0.5f;
        private static final float MIN_SCALE = 0.85f;
        protected void onTransform(View v, float p) {
            if (p < -1f || p > 1f) return;
            float height = v.getHeight();
            float scale = Math.max(MIN_SCALE, 1f - Math.abs(p));
            float vertMargin = ((1f - scale) * height) / 2f;
            float horzMargin = (v.getWidth() * (1f - scale)) / 2f;
            v.setPivotY(MIN_ALPHA * height);
            v.setTranslationX(p < 0f ? horzMargin - (vertMargin / 2f) : (-horzMargin) + (vertMargin / 2f));
            v.setScaleX(scale); v.setScaleY(scale);
            v.setAlpha((((scale - MIN_SCALE) / 0.14999998f) * MIN_ALPHA) + MIN_ALPHA);
        }
    }

    public static final class RaiseFromCenterTransformer extends Base {
        protected void onTransform(View v, float p) {
            int width = v.getWidth(), height = v.getHeight();
            if (p < -1f || p > 1f) { v.setTranslationX(0f); return; }
            float scale = p < 0f ? 1f + p : 1f - p;
            v.setPivotX(width / 2f); v.setPivotY(height / 2f);
            v.setScaleX(scale); v.setScaleY(scale);
            v.setTranslationX(-p * width);
        }
    }

    public static final class RotateAboutBottomTransformer extends Base {
        protected void onTransform(View v, float p) {
            v.setPivotX(v.getWidth() / 2f);
            v.setPivotY(v.getHeight());
            v.setRotation(p * 20f);
        }
    }

    public static final class TranslationYTransformer extends Base {
        public static final int TOP_TO_BOTTOM = 1, BOTTOM_TO_TOP = 2;
        private final int type;
        public TranslationYTransformer(int type) { this.type = type; }

        protected void onTransform(View v, float p) {
            int height = v.getHeight();
            if (p <= 0f || p > 1f) { v.setTranslationX(0f); return; }
            if (type == TOP_TO_BOTTOM) v.setTranslationY(-p * height);
            else if (type == BOTTOM_TO_TOP) v.setTranslationY(p * height);
        }
    }

    /** Indice 0-based = posizione in R.array.qs_tile_transitions_entries. */
    public static Transformer get(int index) {
        return switch (index) {
            case 0 -> new CubeInTransformer();
            case 1 -> new CubeOutTransformer();
            case 2 -> new AccordionTransformer();
            case 3 -> new BackgroundToForegroundTransformer();
            case 4 -> new DepthPageTransformer();
            case 5 -> new FadeTransformer();
            case 6 -> new ForegroundToBackgroundTransformer();
            case 7 -> new RotateDownTransformer();
            case 8 -> new RotateUpTransformer();
            case 9 -> new StackTransformer();
            case 10 -> new TabletTransformer();
            case 11 -> new ZoomInTransformer();
            case 12 -> new ZoomOutTransformer();
            case 13 -> new ZoomOutSlideTransformer();
            case 14 -> new RaiseFromCenterTransformer();
            case 15 -> new RotateAboutBottomTransformer();
            case 16 -> new TranslationYTransformer(TranslationYTransformer.TOP_TO_BOTTOM);
            case 17 -> new TranslationYTransformer(TranslationYTransformer.BOTTOM_TO_TOP);
            default -> null;
        };
    }

    private TileTransformers() {}
}
