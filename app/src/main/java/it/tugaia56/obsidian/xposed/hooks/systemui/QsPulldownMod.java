package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Quick Pulldown — porting reale di OC's StatusbarMods (solo la parte gesture, il resto di
 * quel file in OC copre feature diverse già gestite altrove in Obsidian). Un fling verso il
 * basso partito dal bordo scelto della status bar apre le Impostazioni Rapide; un fling verso
 * l'alto (se abilitato) comprime lo shade. Nessun ramo OOS13/SDK&lt;35: Obsidian punta solo a
 * OOS16/SDK36, quindi via anche il controllo isSeparateStyle() di OC (funzione che qui non
 * esiste).
 */
public class QsPulldownMod extends XposedMods {

    private static final int STATUSBAR_MODE_SHADE    = 0;
    private static final int STATUSBAR_MODE_KEYGUARD = 1;
    private static final int PULLDOWN_SIDE_RIGHT = 1;

    private static final String PREF_PULLDOWN_ON       = "OBS_QS_PULLDOWN_ON";
    private static final String PREF_PULLDOWN_LENGTH   = "OBS_QS_PULLDOWN_LENGTH";
    private static final String PREF_PULLDOWN_SIDE     = "OBS_QS_PULLDOWN_SIDE";
    private static final String PREF_PULLDOWN_COLLAPSE = "OBS_QS_PULLDOWN_COLLAPSE";

    private boolean mPulldownEnabled = false;
    private boolean mCollapseEnabled = false;
    private int     mPullDownSide = PULLDOWN_SIDE_RIGHT;
    private float   mStatusbarPortion = 0.25f;

    private Object mNotificationPanelViewController;
    private String mQsExpandMethodName;

    public QsPulldownMod(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mPulldownEnabled = Xprefs.getBoolean(PREF_PULLDOWN_ON, false);
        mCollapseEnabled = Xprefs.getBoolean(PREF_PULLDOWN_COLLAPSE, false);
        try { mPullDownSide = Integer.parseInt(Xprefs.getString(PREF_PULLDOWN_SIDE, "1")); }
        catch (Throwable t) { mPullDownSide = PULLDOWN_SIDE_RIGHT; }
        mStatusbarPortion = Xprefs.getInt(PREF_PULLDOWN_LENGTH, 25) / 100f;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!SYSTEM_UI.equals(lp.packageName)) return;

        Class<?> panelControllerClass = findClass(
                "com.android.systemui.shade.NotificationPanelViewController", lp.classLoader);
        Class<?> phoneStatusBarViewControllerClass = findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarViewController", lp.classLoader);

        mQsExpandMethodName = Arrays.stream(panelControllerClass.getMethods())
                .anyMatch(m -> m.getName().equals("expandToQs"))
                ? "expandToQs" : "expandWithQs";

        final GestureDetector.OnGestureListener pullUpListener = new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || mNotificationPanelViewController == null) return false;
                if (!isValidFling(e1, e2, velocityY, -.15f, -.06f)) return false;
                try {
                    callMethod(mNotificationPanelViewController, "collapse", true, 1f, "collapse");
                } catch (Throwable ignored) {}
                return true;
            }
        };

        hookAllConstructors(panelControllerClass, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                mNotificationPanelViewController = p.thisObject;
                try {
                    Object touchHandler = getObjectField(p.thisObject, "mTouchHandler");
                    GestureDetector pullUpDetector = new GestureDetector(mContext, pullUpListener);
                    hookAllMethods(touchHandler.getClass(), "onTouchEvent", new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam tp) {
                            if (!mCollapseEnabled) return;
                            try {
                                int barState = (int) getObjectField(mNotificationPanelViewController, "mBarState");
                                if (barState != STATUSBAR_MODE_KEYGUARD) {
                                    pullUpDetector.onTouchEvent((MotionEvent) tp.args[0]);
                                }
                            } catch (Throwable ignored) {}
                        }
                    });
                } catch (Throwable ignored) {}
            }
        });

        final GestureDetector.OnGestureListener pullDownListener = new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || mNotificationPanelViewController == null) return false;
                try {
                    int barState = (int) getObjectField(mNotificationPanelViewController, "mBarState");
                    if (barState == STATUSBAR_MODE_SHADE && isValidFling(e1, e2, velocityY, .15f, 0.01f)) {
                        callMethod(mNotificationPanelViewController, mQsExpandMethodName);
                        return true;
                    }
                } catch (Throwable ignored) {}
                return false;
            }
        };
        final GestureDetector mGestureDetector = new GestureDetector(mContext, pullDownListener);

        hookAllMethods(phoneStatusBarViewControllerClass, "onTouch", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!mPulldownEnabled) return;
                try {
                    MotionEvent event = p.args[0] instanceof MotionEvent
                            ? (MotionEvent) p.args[0] : (MotionEvent) p.args[1];
                    mGestureDetector.onTouchEvent(event);
                } catch (Throwable ignored) {}
            }
        });
    }

    private boolean isValidFling(MotionEvent e1, MotionEvent e2, float velocityY, float speedFactor, float heightFactor) {
        try {
            Rect displayBounds = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                    .getCurrentWindowMetrics().getBounds();
            return (e2.getY() - e1.getY()) / heightFactor > displayBounds.height()
                    && isTouchInRegion(e1, displayBounds.width())
                    && (velocityY / speedFactor > displayBounds.height());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isTouchInRegion(MotionEvent e, float width) {
        float x = e.getX();
        float region = width * mStatusbarPortion;
        return (mPullDownSide == PULLDOWN_SIDE_RIGHT) ? width - region < x : x < region;
    }

    @Override public boolean listensTo(String packageName) { return SYSTEM_UI.equals(packageName); }
}
