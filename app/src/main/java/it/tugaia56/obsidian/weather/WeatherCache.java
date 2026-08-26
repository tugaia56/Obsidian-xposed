package it.tugaia56.obsidian.weather;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cache in memoria (per processo) del meteo corrente, condivisa tra AodWeather e
 * LockscreenWeather — evita richieste di rete duplicate se entrambe le funzioni sono
 * attive nello stesso processo. Niente persistenza tra riavvii di SystemUI: un nuovo
 * fetch al primo avvio è normale e sufficiente (il meteo cambia lentamente).
 */
public class WeatherCache {

    private static volatile WeatherInfo sLast = null;
    private static volatile String sLastCityKey = null;
    private static final AtomicBoolean sFetching = new AtomicBoolean(false);

    public static WeatherInfo get() { return sLast; }

    /** true se serve un nuovo fetch: nessun dato, città cambiata, o dato più vecchio
     *  di {@code maxAgeMinutes}. */
    public static boolean isStale(String cityKey, int maxAgeMinutes) {
        if (sLast == null) return true;
        if (cityKey != null && !cityKey.equals(sLastCityKey)) return true;
        long ageMs = System.currentTimeMillis() - sLast.fetchedAtMillis;
        return ageMs > maxAgeMinutes * 60_000L;
    }

    public static boolean tryStartFetch() {
        return sFetching.compareAndSet(false, true);
    }

    public static void finishFetch(WeatherInfo info, String cityKey) {
        if (info != null) {
            sLast = info;
            sLastCityKey = cityKey;
        }
        sFetching.set(false);
    }
}
