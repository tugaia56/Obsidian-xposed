package it.tugaia56.obsidian.xposed.hooks.systemui;

import android.graphics.drawable.Drawable;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook diretto su com.android.systemui.statusbar.notification.row.NotificationBackgroundView
 * — la classe reale (OOS/AOSP) che disegna lo sfondo di ogni notifica. Su questa build lo
 * sfondo non passa mai per una risorsa drawable sostituibile via XResources (verificato con
 * una diagnostica: notification_material_bg[/_monet] non viene mai richiesta), quindi
 * DstNotifStyle.applyPreloaded() (basato su XResources.setReplacement) non ha alcun effetto.
 *
 * Stessa tecnica già usata con successo per il Dialogo (vedi DstDialogStyle): si intercetta
 * il metodo che imposta lo sfondo e si sostituisce l'argomento con il nostro drawable, invece
 * di sostituire una risorsa che il sistema non carica mai.
 */
public class DstNotifBgView {

    private static final String CLASS_NAME =
        "com.android.systemui.statusbar.notification.row.NotificationBackgroundView";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> cls = lpparam.classLoader.loadClass(CLASS_NAME);

            XposedBridge.hookAllMethods(cls, "setCustomBackground", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args.length == 0) return;
                    if (!(p.args[0] instanceof Drawable)) return; // skip int-resId overload
                    try {
                        View v = (View) p.thisObject;
                        float density = v.getResources().getDisplayMetrics().density;
                        boolean isNight = (v.getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                        Drawable d = DstNotifStyle.currentDrawable(density, isNight);
                        if (d != null) p.args[0] = d;
                    } catch (Throwable t) {
                        XposedBridge.log("[ Obsidian ] DstNotifBgView build ERROR: " + t);
                    }
                }
            });
            XposedBridge.log("[ Obsidian ] DstNotifBgView: hooked " + CLASS_NAME
                    + ".setCustomBackground");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] DstNotifBgView: hook FAILED: " + t);
        }
    }
}
