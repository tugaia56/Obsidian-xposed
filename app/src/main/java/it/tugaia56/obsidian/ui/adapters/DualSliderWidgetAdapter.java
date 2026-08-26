package it.tugaia56.obsidian.ui.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.RangeSlider;

import java.util.List;
import java.util.function.BiConsumer;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.utils.ObsidianTheme;
import it.tugaia56.obsidian.utils.ObsidianTheme.GroupPos;

/**
 * Cursore a doppio pomello (min/max) — come OC's OplusSliderPreference con valueCount="2",
 * usato per la zona del gesture Indietro (altezza min/max dal bordo). A differenza di OC
 * (che salva "min,max" in un'unica stringa CSV), qui min e max vivono in due chiavi
 * ObsidianPrefs separate — più semplice, coerente col resto dell'app (nessun parsing CSV).
 */
public class DualSliderWidgetAdapter extends RecyclerView.Adapter<DualSliderWidgetAdapter.VH> {

    public static class DualSliderItem {
        public String title;
        public int min, max;
        public int rangeMin, rangeMax;
        public String suffix;
        /** (newMin, newMax) — chiamato al rilascio del cursore. */
        public BiConsumer<Integer, Integer> onChanged;
        /** Live durante il trascinamento (per anteprima), non persistito. */
        public Runnable onDragStart;
        public BiConsumer<Integer, Integer> onDrag;
        public Runnable onDragEnd;
        public GroupPos groupPos = GroupPos.SINGLE;

        public DualSliderItem(String title, int min, int max, int rangeMin, int rangeMax,
                               String suffix, BiConsumer<Integer, Integer> onChanged) {
            this.title = title;
            this.min = min;
            this.max = max;
            this.rangeMin = rangeMin;
            this.rangeMax = rangeMax;
            this.suffix = suffix != null ? suffix : "";
            this.onChanged = onChanged;
        }
    }

    private final List<DualSliderItem> items;

    public DualSliderWidgetAdapter(List<DualSliderItem> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_widget_dual_slider, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DualSliderItem item = items.get(pos);
        h.title.setText(item.title);

        float lo = Math.max(item.rangeMin, Math.min(item.rangeMax, item.min));
        float hi = Math.max(item.rangeMin, Math.min(item.rangeMax, item.max));
        if (lo > hi) { float t = lo; lo = hi; hi = t; }

        h.slider.setValueFrom(item.rangeMin);
        h.slider.setValueTo(item.rangeMax);
        h.slider.setValues(lo, hi);
        h.value.setText(item.min + item.suffix + " – " + item.max + item.suffix);

        int accent = ObsidianTheme.accentColor();
        ColorStateList accentList = ColorStateList.valueOf(accent);
        ColorStateList inactiveList = ColorStateList.valueOf(
                Color.argb(0x40, Color.red(accent), Color.green(accent), Color.blue(accent)));
        h.slider.setTrackActiveTintList(accentList);
        h.slider.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        h.slider.setTrackInactiveTintList(inactiveList);
        h.slider.setHaloTintList(accentList);
        h.value.setTextColor(accent);

        h.itemView.setBackground(ObsidianTheme.groupBackground(h.itemView.getContext(), item.groupPos));
        if (h.itemView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) h.itemView.getLayoutParams();
            ObsidianTheme.applyGroupMargins(h.itemView.getContext(), lp, item.groupPos);
            h.itemView.setLayoutParams(lp);
        }

        h.slider.clearOnChangeListeners();
        h.slider.clearOnSliderTouchListeners();

        h.slider.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser) return;
            List<Float> values = slider.getValues();
            item.min = Math.round(values.get(0));
            item.max = Math.round(values.get(1));
            h.value.setText(item.min + item.suffix + " – " + item.max + item.suffix);
            if (item.onDrag != null) item.onDrag.accept(item.min, item.max);
        });

        h.slider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(@NonNull RangeSlider s) {
                if (item.onDragStart != null) item.onDragStart.run();
            }
            @Override public void onStopTrackingTouch(@NonNull RangeSlider s) {
                if (item.onChanged != null) item.onChanged.accept(item.min, item.max);
                if (item.onDragEnd != null) item.onDragEnd.run();
            }
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, value;
        RangeSlider slider;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.dualSliderTitle);
            value = v.findViewById(R.id.dualSliderValue);
            slider = v.findViewById(R.id.dualSeekBar);
        }
    }
}
