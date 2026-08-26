package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Method-level hook companion for DstNotifStyle.
 *
 * Problem: OOS applies a system tint (and on API ≥ 35 a blur/glass effect) to the
 * notification background AFTER our XResources drawable replacement. This hook
 * undoes that tint so the user's chosen colour shows through.
 *
 * Approach mirrors OC's NotificationTransparency + NotificationVanillaIceCream:
 *  • API < 35  → clear tint on ActivatableNotificationView + NotificationBackgroundView
 *  • API ≥ 35  → also null-out ViewBlurManager calls to disable OOS glass blur
 */
public class DstNotifStyleMod extends XposedMods {

    private static final String PKG_SYSTEMUI = "com.android.systemui";

    public DstNotifStyleMod(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        // No Xprefs needed — DstNotifStyle.isEnabled() reads the preloaded static.
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("[ Obsidian ] DstNotifStyleMod: SDK=" + Build.VERSION.SDK_INT
                + " preset=" + DstNotifStyle.isEnabled());
        // Always install A14 tint hooks (safe on all SDK levels via try/catch).
        hookTintRemoval(lpparam);
        // On Android 15+ also disable OOS blur engine.
        if (Build.VERSION.SDK_INT >= 35) {
            hookBlurRemoval(lpparam);
        }
    }

    // ── A14+ tint removal ─────────────────────────────────────────────────────

    private void hookTintRemoval(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> ActivatableNotifView = lpparam.classLoader.loadClass(
                    "com.android.systemui.statusbar.notification.row.ActivatableNotificationView");
            Class<?> NotifBgView = lpparam.classLoader.loadClass(
                    "com.android.systemui.statusbar.notification.row.NotificationBackgroundView");

            // After OOS sets the tint on the notification row, clear it.
            XC_MethodHook clearTint = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    XposedBridge.log("[ Obsidian ] DstNotifStyleMod.clearTint fired, enabled=" + DstNotifStyle.isEnabled());
                    if (!DstNotifStyle.isEnabled()) return;
                    try {
                        View bgView = (View) getObjectField(param.thisObject, "mBackgroundNormal");
                        if (bgView == null) return;
                        try {
                            if (param.args.length > 0)
                                setObjectField(param.thisObject, "mCurrentBackgroundTint", param.args[0]);
                        } catch (Throwable ignored) {}
                        try {
                            Object mBg = getObjectField(bgView, "mBackground");
                            if (mBg instanceof Drawable) callMethod(mBg, "clearColorFilter");
                        } catch (Throwable ignored) {}
                        try { callMethod(bgView, "setColorFilter", 0); } catch (Throwable ignored) {}
                        try { setObjectField(bgView, "mTintColor", 0); } catch (Throwable ignored) {}
                        bgView.invalidate();
                    } catch (Throwable t) {
                        XposedBridge.log("[ Obsidian ] DstNotifStyleMod.clearTint error: " + t);
                    }
                }
            };
            hookAllMethods(ActivatableNotifView, "setBackgroundTintColor", clearTint);
            hookAllMethods(ActivatableNotifView, "updateBackgroundTint",   clearTint);

            // Before OOS sets a custom background tint, zero out mTintColor.
            XC_MethodHook zeroTintColor = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!DstNotifStyle.isEnabled()) return;
                    try { setObjectField(param.thisObject, "mTintColor", 0); } catch (Throwable ignored) {}
                }
            };
            try { hookAllMethods(NotifBgView, "setCustomBackground",   zeroTintColor); } catch (Throwable ignored) {}
            try { hookAllMethods(NotifBgView, "setCustomBackground$1", zeroTintColor); } catch (Throwable ignored) {}

            XposedBridge.log("[ Obsidian ] DstNotifStyleMod: tint removal hooks installed");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] DstNotifStyleMod.hookTintRemoval error: " + t);
        }
    }

    // ── A15+ blur/glass removal ───────────────────────────────────────────────

    private void hookBlurRemoval(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> ViewBlurManager = findClassIfExists(
                    "com.oplus.systemui.notification.blur.ViewBlurManager",
                    lpparam.classLoader);
            if (ViewBlurManager == null) {
                XposedBridge.log("[ Obsidian ] DstNotifStyleMod: ViewBlurManager not found");
                return;
            }

            XC_MethodHook nullReturner = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (DstNotifStyle.isEnabled()) param.setResult(null);
                }
            };
            try { hookAllMethods(ViewBlurManager, "updateCardPlatformMixConfig", nullReturner); } catch (Throwable ignored) {}
            try { hookAllMethods(ViewBlurManager, "updateRowsBlur",              nullReturner); } catch (Throwable ignored) {}
            try { hookAllMethods(ViewBlurManager, "applyBlurConfig",             nullReturner); } catch (Throwable ignored) {}
            // requireBlurProxyForView is called during View CONSTRUCTION (CapsuleEarView.<init>).
            // Returning null there causes NPE → SystemUI crash. Skip it; the other hooks suffice.
            // try { hookAllMethods(ViewBlurManager, "requireBlurProxyForView", nullReturner); } catch (Throwable ignored) {}
            try { hookAllMethods(ViewBlurManager, "headsupCardMotionMixConfig",  nullReturner); } catch (Throwable ignored) {}
            try { hookAllMethods(ViewBlurManager, "blurForHeadsUp",              nullReturner); } catch (Throwable ignored) {}
            try { hookAllMethods(ViewBlurManager, "cancelBlurForHeadsUp",        nullReturner); } catch (Throwable ignored) {}

            // OOS 15/16: OplusNotificationChildrenContainerOverlayExImpl.flushBackground
            // draws the grouped notification background using blur — bypass it with our drawable.
            hookFlushBackground(lpparam);

            XposedBridge.log("[ Obsidian ] DstNotifStyleMod: blur removal hooks installed");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] DstNotifStyleMod.hookBlurRemoval error: " + t);
        }
    }

    // ── OOS 15/16: flushBackground bypass ────────────────────────────────────

    private void hookFlushBackground(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> OplusNotifContainer = findClassIfExists(
                    "com.oplus.systemui.statusbar.notification.stack.OplusNotificationChildrenContainerOverlayExImpl",
                    lpparam.classLoader);
            if (OplusNotifContainer == null) {
                XposedBridge.log("[ Obsidian ] DstNotifStyleMod: OplusNotifChildrenContainerOverlayExImpl not found");
                return;
            }
            hookAllMethods(OplusNotifContainer, "flushBackground", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!DstNotifStyle.isEnabled()) return;
                    try {
                        View view = (View) param.args[0];
                        Object expandableRow = param.args.length > 1 ? param.args[1] : null;
                        if (view == null) return;
                        Object notifBgView = expandableRow != null
                                ? getObjectField(expandableRow, "mBackgroundNormal") : null;
                        Drawable baseBack = null;
                        if (notifBgView != null) {
                            try {
                                baseBack = (Drawable) callMethod(notifBgView, "getBaseBackgroundLayer");
                            } catch (Throwable ignored) {}
                        }
                        Drawable bg = null;
                        if (baseBack != null) {
                            Drawable.ConstantState cs = baseBack.getConstantState();
                            Drawable newD = cs != null
                                    ? cs.newDrawable(view.getContext().getResources()) : null;
                            bg = newD != null ? newD.mutate() : baseBack.mutate();
                        }
                        view.setBackground(bg);
                        view.invalidate();
                        param.setResult(null);
                    } catch (Throwable t) {
                        XposedBridge.log("[ Obsidian ] DstNotifStyleMod.flushBackground error: " + t);
                    }
                }
            });
            XposedBridge.log("[ Obsidian ] DstNotifStyleMod: flushBackground hook installed");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] DstNotifStyleMod.hookFlushBackground error: " + t);
        }
    }

    @Override
    public boolean listensTo(String packageName) {
        return PKG_SYSTEMUI.equals(packageName);
    }
}
