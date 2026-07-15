package it.tugaia56.obsidian.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.models.DarkShadowItem;

public class DarkShadowColorListener extends RecyclerView.Adapter<DarkShadowColorListener.VH> {

    public interface OnEnabled  { void run(DarkShadowItem item); }
    public interface OnDisabled { void run(DarkShadowItem item); }
    public interface OnSwatch   { void run(DarkShadowItem item, int dialogId); }

    private final List<DarkShadowItem> items;
    private final OnEnabled  onEnabled;
    private final OnDisabled onDisabled;
    private final OnSwatch   onSwatch;
    public DarkShadowColorListener(List<DarkShadowItem> items,
                                   OnEnabled e, OnDisabled d, OnSwatch s) {
        this.items      = items;
        this.onEnabled  = e;
        this.onDisabled = d;
        this.onSwatch   = s;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dark_shadow, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DarkShadowItem item = items.get(pos);

        // Color swatch — mutate() isolates the drawable per-view, setColor() changes only the
        // fill; the white stroke defined in circle_swatch.xml is left untouched.
        Drawable bg = h.swatch.getBackground().mutate();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg).setColor(item.getColor());
        }

        // Hex label — show alpha only if not fully opaque
        int c = item.getColor();
        String hex = (Color.alpha(c) == 0xFF)
                ? String.format(Locale.ROOT, "#%06X", c & 0xFFFFFF)
                : String.format(Locale.ROOT, "#%08X", c);
        h.hex.setText(hex);

        // Tap on swatch container → open color picker
        h.swatchContainer.setOnClickListener(v -> onSwatch.run(item, System.identityHashCode(item)));

        h.name.setText(item.getName());

        h.sw.setOnCheckedChangeListener(null);
        h.sw.setChecked(item.isEnabled());
        h.sw.setOnCheckedChangeListener((btn, checked) -> {
            item.setEnabled(checked);
            if (checked) onEnabled.run(item);
            else         onDisabled.run(item);
        });

        // Tap anywhere on the row (except the switch) → open color picker
        h.itemView.setOnClickListener(v -> onSwatch.run(item, System.identityHashCode(item)));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        FrameLayout  swatchContainer;
        View         swatch;
        TextView     name;
        TextView     hex;
        SwitchCompat sw;

        VH(View v) {
            super(v);
            swatch          = v.findViewById(R.id.colorSwatch);
            swatchContainer = (FrameLayout) swatch.getParent();
            name            = v.findViewById(R.id.itemName);
            hex             = v.findViewById(R.id.itemHex);
            sw              = v.findViewById(R.id.itemSwitch);
            sw.setClickable(true);
            sw.setFocusable(true);
        }
    }
}
