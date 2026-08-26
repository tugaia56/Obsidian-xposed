package it.tugaia56.obsidian.xposed;

import android.content.res.Resources;

/**
 * Solo il campo {@code modRes} di ResourceManager, ma in una classe che NON implementa
 * interfacce Xposed (IXposedHookInitPackageResources/IXposedHookZygoteInit) — quelle
 * interfacce sono "compileOnly" (presenti solo a compile-time, mai nell'APK), quindi
 * qualsiasi classe le implementi non si carica/verifica nel processo dell'app stessa (fuori
 * da un hook LSPosed): java.lang.NoClassDefFoundError su IXposedHook... — confermato via
 * log dispositivo (WeatherIconPacks/CurrentWeatherView, che leggono modRes, mandavano in
 * crash la schermata "Meteo SdB" appena aperta nell'app).
 *
 * Le classi condivise tra hook (SystemUI) e UI app (WeatherIconPacks, WeatherInfo,
 * CurrentWeatherView — quest'ultima riusata anche per l'anteprima "Sfondo") leggono da QUI,
 * non da ResourceManager.modRes. XPLauncher scrive su ENTRAMBI i campi cosi tutto il resto
 * del codice (SystemUI-only) continua a funzionare invariato.
 */
public class ModResHolder {
    public static Resources modRes;
}
