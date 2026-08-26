package it.tugaia56.obsidian.weather;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Client minimale per OpenWeatherMap — richiede una chiave API (piano gratuito ok). */
public class OpenWeatherMapClient {

    private static final String URL_FMT =
        "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=metric&appid=%s";

    public static WeatherInfo fetchCurrent(double lat, double lon, String cityName, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) return null;
        try {
            String url = String.format(Locale.US, URL_FMT, lat, lon, apiKey.trim());
            JSONObject root = new JSONObject(httpGet(url));
            JSONObject main = root.getJSONObject("main");
            JSONObject wind = root.optJSONObject("wind");
            int conditionId = root.getJSONArray("weather").getJSONObject(0).optInt("id", 800);

            WeatherInfo w = new WeatherInfo();
            w.tempC = (int) Math.round(main.getDouble("temp"));
            w.humidityPct = main.optInt("humidity", 0);
            w.windKmh = wind != null ? wind.optDouble("speed", 0) * 3.6 : 0;
            w.weatherCode = owmToWmo(conditionId);
            w.isDay = true; // OWM current endpoint doesn't give a simple day/night flag here
            w.cityName = cityName;
            w.fetchedAtMillis = System.currentTimeMillis();
            return w;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Mappa gli ID condizione di OpenWeatherMap sui codici WMO usati da WeatherInfo. */
    private static int owmToWmo(int id) {
        if (id >= 200 && id <= 232) return 95;               // temporale
        if (id >= 300 && id <= 321) return 53;                // pioviggine
        if (id >= 500 && id <= 504) return 63;                // pioggia
        if (id == 511) return 66;                             // pioggia gelata
        if (id >= 520 && id <= 531) return 80;                // rovesci
        if (id >= 600 && id <= 622) return 73;                // neve
        if (id >= 701 && id <= 781) return 45;                // nebbia/foschia/ecc
        if (id == 800) return 0;                              // sereno
        if (id == 801) return 1;                              // poco nuvoloso
        if (id == 802) return 2;                              // parzialmente nuvoloso
        if (id == 803 || id == 804) return 3;                 // nuvoloso/coperto
        return 3;
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
