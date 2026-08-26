package it.tugaia56.obsidian.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client minimale per open-meteo.com — gratis, senza chiave API. Nessuna dipendenza
 * esterna (HttpURLConnection + org.json, entrambi già inclusi in Android).
 */
public class OpenMeteoClient {

    private static final String GEOCODE_URL =
        "https://geocoding-api.open-meteo.com/v1/search?count=1&language=it&format=json&name=";
    private static final String FORECAST_URL =
        "https://api.open-meteo.com/v1/forecast?current=temperature_2m,relative_humidity_2m,"
        + "wind_speed_10m,weather_code,is_day&timezone=auto&latitude=%f&longitude=%f";

    public static class GeoResult {
        public double lat, lon;
        public String resolvedName;
    }

    /** Risolve un nome città in coordinate. Ritorna null se non trovata o in caso di errore. */
    public static GeoResult geocode(String cityName) {
        try {
            String url = GEOCODE_URL + URLEncoder.encode(cityName, StandardCharsets.UTF_8.name());
            String json = httpGet(url);
            JSONObject root = new JSONObject(json);
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) return null;
            JSONObject first = results.getJSONObject(0);
            GeoResult r = new GeoResult();
            r.lat = first.getDouble("latitude");
            r.lon = first.getDouble("longitude");
            r.resolvedName = first.optString("name", cityName);
            String admin = first.optString("admin1", "");
            String country = first.optString("country", "");
            if (!admin.isEmpty()) r.resolvedName += ", " + admin;
            else if (!country.isEmpty()) r.resolvedName += ", " + country;
            return r;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Scarica il meteo corrente per le coordinate date. Ritorna null in caso di errore. */
    public static WeatherInfo fetchCurrent(double lat, double lon, String cityName) {
        try {
            String url = String.format(java.util.Locale.US, FORECAST_URL, lat, lon);
            String json = httpGet(url);
            JSONObject root = new JSONObject(json);
            JSONObject current = root.getJSONObject("current");

            WeatherInfo w = new WeatherInfo();
            w.tempC = (int) Math.round(current.getDouble("temperature_2m"));
            w.humidityPct = current.optInt("relative_humidity_2m", 0);
            w.windKmh = current.optDouble("wind_speed_10m", 0);
            w.weatherCode = current.optInt("weather_code", 0);
            w.isDay = current.optInt("is_day", 1) == 1;
            w.cityName = cityName;
            w.fetchedAtMillis = System.currentTimeMillis();
            return w;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setRequestMethod("GET");
        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
