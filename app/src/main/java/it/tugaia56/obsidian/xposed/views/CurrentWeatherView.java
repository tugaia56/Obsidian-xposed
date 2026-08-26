package it.tugaia56.obsidian.xposed.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.xposed.ModResHolder;
import it.tugaia56.obsidian.utils.WeatherBgFactory;
import it.tugaia56.obsidian.weather.WeatherIconPacks;
import it.tugaia56.obsidian.weather.WeatherInfo;

/**
 * View meteo riusabile — un'icona + testo, condivisa da AOD e Lockscreen (come
 * CurrentWeatherView di OC). Non fa fetching: riceve un WeatherInfo già pronto e si
 * limita a disegnarlo secondo le preferenze correnti.
 */
public class CurrentWeatherView extends LinearLayout {

    private final ImageView mIcon;
    private final TextView  mTempCondition;
    private final TextView  mLocation;
    private final TextView  mExtra; // umidità/vento
    private String mIconPack = WeatherIconPacks.DEFAULT;
    private int mColor = Color.WHITE;

    public CurrentWeatherView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        mIcon = new ImageView(context);
        LayoutParams iconLp = new LayoutParams(dp(18), dp(18));
        iconLp.setMarginEnd(dp(6));
        mIcon.setLayoutParams(iconLp);
        addView(mIcon);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(VERTICAL);
        textCol.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mTempCondition = new TextView(context);
        mTempCondition.setTextColor(Color.WHITE);
        mTempCondition.setTextSize(16);
        textCol.addView(mTempCondition);

        mLocation = new TextView(context);
        mLocation.setTextColor(0xCCFFFFFF);
        mLocation.setTextSize(12);
        textCol.addView(mLocation);

        mExtra = new TextView(context);
        mExtra.setTextColor(0xCCFFFFFF);
        mExtra.setTextSize(12);
        textCol.addView(mExtra);

        addView(textCol);
    }

    /** Aggiorna il contenuto mostrato. {@code data} può essere null (mostra "--"). */
    public void updateData(WeatherInfo data, boolean metric, boolean showLocation,
                            boolean showCondition, boolean showHumidity, boolean showWind) {
        if (data == null) {
            mIcon.setImageDrawable(null);
            mTempCondition.setText("--");
            mLocation.setVisibility(GONE);
            mExtra.setVisibility(GONE);
            return;
        }

        applyIcon(data);

        int temp = metric ? data.tempC : Math.round(data.tempC * 9f / 5f + 32);
        String unit = metric ? "°C" : "°F";
        String line = temp + unit;
        if (showCondition) line += " · " + WeatherInfo.conditionText(data.weatherCode, getContext());
        mTempCondition.setText(line);

        if (showLocation && data.cityName != null && !data.cityName.isEmpty()) {
            mLocation.setText(data.cityName);
            mLocation.setVisibility(VISIBLE);
        } else {
            mLocation.setVisibility(GONE);
        }

        StringBuilder extra = new StringBuilder();
        if (showHumidity) extra.append(data.humidityPct).append("% ");
        if (showWind) {
            double wind = metric ? data.windKmh : data.windKmh * 0.621371;
            extra.append(Math.round(wind)).append(metric ? "km/h" : "mph");
        }
        if (extra.length() > 0) {
            mExtra.setText(extra.toString().trim());
            mExtra.setVisibility(VISIBLE);
        } else {
            mExtra.setVisibility(GONE);
        }
    }

    public void updateSizes(int textSizeSp, int imageSizeDp) {
        mTempCondition.setTextSize(textSizeSp);
        mLocation.setTextSize(Math.max(10, textSizeSp - 4));
        mExtra.setTextSize(Math.max(10, textSizeSp - 4));
        LayoutParams iconLp = (LayoutParams) mIcon.getLayoutParams();
        iconLp.width = dp(imageSizeDp);
        iconLp.height = dp(imageSizeDp);
        mIcon.setLayoutParams(iconLp);
    }

    public void updateColor(int color) {
        mColor = color;
        mTempCondition.setTextColor(color);
        applyIconTint();
        int dim = (color & 0x00FFFFFF) | 0xCC000000;
        mLocation.setTextColor(dim);
        mExtra.setTextColor(dim);
    }

    /** Pacchetto icone condizioni (prefisso, vedi WeatherIconPacks) — "none" usa le 8
     *  icone vettoriali built-in (tintabili); i pacchetti reali sono immagini a colori
     *  fissi e non vengono tinti. */
    public void updateIconPack(String prefix) {
        mIconPack = prefix;
    }

    private void applyIcon(WeatherInfo data) {
        int packIcon = WeatherIconPacks.resolve(getContext(), mIconPack, data.weatherCode, data.isDay);
        Drawable d = packIcon != 0 ? resolveDrawable(packIcon) : null;
        if (d == null) d = resolveDrawable(WeatherInfo.iconRes(data.weatherCode, data.isDay));
        mIcon.setImageDrawable(d);
        applyIconTint();
    }

    /** Risolve un drawable del NOSTRO modulo per ID, usando ModResHolder.modRes —
     *  getContext() qui è il Context di SystemUI: setImageResource(int)/getDrawable(int)
     *  su di esso risolverebbe l'ID contro le risorse sbagliate (quelle di SystemUI). */
    private Drawable resolveDrawable(int resId) {
        if (resId == 0) return null;
        Resources res = ModResHolder.modRes != null ? ModResHolder.modRes : getResources();
        try {
            return ResourcesCompat.getDrawable(res, resId, getContext().getTheme());
        } catch (Throwable t) {
            return null;
        }
    }

    private void applyIconTint() {
        boolean usingRealPack = mIconPack != null && !WeatherIconPacks.DEFAULT.equals(mIconPack);
        if (usingRealPack) {
            mIcon.clearColorFilter();
        } else {
            mIcon.setColorFilter(mColor, PorterDuff.Mode.SRC_IN);
        }
    }

    /** Sfondo del widget (0-8) — porting reale di OC's CurrentWeatherView.updateWeatherBg().
     *  Le varianti 0-3 (nessuno/box/pillola) sono drawable XML statici, senza accento.
     *  Le varianti 4-8 (accento/gradiente/bordi) sono costruite qui in Java con
     *  {@code accent1}/{@code accent2} — non colori di sistema fissi, cosi seguono lo
     *  stesso accento del resto dell'app (DST se attivo, altrimenti i default di Obsidian),
     *  non il colore Material You del dispositivo che può risultare molto più scuro/diverso. */
    public void updateWeatherBg(int selection, int accent1, int accent2) {
        Drawable bg = switch (selection) {
            case 1 -> resolveDrawable(R.drawable.weather_bg_box);
            case 2 -> resolveDrawable(R.drawable.weather_bg_box_round);
            case 3 -> resolveDrawable(R.drawable.weather_bg_pill);
            default -> WeatherBgFactory.buildAccentDrawable(
                    selection, accent1, accent2, getResources().getDisplayMetrics().density);
        };
        if (bg != null) bg = bg.mutate();
        setBackground(bg);
        if (bg != null) bg.setAlpha(WeatherBgFactory.alpha(selection));
        int padHDp = WeatherBgFactory.paddingHDp(selection);
        int padVDp = WeatherBgFactory.paddingVDp(selection);
        setPadding(dp(padHDp), dp(padVDp), dp(padHDp), dp(padVDp));
    }

    public void setGravityCentered(boolean centered) {
        setGravity(centered ? (Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL) : Gravity.CENTER_VERTICAL);
        for (int i = 0; i < getChildCount(); i++) {
            android.view.View child = getChildAt(i);
            if (child instanceof LinearLayout ll) {
                ll.setGravity(centered ? Gravity.CENTER_HORIZONTAL : Gravity.START);
            }
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
