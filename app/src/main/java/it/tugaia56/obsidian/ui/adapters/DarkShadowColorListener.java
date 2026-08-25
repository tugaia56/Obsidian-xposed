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
import it.tugaia56.obsidian.utils.ObsidianTheme;
import it.tugaia56.obsidian.utils.ObsidianTheme.GroupPos;

public class DarkShadowColorListener extends RecyclerView.Adapter<DarkShadowColorListener.VH> {

    public interface OnEnabled  { void run(DarkShadowItem item); }
    public interface OnDisabled { void run(DarkShadowItem item); }
    public interface OnSwatch   { void run(DarkShadowItem item, int dialogId); }

    private final List<DarkShadowItem> items;
    private final OnEnabled  onEnabled;
    private final OnDisabled onDisabled;
    private final OnSwatch   onSwatch;
    /** true → same lighter/bordered background as GroupUtils.addGroup(..., true) instead of
     *  the static dst_item_bg drawable — for when this grid sits among other "nested" rows
     *  (options revealed by tapping a switch above) and needs to match them visually. */
    private final boolean nested;
    /** When set, every row uses this GroupPos instead of the usual per-index TOP/MIDDLE/
     *  BOTTOM/SINGLE computed from the list's own size — for when this grid is sandwiched
     *  between other nested rows above/below it and needs to render flush (no rounding, no
     *  vertical margin) as a MIDDLE segment of that larger merged block. */
    private final GroupPos forcedPos;

    public DarkShadowColorListener(List<DarkShadowItem> items,
                                   OnEnabled e, OnDisabled d, OnSwatch s) {
        this(items, e, d, s, false, null);
    }

    public DarkShadowColorListener(List<DarkShadowItem> items,
                                   OnEnabled e, OnDisabled d, OnSwatch s, boolean nested) {
        this(items, e, d, s, nested, null);
    }

    public DarkShadowColorListener(List<DarkShadowItem> items,
                                   OnEnabled e, OnDisabled d, OnSwatch s, boolean nested, GroupPos forcedPos) {
        this.items      = items;
        this.onEnabled  = e;
        this.onDisabled = d;
        this.onSwatch   = s;
        this.nested     = nested;
        this.forcedPos  = forcedPos;
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

        if (nested) {
            int n = items.size();
            GroupPos groupPos = forcedPos != null ? forcedPos
                    : n == 1 ? GroupPos.SINGLE
                    : pos == 0 ? GroupPos.TOP
                    : pos == n - 1 ? GroupPos.BOTTOM
                    : GroupPos.MIDDLE;
            h.itemView.setBackground(ObsidianTheme.nestedGroupBackground(h.itemView.getContext(), groupPos));
            if (h.itemView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams lp) {
                ObsidianTheme.applyGroupMargins(h.itemView.getContext(), lp, groupPos);
                h.itemView.setLayoutParams(lp);
            }
        }

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
        h.hex.setTextColor(ObsidianTheme.textColor(0x66));

        h.name.setText(item.getName());
        h.name.setTextColor(ObsidianTheme.textColor());

        h.sw.setTrackTintList(new android.content.res.ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{ObsidianTheme.accentColor(), ObsidianTheme.accentDim()}));
        h.sw.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));

        h.sw.setOnCheckedChangeListener(null);
        h.sw.setChecked(item.isEnabled());
        h.sw.setOnCheckedChangeListener((btn, checked) -> {
            item.setEnabled(checked);
            if (checked) onEnabled.run(item);
            else         onDisabled.run(item);
        });

        // Riga bloccata (es. "Attivo" mentre "Collega all'Accento" è attivo) — attenuata e non
        // toccabile, per non lasciare che il picker prevalga in silenzio sull'accento.
        boolean locked = item.isLocked();
        h.itemView.setAlpha(locked ? 0.4f : 1f);
        h.sw.setEnabled(!locked);
        h.swatchContainer.setEnabled(!locked);
        if (locked) {
            h.swatchContainer.setOnClickListener(null);
            h.swatchContainer.setClickable(false);
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
        } else {
            // Tap on swatch container → open color picker
            h.swatchContainer.setOnClickListener(v -> onSwatch.run(item, System.identityHashCode(item)));
            // Tap anywhere on the row (except the switch) → open color picker
            h.itemView.setOnClickListener(v -> onSwatch.run(item, System.identityHashCode(item)));
        }
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
