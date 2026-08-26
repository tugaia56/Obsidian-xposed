package it.tugaia56.obsidian.weather;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Client minimale per Yandex Weather API — richiede una chiave (X-Yandex-Weather-Key). */
public class YandexWeatherClient {

    private static final String URL_FMT =
        "https://api.weather.yandex.ru/v2/forecast?lat=%f&lon=%f&lang=en_US";

    public static WeatherInfo fetchCurrent(double lat, double lon, String cityName, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) return null;
        try {
            String url = String.format(Locale.US, URL_FMT, lat, lon);
            JSONObject root = new JSONObject(httpGet(url, apiKey.trim()));
            JSONObject fact = root.getJSONObject("fact");

            WeatherInfo w = new WeatherInfo();
            w.tempC = fact.getInt("temp");
            w.humidityPct = fact.optInt("humidity", 0);
            w.windKmh = fact.optDouble("wind_speed", 0) * 3.6;
            w.weatherCode = yandexToWmo(fact.optString("condition", "cloudy"));
            w.isDay = "day".equals(fact.optString("daytime", "day"));
            w.cityName = cityName;
            w.fetchedAtMillis = System.currentTimeMillis();
            return w;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Mappa le stringhe condizione di Yandex sui codici WMO usati da WeatherInfo. */
    private static int yandexToWmo(String cond) {
        return switch (cond) {
            case "clear" -> 0;
            case "partly-cloudy" -> 1;
            case "cloudy" -> 2;
            case "overcast" -> 3;
            case "drizzle", "light-rain" -> 53;
            case "rain", "moderate-rain", "heavy-rain", "continuous-heavy-rain" -> 63;
            case "showers" -> 80;
            case "wet-snow", "light-snow", "snow", "snow-showers" -> 73;
            case "hail" -> 96;
            case "thunderstorm", "thunderstorm-with-rain", "thunderstorm-with-hail" -> 95;
            default -> 3;
        };
    }

    private static String httpGet(String urlStr, String apiKey) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Yandex-Weather-Key", apiKey);
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
