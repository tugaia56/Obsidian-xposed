package it.tugaia56.obsidian.weather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Prende un fix GPS/rete UNA volta (non tracking continuo — il meteo non ha bisogno di
 * aggiornamenti in tempo reale) e lo salva in ObsidianPrefs come lat/lon/nome, da cui
 * LockscreenWeather/AodWeather (Mod, processo SystemUI) leggono direttamente — SystemUI
 * stesso non ha il permesso di posizione, va richiesto qui nel processo Obsidian.
 */
public class GpsLocationHelper {

    public static final String KEY_GPS_LAT  = "weather_gps_lat";
    public static final String KEY_GPS_LON  = "weather_gps_lon";
    public static final String KEY_GPS_NAME = "weather_gps_name";

    private static final long FIX_TIMEOUT_MS = 15_000;

    public static boolean hasPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Nome già risolto dall'ultimo fix, o null se non ancora disponibile — per la UI. */
    public static String lastKnownName() {
        String name = ObsidianPrefs.getString(KEY_GPS_NAME, "");
        return name.isEmpty() ? null : name;
    }

    /** Chiede un fix (ultima posizione nota se già disponibile e recente, altrimenti un
     *  singolo aggiornamento con timeout), reverse-geocodifica e salva in ObsidianPrefs.
     *  {@code onDone} viene chiamato sul thread main con true/false. Presuppone il permesso
     *  già concesso — il chiamante (Fragment) lo richiede prima. */
    public static void requestFix(Context ctx, Consumer<Boolean> onDone) {
        if (!hasPermission(ctx)) { onDone.accept(false); return; }
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { onDone.accept(false); return; }

        Location best = null;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (!lm.isProviderEnabled(provider)) continue;
                Location loc = lm.getLastKnownLocation(provider);
                if (loc != null && (best == null || loc.getTime() > best.getTime())) best = loc;
            } catch (SecurityException ignored) {}
        }

        // Un'ultima posizione nota più vecchia di 30 minuti è meglio scartarla e chiedere un
        // fix fresco — il meteo dipende dalla posizione ATTUALE, non da dove si era prima.
        if (best != null && System.currentTimeMillis() - best.getTime() < 30 * 60_000L) {
            finish(ctx, best, onDone);
            return;
        }

        String provider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER
                : lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : null;
        if (provider == null) {
            // Nessun provider attivo (GPS/rete spenti) — usa comunque l'ultima posizione nota
            // per quanto vecchia, meglio di niente; se non c'è nemmeno quella, fallisce.
            finish(ctx, best, onDone);
            return;
        }

        Handler main = new Handler(Looper.getMainLooper());
        boolean[] done = {false};
        LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (done[0]) return;
                done[0] = true;
                lm.removeUpdates(this);
                finish(ctx, location, onDone);
            }
        };
        try {
            lm.requestLocationUpdates(provider, 0, 0, listener, Looper.getMainLooper());
        } catch (SecurityException e) {
            onDone.accept(false);
            return;
        }
        Location finalBest = best;
        main.postDelayed(() -> {
            if (done[0]) return;
            done[0] = true;
            try { lm.removeUpdates(listener); } catch (Throwable ignored) {}
            finish(ctx, finalBest, onDone); // vecchia posizione se c'è, altrimenti fallisce
        }, FIX_TIMEOUT_MS);
    }

    private static void finish(Context ctx, Location loc, Consumer<Boolean> onDone) {
        if (loc == null) { onDone.accept(false); return; }
        double lat = loc.getLatitude(), lon = loc.getLongitude();
        new Thread(() -> {
            String name = reverseGeocode(ctx, lat, lon);
            ObsidianPrefs.putFloat(KEY_GPS_LAT, (float) lat);
            ObsidianPrefs.putFloat(KEY_GPS_LON, (float) lon);
            ObsidianPrefs.putString(KEY_GPS_NAME, name != null ? name : "");
            new Handler(Looper.getMainLooper()).post(() -> onDone.accept(true));
        }).start();
    }

    private static String reverseGeocode(Context ctx, double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
            @SuppressWarnings("deprecation")
            List<android.location.Address> results = geocoder.getFromLocation(lat, lon, 1);
            if (results == null || results.isEmpty()) return null;
            android.location.Address a = results.get(0);
            String locality = a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
            String admin = a.getAdminArea();
            if (locality == null) return admin;
            return admin != null && !admin.equals(locality) ? locality + ", " + admin : locality;
        } catch (Throwable t) {
            return null;
        }
    }
}
