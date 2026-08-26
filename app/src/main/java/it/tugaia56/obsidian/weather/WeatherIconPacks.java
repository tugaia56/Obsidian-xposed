package it.tugaia56.obsidian.weather;

import android.content.Context;
import android.content.res.Resources;

/**
 * Pacchetti icone condizioni meteo — porting del sistema a 18 pacchetti di OC
 * (OmniJawsClient). I file sono numerati secondo lo schema di condizione Yahoo Weather
 * (0-47 + "na"), lo stesso usato dai pacchetti originali: "&lt;prefisso&gt;_&lt;codice&gt;".
 * "none" (il primo valore) usa le 8 icone vettoriali semplici già presenti in Obsidian
 * (WeatherInfo.iconRes), tintabili con il colore personalizzato; i pacchetti reali sono
 * immagini a colori fissi e non vengono tinti.
 */
public class WeatherIconPacks {

    public static final String DEFAULT = "none";

    /** Ordine identico allo screenshot di OC: Google, Google Material Dark/Light, Outline,
     *  LockClock, Weather Client, Stickers, Marshmallow, Color Hand, Faded, Weezle,
     *  Galaxy S6, MIUI, Sthul, Tapas, Magical, VClouds, Nothing. */
    public static final String[] PREFIXES = {
            DEFAULT, "google", "google_new_dark", "google_new_light", "outline",
            "weather_color", "weatherclient", "stickers", "marshmallow", "icons8_color_hand",
            "weather_faded", "weather_weezle", "weather_gs6", "weather_miui", "weather_sthul",
            "weather_tapas", "weather_magical", "weather_vclouds", "nothing",
    };

    /** Nomi propri (marchi/pacchetti) — nessuna traduzione necessaria, come per i provider meteo. */
    private static final String[] LABELS_NO_DEFAULT = {
            "Google", "Google Material Dark", "Google Material Light", "Outline",
            "LockClock", "Weather Client", "Stickers", "Marshmallow", "Color Hand",
            "Faded", "Weezle", "Galaxy S6", "MIUI", "Sthul", "Tapas", "Magical", "VClouds", "Nothing",
    };

    /** Icona di anteprima per pacchetto (condizione 30 = "sunny", stesso codice usato da OC). */
    private static final int[] PREVIEW_ICONS_NO_DEFAULT = {
            it.tugaia56.obsidian.R.drawable.google_30,
            it.tugaia56.obsidian.R.drawable.google_new_dark_30,
            it.tugaia56.obsidian.R.drawable.google_new_light_30,
            it.tugaia56.obsidian.R.drawable.outline_30,
            it.tugaia56.obsidian.R.drawable.weather_color_30,
            it.tugaia56.obsidian.R.drawable.weatherclient_30,
            it.tugaia56.obsidian.R.drawable.stickers_30,
            it.tugaia56.obsidian.R.drawable.marshmallow_30,
            it.tugaia56.obsidian.R.drawable.icons8_color_hand_30,
            it.tugaia56.obsidian.R.drawable.weather_faded_30,
            it.tugaia56.obsidian.R.drawable.weather_weezle_30,
            it.tugaia56.obsidian.R.drawable.weather_gs6_30,
            it.tugaia56.obsidian.R.drawable.weather_miui_30,
            it.tugaia56.obsidian.R.drawable.weather_sthul_30,
            it.tugaia56.obsidian.R.drawable.weather_tapas_30,
            it.tugaia56.obsidian.R.drawable.weather_magical_30,
            it.tugaia56.obsidian.R.drawable.weather_vclouds_30,
            it.tugaia56.obsidian.R.drawable.nothing_30,
    };

    /** Etichette per la dialog di scelta, con "Predefinito" in prima posizione. */
    public static String[] labels(Context ctx) {
        String[] out = new String[LABELS_NO_DEFAULT.length + 1];
        out[0] = ctx.getString(it.tugaia56.obsidian.R.string.weather_icon_pack_default);
        System.arraycopy(LABELS_NO_DEFAULT, 0, out, 1, LABELS_NO_DEFAULT.length);
        return out;
    }

    /** Icona di anteprima per indice (0 = predefinito, usa l'icona vettoriale "sereno"). */
    public static int previewIcon(int index) {
        if (index <= 0) return it.tugaia56.obsidian.R.drawable.ic_weather_sunny;
        int i = index - 1;
        return (i >= 0 && i < PREVIEW_ICONS_NO_DEFAULT.length) ? PREVIEW_ICONS_NO_DEFAULT[i] : it.tugaia56.obsidian.R.drawable.ic_weather_sunny;
    }

    public static String prefixForIndex(int index) {
        return (index >= 0 && index < PREFIXES.length) ? PREFIXES[index] : DEFAULT;
    }

    public static int indexForPrefix(String prefix) {
        for (int i = 0; i < PREFIXES.length; i++) if (PREFIXES[i].equals(prefix)) return i;
        return 0;
    }

    /** Risolve il drawable del pacchetto per il codice WMO dato, o 0 se il pacchetto è
     *  "none" o il file non esiste (fallback poi alle icone vettoriali built-in).
     *  Usa sempre le risorse/il nome pacchetto del NOSTRO modulo (mai quelle di {@code ctx},
     *  che dentro SystemUI punterebbero alla tabella risorse sbagliata). */
    public static int resolve(Context ctx, String prefix, int wmoCode, boolean isDay) {
        if (prefix == null || DEFAULT.equals(prefix)) return 0;
        Resources modRes = it.tugaia56.obsidian.xposed.ModResHolder.modRes;
        Resources res = modRes != null ? modRes : ctx.getResources();
        String pkg = it.tugaia56.obsidian.BuildConfig.APPLICATION_ID;
        int yahoo = yahooCode(wmoCode, isDay);
        int resId = res.getIdentifier(prefix + "_" + yahoo, "drawable", pkg);
        if (resId != 0) return resId;
        resId = res.getIdentifier(prefix + "_na", "drawable", pkg);
        return resId; // può essere 0 — il chiamante ricade sulle icone built-in
    }

    /** Mappa il codice WMO (open-meteo) sul codice di condizione Yahoo Weather (0-47),
     *  lo schema numerico usato dai nomi dei file dei pacchetti icone. */
    private static int yahooCode(int wmo, boolean isDay) {
        if (wmo == 0) return isDay ? 32 : 31;               // clear
        if (wmo <= 2) return isDay ? 30 : 29;                // mainly clear / partly cloudy
        if (wmo == 3) return 26;                             // overcast -> cloudy
        if (wmo == 45 || wmo == 48) return 20;               // fog
        if (wmo >= 56 && wmo <= 57) return 8;                // freezing drizzle
        if (wmo >= 51 && wmo <= 55) return 9;                // drizzle
        if (wmo >= 66 && wmo <= 67) return 10;                // freezing rain
        if (wmo == 61) return 11;                             // rain slight
        if (wmo == 63) return 11;                             // rain moderate
        if (wmo == 65) return 12;                             // rain heavy
        if (wmo == 77) return 13;                             // snow grains
        if (wmo == 71 || wmo == 73) return 16;                // snow slight/moderate
        if (wmo == 75) return 41;                             // snow heavy
        if (wmo == 80) return 11;                             // rain showers slight
        if (wmo == 81) return 40;                             // rain showers moderate
        if (wmo == 82) return 12;                             // rain showers violent
        if (wmo == 85 || wmo == 86) return 46;                // snow showers
        if (wmo == 95) return 4;                              // thunderstorm
        if (wmo == 96 || wmo == 99) return 3;                 // thunderstorm w/ hail
        return 26;
    }
}
