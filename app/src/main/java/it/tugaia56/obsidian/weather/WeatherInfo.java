package it.tugaia56.obsidian.weather;

/**
 * Semplice modello dati meteo — sottoinsieme di quello che serve alle view AOD/Lockscreen
 * (temperatura, condizione, umidità, vento, nome località). Serializzato come stringa
 * delimitata da "|" per il salvataggio in ObsidianPrefs (niente Gson/dipendenze nuove).
 */
public class WeatherInfo {
    public int    tempC;
    public int    humidityPct;
    public double windKmh;
    public int    weatherCode;   // codice WMO (open-meteo.com/en/docs)
    public boolean isDay;
    public String cityName;
    public long   fetchedAtMillis;

    public String toSerializedString() {
        return tempC + "|" + humidityPct + "|" + windKmh + "|" + weatherCode + "|"
                + (isDay ? 1 : 0) + "|" + (cityName == null ? "" : cityName.replace("|", " "))
                + "|" + fetchedAtMillis;
    }

    public static WeatherInfo fromSerializedString(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] p = s.split("\\|", -1);
            WeatherInfo w = new WeatherInfo();
            w.tempC = Integer.parseInt(p[0]);
            w.humidityPct = Integer.parseInt(p[1]);
            w.windKmh = Double.parseDouble(p[2]);
            w.weatherCode = Integer.parseInt(p[3]);
            w.isDay = "1".equals(p[4]);
            w.cityName = p[5];
            w.fetchedAtMillis = Long.parseLong(p[6]);
            return w;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Testo condizione in italiano/inglese a seconda del codice WMO (open-meteo). */
    public static String conditionText(int code, android.content.Context ctx) {
        int resId;
        if (code == 0) resId = it.tugaia56.obsidian.R.string.weather_cond_clear;
        else if (code <= 2) resId = it.tugaia56.obsidian.R.string.weather_cond_partly_cloudy;
        else if (code == 3) resId = it.tugaia56.obsidian.R.string.weather_cond_cloudy;
        else if (code == 45 || code == 48) resId = it.tugaia56.obsidian.R.string.weather_cond_fog;
        else if (code >= 51 && code <= 57) resId = it.tugaia56.obsidian.R.string.weather_cond_drizzle;
        else if (code >= 61 && code <= 67) resId = it.tugaia56.obsidian.R.string.weather_cond_rain;
        else if (code >= 71 && code <= 77) resId = it.tugaia56.obsidian.R.string.weather_cond_snow;
        else if (code >= 80 && code <= 82) resId = it.tugaia56.obsidian.R.string.weather_cond_rain_showers;
        else if (code >= 85 && code <= 86) resId = it.tugaia56.obsidian.R.string.weather_cond_snow_showers;
        else if (code >= 95) resId = it.tugaia56.obsidian.R.string.weather_cond_thunderstorm;
        else resId = it.tugaia56.obsidian.R.string.weather_cond_unknown;
        // Dentro SystemUI, ctx.getString() risolverebbe l'ID contro le risorse di SystemUI
        // (sbagliate) — vanno usate le risorse del nostro modulo caricate a parte.
        android.content.res.Resources modRes = it.tugaia56.obsidian.xposed.ModResHolder.modRes;
        if (modRes != null) {
            try { return modRes.getString(resId); } catch (Throwable ignored) {}
        }
        return ctx.getString(resId);
    }

    /** Icona (vettoriale semplice, poche categorie) in base al codice WMO. */
    public static int iconRes(int code, boolean isDay) {
        if (code == 0) return isDay ? it.tugaia56.obsidian.R.drawable.ic_weather_sunny
                                     : it.tugaia56.obsidian.R.drawable.ic_weather_clear_night;
        if (code <= 2) return isDay ? it.tugaia56.obsidian.R.drawable.ic_weather_partly_cloudy
                                     : it.tugaia56.obsidian.R.drawable.ic_weather_clear_night;
        if (code == 3) return it.tugaia56.obsidian.R.drawable.ic_weather_cloudy;
        if (code == 45 || code == 48) return it.tugaia56.obsidian.R.drawable.ic_weather_fog;
        if (code >= 51 && code <= 67) return it.tugaia56.obsidian.R.drawable.ic_weather_rain;
        if (code >= 71 && code <= 77) return it.tugaia56.obsidian.R.drawable.ic_weather_snow;
        if (code >= 80 && code <= 82) return it.tugaia56.obsidian.R.drawable.ic_weather_rain;
        if (code >= 85 && code <= 86) return it.tugaia56.obsidian.R.drawable.ic_weather_snow;
        if (code >= 95) return it.tugaia56.obsidian.R.drawable.ic_weather_thunderstorm;
        return it.tugaia56.obsidian.R.drawable.ic_weather_cloudy;
    }
}
