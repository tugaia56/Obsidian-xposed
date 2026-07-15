package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Status bar clock customisation:
 *  – Position: left / center / right         (pref "status_bar_clock", values 2/1/0)
 *  – Font size: 12–20 sp                     (pref "status_bar_clock_size", int)
 *  – Extra padding: 0–16 dp                  (pref "status_bar_clock_padding", int)
 *  – Show seconds                            (pref "status_bar_clock_seconds", boolean)
 *  – AM/PM style                             (pref "status_bar_am_pm", "0"=norm/"1"=small/"2"=hidden)
 *  – Date display: off / small / normal      (pref "status_bar_clock_date_display", "0/1/2")
 *  – Date position: before / after           (pref "status_bar_clock_date_position", "0/1")
 *  – Custom text before clock                (pref "sbc_before_clock_format", String)
 *  – Before text small (70%)                 (pref "sbc_before_small", boolean)
 *  – Custom text after clock                 (pref "sbc_after_clock_format", String)
 *  – After text small (70%)                  (pref "sbc_after_small", boolean)
 *  – Auto-hide when launcher visible         (pref "status_bar_clock_auto_hide_launcher", boolean)
 *  – Auto-hide on interval                   (pref "status_bar_clock_auto_hide", boolean)
 *  – Custom color                            (pref "status_bar_custom_clock_color" bool +
 *                                                  "status_bar_clock_color" int)
 */
public class StatusbarClock extends XposedMods {

    // Clock position
    private static final int POS_RIGHT  = 0;
    private static final int POS_CENTER = 1;
    private static final int POS_LEFT   = 2;

    // Date display
    private static final int DATE_GONE  = 0;
    private static final int DATE_SMALL = 1;

    // Date position
    private static final int DATE_BEFORE = 0;

    // AM/PM style
    private static final int AM_PM_NORMAL = 0;
    private static final int AM_PM_SMALL  = 1;
    private static final int AM_PM_GONE   = 2;

    // Auto-hide defaults (seconds)
    private static final int DEFAULT_HIDE_DURATION = 60;
    private static final int DEFAULT_SHOW_DURATION = 5;

    // ── Prefs ──────────────────────────────────────────────────────────────────

    private int     mPosition    = POS_LEFT;
    private int     mSize        = 12;
    private int     mPadding     = 0;
    private boolean mCustomColor = false;
    private int     mColor       = Color.WHITE;

    private boolean mShowSeconds = false;
    private int     mAmPmStyle   = AM_PM_GONE;

    private int     mDateDisplay = DATE_GONE;
    private int     mDatePos     = DATE_BEFORE;

    private String  mBeforeClock = "";
    private boolean mBeforeSmall = false;
    private String  mAfterClock  = "";
    private boolean mAfterSmall  = false;

    private boolean mAutoHideLauncher = false;
    private boolean mAutoHide         = false;
    private int     mHideDuration     = DEFAULT_HIDE_DURATION;
    private int     mShowDuration     = DEFAULT_SHOW_DURATION;

    // Computed before/after (resolved from date + custom text prefs)
    private String  mComputedBefore      = "";
    private boolean mComputedBeforeSmall = false;
    private String  mComputedAfter       = "";
    private boolean mComputedAfterSmall  = false;

    // ── Views ──────────────────────────────────────────────────────────────────

    private TextView     mClockView      = null;
    private Object       mSbFragment     = null;
    private ViewGroup    mStartSide      = null;
    private View         mCenteredArea   = null;
    private LinearLayout mSystemIconArea = null;
    private int          mStartPadding   = 0;

    // ── Auto-hide ──────────────────────────────────────────────────────────────

    private final Handler autoHideHandler  = new Handler(Looper.getMainLooper());
    private boolean       mScreenOn        = true;
    private boolean       mLauncherVisible = false;

    // Initialised in constructor to avoid illegal forward reference
    private Runnable mHideRunnable;
    private Runnable mShowRunnable;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mClockView == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenOn = false;
                autoHideHandler.removeCallbacks(mHideRunnable);
                autoHideHandler.removeCallbacks(mShowRunnable);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mScreenOn = true;
                updateAutoHide();
            }
        }
    };

    // ── Constructor ────────────────────────────────────────────────────────────

    public StatusbarClock(Context context) {
        super(context);

        // Runnables must be assigned here (not as field initializers) to avoid
        // illegal forward references between mHideRunnable and mShowRunnable.
        mHideRunnable = () -> {
            if (mClockView != null) mClockView.setVisibility(View.INVISIBLE);
            autoHideHandler.postDelayed(mShowRunnable, mHideDuration * 1000L);
        };
        mShowRunnable = () -> {
            if (mClockView != null) mClockView.setVisibility(View.VISIBLE);
            if (mAutoHide) autoHideHandler.postDelayed(mHideRunnable, mShowDuration * 1000L);
        };

        try {
            int id = mContext.getResources().getIdentifier(
                    "status_bar_clock_starting_padding", "dimen", mContext.getPackageName());
            if (id != 0)
                mStartPadding = mContext.getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {}
    }

    // ── XposedMods ─────────────────────────────────────────────────────────────

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;

        mPosition    = Integer.parseInt(Xprefs.getString("status_bar_clock", String.valueOf(POS_LEFT)));
        mSize        = Xprefs.getSliderInt("status_bar_clock_size",    12);
        mPadding     = Xprefs.getSliderInt("status_bar_clock_padding", 0);
        mCustomColor = Xprefs.getBoolean("status_bar_custom_clock_color", false);
        mColor       = Xprefs.getInt("status_bar_clock_color", Color.WHITE);

        mShowSeconds = Xprefs.getBoolean("status_bar_clock_seconds", false);
        mAmPmStyle   = Integer.parseInt(Xprefs.getString("status_bar_am_pm", String.valueOf(AM_PM_GONE)));

        mDateDisplay = Integer.parseInt(Xprefs.getString("status_bar_clock_date_display", "0"));
        mDatePos     = Integer.parseInt(Xprefs.getString("status_bar_clock_date_position", "0"));

        mBeforeClock = Xprefs.getString("sbc_before_clock_format", "");
        mBeforeSmall = Xprefs.getBoolean("sbc_before_small", false);
        mAfterClock  = Xprefs.getString("sbc_after_clock_format", "");
        mAfterSmall  = Xprefs.getBoolean("sbc_after_small", false);

        mAutoHideLauncher = Xprefs.getBoolean("status_bar_clock_auto_hide_launcher", false);
        mAutoHide         = Xprefs.getBoolean("status_bar_clock_auto_hide", false);
        mHideDuration     = Xprefs.getSliderInt("status_bar_clock_auto_hide_hduration", DEFAULT_HIDE_DURATION);
        mShowDuration     = Xprefs.getSliderInt("status_bar_clock_auto_hide_sduration", DEFAULT_SHOW_DURATION);

        buildComputedTexts();

        if (Key.length > 0) {
            switch (Key[0]) {
                case "status_bar_clock":
                    placeClock();
                    break;
                case "status_bar_clock_size":
                case "status_bar_clock_padding":
                    setClockSize();
                    break;
                case "status_bar_clock_auto_hide":
                case "status_bar_clock_auto_hide_launcher":
                    updateAutoHide();
                    break;
                default:
                    refreshClock();
                    break;
            }
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!SYSTEM_UI.equals(lp.packageName)) return;

        // Register screen on/off receiver
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiver(mScreenReceiver, filter);
        } catch (Throwable ignored) {}

        // ── 1. CollapsedStatusBarFragment: grab views ─────────────────────────

        try {
            Class<?> CollapsedSBF = findClass(
                    "com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment",
                    lp.classLoader);

            hookAllMethods(CollapsedSBF, "onViewCreated", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    mSbFragment = p.thisObject;

                    try {
                        mClockView = (TextView) getObjectField(p.thisObject, "mClockView");
                    } catch (Throwable t) {
                        log("[ Obsidian ] StatusbarClock: mClockView not found: " + t);
                        return;
                    }

                    ViewGroup mStatusBar = null;
                    try {
                        mStatusBar = (ViewGroup) getObjectField(mSbFragment, "mStatusBar");
                    } catch (Throwable ignored) {}

                    if (mStatusBar != null) {
                        int sideId = mContext.getResources().getIdentifier(
                                "status_bar_start_side_except_heads_up", "id",
                                mContext.getPackageName());
                        if (sideId == 0) sideId = mContext.getResources().getIdentifier(
                                "status_bar_left_side", "id", mContext.getPackageName());
                        if (sideId != 0)
                            mStartSide = mStatusBar.findViewById(sideId);

                        int iconId = mContext.getResources().getIdentifier(
                                "statusIcons", "id", mContext.getPackageName());
                        if (iconId == 0) iconId = mContext.getResources().getIdentifier(
                                "system_icon_area", "id", mContext.getPackageName());
                        if (iconId != 0)
                            mSystemIconArea = mStatusBar.findViewById(iconId);
                    }

                    try {
                        mCenteredArea = (View) ((View) getObjectField(
                                p.thisObject, "mCenteredIconArea")).getParent();
                    } catch (Throwable ignored) {
                        if (mStatusBar != null) {
                            LinearLayout center = new LinearLayout(mContext);
                            FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT);
                            lp2.gravity = Gravity.CENTER;
                            center.setLayoutParams(lp2);
                            mStatusBar.addView(center);
                            mCenteredArea = center;
                        }
                    }

                    placeClock();
                    setClockSize();
                    refreshClock();
                    updateAutoHide();
                }
            });
        } catch (Throwable t) {
            log("[ Obsidian ] StatusbarClock: CollapsedStatusBarFragment hook failed: " + t);
        }

        // ── 2. Clock.updateClockVisibility: apply custom text color ───────────

        try {
            Class<?> ClockClass = findClass(
                    "com.android.systemui.statusbar.policy.Clock", lp.classLoader);

            hookAllMethods(ClockClass, "updateClockVisibility", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.thisObject != mClockView || !mCustomColor || mClockView == null) return;
                    mClockView.post(() -> mClockView.setTextColor(mColor));
                }
            });
        } catch (Throwable t) {
            log("[ Obsidian ] StatusbarClock: updateClockVisibility hook failed: " + t);
        }

        // ── 3a. Clock.getSmallTime BEFORE: toggle seconds field ───────────────

        try {
            Class<?> ClockClass = findClass(
                    "com.android.systemui.statusbar.policy.Clock", lp.classLoader);

            hookAllMethods(ClockClass, "getSmallTime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.thisObject != mClockView) return;
                    try {
                        setObjectField(p.thisObject, "mShowSeconds", mShowSeconds);
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable t) {
            log("[ Obsidian ] StatusbarClock: getSmallTime before-hook failed: " + t);
        }

        // ── 3b. Clock.getSmallTime AFTER: text size, before/after, color ──────

        try {
            Class<?> ClockClass = findClass(
                    "com.android.systemui.statusbar.policy.Clock", lp.classLoader);

            hookAllMethods(ClockClass, "getSmallTime", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (p.thisObject != mClockView) return;

                    TextView tv = (TextView) p.thisObject;
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mSize);

                    CharSequence original = (CharSequence) p.getResult();
                    if (original == null) return;

                    boolean hasBefore = !mComputedBefore.isEmpty();
                    boolean hasAfter  = !mComputedAfter.isEmpty();
                    boolean hasAmPm   = (mAmPmStyle != AM_PM_GONE);
                    if (!mCustomColor && !hasBefore && !hasAfter && !hasAmPm) return;

                    SpannableStringBuilder result = new SpannableStringBuilder();

                    // Before text
                    if (hasBefore) {
                        result.append(buildSpan(formatText(mComputedBefore), mComputedBeforeSmall));
                        result.append(" ");
                    }

                    // Clock text
                    SpannableStringBuilder clockSpan = SpannableStringBuilder.valueOf(original);
                    if (mCustomColor) {
                        clockSpan.setSpan(new ForegroundColorSpan(mColor), 0, clockSpan.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    result.append(clockSpan);

                    // AM/PM
                    if (hasAmPm) {
                        String ampm = new SimpleDateFormat("a", Locale.getDefault()).format(new Date());
                        SpannableStringBuilder ampmSpan = new SpannableStringBuilder(ampm);
                        if (mAmPmStyle == AM_PM_SMALL) {
                            ampmSpan.setSpan(new RelativeSizeSpan(0.75f), 0, ampmSpan.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        if (mCustomColor) {
                            ampmSpan.setSpan(new ForegroundColorSpan(mColor), 0, ampmSpan.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        result.append(ampmSpan);
                    }

                    // After text
                    if (hasAfter) {
                        result.append(" ");
                        result.append(buildSpan(formatText(mComputedAfter), mComputedAfterSmall));
                    }

                    p.setResult(result);
                }
            });
        } catch (Throwable t) {
            log("[ Obsidian ] StatusbarClock: getSmallTime after-hook failed: " + t);
        }

        // ── 4. StatClock.updateMinWidth (OOS-specific) ────────────────────────

        try {
            Class<?> StatClock = findClass(
                    "com.oplus.systemui.statusbar.widget.StatClock", lp.classLoader);

            hookAllMethods(StatClock, "updateMinWidth", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    ((TextView) p.thisObject).setTextSize(TypedValue.COMPLEX_UNIT_SP, mSize);
                }
            });
        } catch (Throwable ignored) {}

        // ── 5. Auto-hide launcher: TaskStackListenerImpl ──────────────────────

        try {
            Class<?> TaskStackListenerImpl = findClass(
                    "com.android.wm.shell.common.TaskStackListenerImpl", lp.classLoader);

            hookAllMethods(TaskStackListenerImpl, "onTaskMovedToFront", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (!mAutoHideLauncher || mClockView == null) return;
                    try {
                        Object taskInfo  = p.args[0];
                        Intent baseIntent = (Intent) callMethod(taskInfo, "getBaseIntent");
                        if (baseIntent == null) return;
                        String pkg = baseIntent.getPackage();
                        if (pkg == null && baseIntent.getComponent() != null)
                            pkg = baseIntent.getComponent().getPackageName();
                        if (pkg == null) return;

                        final boolean launcher = isLauncherPackage(pkg);
                        mLauncherVisible = launcher;
                        final int vis = launcher ? View.INVISIBLE : View.VISIBLE;
                        mClockView.post(() -> mClockView.setVisibility(vis));
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Resolve computed before/after texts.
     * Custom text overrides date display when non-empty.
     */
    private void buildComputedTexts() {
        boolean hasCustom = !mBeforeClock.isEmpty() || !mAfterClock.isEmpty();
        if (hasCustom) {
            mComputedBefore      = mBeforeClock;
            mComputedBeforeSmall = mBeforeSmall;
            mComputedAfter       = mAfterClock;
            mComputedAfterSmall  = mAfterSmall;
        } else if (mDateDisplay != DATE_GONE) {
            boolean small = (mDateDisplay == DATE_SMALL);
            String fmt = "EEE, d MMM";
            if (mDatePos == DATE_BEFORE) {
                mComputedBefore      = fmt;
                mComputedBeforeSmall = small;
                mComputedAfter       = "";
                mComputedAfterSmall  = false;
            } else {
                mComputedBefore      = "";
                mComputedBeforeSmall = false;
                mComputedAfter       = fmt;
                mComputedAfterSmall  = small;
            }
        } else {
            mComputedBefore = ""; mComputedBeforeSmall = false;
            mComputedAfter  = ""; mComputedAfterSmall  = false;
        }
    }

    /** Build a spannable applying optional relative-size and color spans. */
    private SpannableStringBuilder buildSpan(String text, boolean small) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        int len = sb.length();
        if (small)       sb.setSpan(new RelativeSizeSpan(0.75f),            0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (mCustomColor) sb.setSpan(new ForegroundColorSpan(mColor),       0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    /**
     * Format text: try as SimpleDateFormat pattern; fall back to literal.
     */
    private String formatText(String pattern) {
        if (pattern == null || pattern.isEmpty()) return "";
        try {
            return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
        } catch (Throwable ignored) {
            return pattern;
        }
    }

    /** Check whether a package is the default launcher. */
    private boolean isLauncherPackage(String pkg) {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> launchers = mContext.getPackageManager()
                    .queryIntentActivities(homeIntent, 0);
            for (ResolveInfo ri : launchers) {
                if (pkg.equals(ri.activityInfo.packageName)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /** Start / stop interval auto-hide. */
    private void updateAutoHide() {
        autoHideHandler.removeCallbacks(mHideRunnable);
        autoHideHandler.removeCallbacks(mShowRunnable);
        if (mClockView == null) return;
        if (mAutoHide && mScreenOn) {
            autoHideHandler.postDelayed(mHideRunnable, mShowDuration * 1000L);
        } else if (!mAutoHide && !mAutoHideLauncher) {
            mClockView.setVisibility(View.VISIBLE);
        }
    }

    /** Move the clock to the requested position area. */
    @SuppressLint("RtlHardcoded")
    private void placeClock() {
        if (mClockView == null) return;
        ViewGroup parent = (ViewGroup) mClockView.getParent();
        ViewGroup target = null;
        Integer   index  = null;
        int extraPx = dp2px(mPadding);

        switch (mPosition) {
            case POS_LEFT:
                target = mStartSide;
                index  = 1;
                mClockView.setPadding(mStartPadding, 0, mStartPadding + extraPx, 0);
                break;
            case POS_CENTER:
                target = (mCenteredArea instanceof ViewGroup) ? (ViewGroup) mCenteredArea : null;
                mClockView.setPadding(mStartPadding, 0, mStartPadding + extraPx, 0);
                break;
            case POS_RIGHT:
                if (mSystemIconArea != null)
                    target = (ViewGroup) mSystemIconArea.getParent();
                mClockView.setPadding(mStartPadding, 0, extraPx, 0);
                break;
        }

        if (target != null && parent != null) {
            parent.removeView(mClockView);
            if (index != null) target.addView(mClockView, index);
            else                target.addView(mClockView);
        }
    }

    /** Apply font size and trigger layout. */
    private void setClockSize() {
        if (mClockView == null) return;
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mSize);
        if (mSize > 12) {
            ViewGroup.LayoutParams p = mClockView.getLayoutParams();
            if (p != null) {
                p.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mClockView.setLayoutParams(p);
            }
        }
        mClockView.post(mClockView::requestLayout);
        refreshClock();
    }

    /** Force the clock to redraw. */
    private void refreshClock() {
        if (mClockView == null) return;
        mClockView.post(() -> {
            try {
                Object calendar = getObjectField(mClockView, "mCalendar");
                callMethod(calendar, "setTimeInMillis", System.currentTimeMillis());
                callMethod(mClockView, "updateClock");
            } catch (Throwable ignored) {}
            if (mCustomColor) mClockView.setTextColor(mColor);
        });
    }

    private int dp2px(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                mContext.getResources().getDisplayMetrics()));
    }

    @Override
    public boolean listensTo(String packageName) { return SYSTEM_UI.equals(packageName); }
}
