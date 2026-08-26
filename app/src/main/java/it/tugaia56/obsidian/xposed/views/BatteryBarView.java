package it.tugaia56.obsidian.xposed.views;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;

import it.tugaia56.obsidian.xposed.hooks.systemui.BatteryDataProvider;

/**
 * Thin bar drawn under (or over) the status bar showing the current battery level.
 * Mirrors OC's BatteryBarView mechanism (mask-width-by-level + colour by breakpoint,
 * charging/fast-charging/power-save tint overrides, colourful gradient mode, charging
 * pulse animation), simplified where Obsidian's UI doesn't need full fidelity (a single
 * alpha-pulse instead of OC's translate-sweep charging animation).
 */
public class BatteryBarView extends FrameLayout implements BatteryDataProvider.BatteryInfoCallback {

    @SuppressLint("StaticFieldLeak")
    private static BatteryBarView sInstance;

    private final View mBar;
    private ValueAnimator mChargeAnim;
    private int mLastColor = Color.WHITE;

    private boolean mOnlyWhileCharging = false;
    private boolean mOnTop = true;
    private boolean mColorful = false;
    private boolean mCentered = false;
    private boolean mTransitColors = false;
    private boolean mAnimateCharging = true;
    private int mOpacity = 100;
    private int mHeightRaw = 50;

    private int mCriticalLevel = 15;
    private int mWarningLevel = 40;
    private int mCriticalColor = Color.RED;
    private int mWarningColor = Color.YELLOW;
    private int mNormalColor = Color.WHITE;

    private boolean mIndicateCharging = true;
    private int mChargingColor = Color.GREEN;
    private boolean mIndicateFastCharging = false;
    private int mFastChargingColor = Color.GREEN;
    private boolean mIndicatePowerSave = false;
    private int mPowerSaveColor = Color.GREEN;

    public static BatteryBarView getInstance(Context context) {
        if (sInstance == null) sInstance = new BatteryBarView(context.getApplicationContext());
        return sInstance;
    }

    public static BatteryBarView getInstance() { return sInstance; }

    public static boolean hasInstance() { return sInstance != null; }

    /** Last colour applied to the bar (colourful mode returns white — no single colour). */
    public int getLastColor() { return mLastColor; }

    private BatteryBarView(Context context) {
        super(context);
        mBar = new View(context);
        addView(mBar, new FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT));
        BatteryDataProvider.registerInfoCallback(this);
    }

    public void setOnlyWhileCharging(boolean v) { mOnlyWhileCharging = v; }
    public void setOnTop(boolean v) { mOnTop = v; }
    public void setColorful(boolean v) { mColorful = v; }
    public void setCentered(boolean v) { mCentered = v; }
    public void setTransitColors(boolean v) { mTransitColors = v; }
    public void setAnimateCharging(boolean v) { mAnimateCharging = v; }
    public void setOpacityPct(int v) { mOpacity = v; setAlpha(v / 100f); }
    public void setHeightRaw(int v) { mHeightRaw = v; }
    public void setLevelColors(int criticalLevel, int warningLevel, int criticalColor, int warningColor) {
        mCriticalLevel = criticalLevel; mWarningLevel = warningLevel;
        mCriticalColor = criticalColor; mWarningColor = warningColor;
    }
    public void setStateColors(boolean indicateCharging, int chargingColor,
                                boolean indicateFastCharging, int fastChargingColor,
                                boolean indicatePowerSave, int powerSaveColor) {
        mIndicateCharging = indicateCharging; mChargingColor = chargingColor;
        mIndicateFastCharging = indicateFastCharging; mFastChargingColor = fastChargingColor;
        mIndicatePowerSave = indicatePowerSave; mPowerSaveColor = powerSaveColor;
    }

    public void refreshLayout() {
        int heightPx = Math.round((Math.round(mHeightRaw / 10f) + 5) * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, heightPx);
        lp.gravity = mOnTop ? Gravity.TOP : Gravity.BOTTOM;
        setLayoutParams(lp);
        setAlpha(mOpacity / 100f);
        refreshVisibility();
        refreshBar();
    }

    private void refreshVisibility() {
        boolean visible = !(mOnlyWhileCharging && !BatteryDataProvider.isCharging());
        setVisibility(visible ? VISIBLE : GONE);
        updateChargeAnimation();
    }

    private void refreshBar() {
        int level = BatteryDataProvider.getCurrentLevel();
        boolean charging = BatteryDataProvider.isCharging();

        if (mColorful) {
            mBar.setBackground(null);
            LinearGradient gradient = new LinearGradient(0, 0, Math.max(getWidth(), 1), 0,
                    new int[]{Color.RED, Color.YELLOW, Color.GREEN}, null, Shader.TileMode.CLAMP);
            android.graphics.drawable.PaintDrawable pd = new android.graphics.drawable.PaintDrawable();
            pd.getPaint().setShader(gradient);
            mBar.setBackground(pd);
        } else {
            int color = resolveColor(level, charging);
            mLastColor = color;
            mBar.setBackground(null);
            mBar.setBackgroundColor(color);
        }

        post(() -> {
            int totalWidth = getWidth();
            if (totalWidth <= 0) return;
            FrameLayout.LayoutParams barLp = (FrameLayout.LayoutParams) mBar.getLayoutParams();
            barLp.width = Math.round(totalWidth * (level / 100f));
            barLp.gravity = (mCentered ? Gravity.CENTER_HORIZONTAL : Gravity.START) | Gravity.CENTER_VERTICAL;
            mBar.setLayoutParams(barLp);
        });

        updateChargeAnimation();
    }

    private int resolveColor(int level, boolean charging) {
        boolean fastCharging = BatteryDataProvider.isCharging() && charging; // no separate fast-charge signal yet
        if (mIndicateFastCharging && fastCharging) return mFastChargingColor;
        if (mIndicateCharging && charging) return mChargingColor;
        if (mIndicatePowerSave && BatteryDataProvider.isPowerSaving()) return mPowerSaveColor;

        if (!mTransitColors) {
            if (level <= mCriticalLevel) return mCriticalColor;
            if (level <= mWarningLevel) return mWarningColor;
            return mNormalColor;
        }
        if (level <= mCriticalLevel) return mCriticalColor;
        if (level <= mWarningLevel) {
            float frac = (level - mCriticalLevel) / (float) Math.max(1, mWarningLevel - mCriticalLevel);
            return ColorUtils.blendARGB(mCriticalColor, mWarningColor, frac);
        }
        float frac = Math.min(1f, (level - mWarningLevel) / (float) Math.max(1, 100 - mWarningLevel));
        return ColorUtils.blendARGB(mWarningColor, mNormalColor, frac);
    }

    private void updateChargeAnimation() {
        boolean shouldAnimate = mAnimateCharging && BatteryDataProvider.isCharging()
                && getVisibility() == VISIBLE;
        if (shouldAnimate && mChargeAnim == null) {
            mChargeAnim = ValueAnimator.ofFloat(1f, 0.4f);
            mChargeAnim.setDuration(1000);
            mChargeAnim.setRepeatMode(ValueAnimator.REVERSE);
            mChargeAnim.setRepeatCount(ValueAnimator.INFINITE);
            mChargeAnim.addUpdateListener(a -> mBar.setAlpha((float) a.getAnimatedValue()));
            mChargeAnim.start();
        } else if (!shouldAnimate && mChargeAnim != null) {
            mChargeAnim.cancel();
            mChargeAnim = null;
            mBar.setAlpha(1f);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        refreshBar();
    }

    @Override
    public void onBatteryInfoChanged() {
        post(() -> {
            refreshVisibility();
            refreshBar();
        });
    }
}
