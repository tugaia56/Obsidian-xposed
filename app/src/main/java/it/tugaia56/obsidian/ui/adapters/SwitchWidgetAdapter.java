package it.tugaia56.obsidian.ui.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.utils.ObsidianTheme;
import it.tugaia56.obsidian.utils.ObsidianTheme.GroupPos;

public class SwitchWidgetAdapter extends RecyclerView.Adapter<SwitchWidgetAdapter.VH> {

    public static class SwitchItem {
        public final String title;
        public final String summary;
        public final @DrawableRes int iconRes;
        public boolean checked;
        public Runnable onChanged;
        /** Se impostato, il tocco sulla riga (titolo/sfondo) chiama questo invece di
         *  attivare/disattivare lo switch — lo switch resta comunque toccabile per conto suo,
         *  come area separata. Usato per righe "espandibili": lo switch attiva/disattiva la
         *  funzione, il tocco sul nome apre/chiude le opzioni sottostanti. */
        public Runnable onRowClick;
        /** Position within a merged-card row group; SINGLE = standalone pill (default). */
        public GroupPos groupPos = GroupPos.SINGLE;
        /** Lighter/bordered background instead of the normal card colour — for options
         *  revealed by tapping a row above them. */
        public boolean nested = false;

        public SwitchItem(String title, String summary, boolean checked, Runnable onChanged) {
            this(title, summary, 0, checked, onChanged);
        }

        public SwitchItem(String title, String summary, @DrawableRes int iconRes,
                          boolean checked, Runnable onChanged) {
            this.title     = title;
            this.summary   = summary;
            this.iconRes   = iconRes;
            this.checked   = checked;
            this.onChanged = onChanged;
        }
    }

    private final List<SwitchItem> items;

    public SwitchWidgetAdapter(List<SwitchItem> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_widget_switch, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SwitchItem item = items.get(pos);

        // ── DST-based colours + grouped/standalone card background ───────────
        int accent    = ObsidianTheme.accentColor();
        int accentDim = ObsidianTheme.accentDim();
        h.itemView.setBackground(item.nested
                ? ObsidianTheme.nestedGroupBackground(h.itemView.getContext(), item.groupPos)
                : ObsidianTheme.groupBackground(h.itemView.getContext(), item.groupPos));

        if (h.itemView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) h.itemView.getLayoutParams();
            ObsidianTheme.applyGroupMargins(h.itemView.getContext(), lp, item.groupPos);
            h.itemView.setLayoutParams(lp);
        }

        // Icon
        if (item.iconRes != 0) {
            h.icon.setVisibility(View.VISIBLE);
            h.icon.setImageResource(item.iconRes);
            h.icon.setImageTintList(ColorStateList.valueOf(accent));
        } else {
            h.icon.setVisibility(View.GONE);
        }

        // Title
        h.title.setText(item.title);
        h.title.setTextColor(ObsidianTheme.textColor());

        // Summary
        if (item.summary != null && !item.summary.isEmpty()) {
            h.summary.setText(item.summary);
            h.summary.setTextColor(ObsidianTheme.textColor(0x99));
            h.summary.setVisibility(View.VISIBLE);
        } else {
            h.summary.setVisibility(View.GONE);
        }

        // Switch track: accent when ON, dim when OFF
        h.sw.setTrackTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{accent, accentDim}));
        // Thumb stays white
        h.sw.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));

        // Toggle
        h.sw.setOnCheckedChangeListener(null);
        h.sw.setChecked(item.checked);
        h.sw.setOnCheckedChangeListener((btn, checked) -> {
            item.checked = checked;
            if (item.onChanged != null) item.onChanged.run();
        });

        if (item.onRowClick != null) {
            h.sw.setClickable(true);
            h.sw.setFocusable(true);
            h.itemView.setOnClickListener(v -> item.onRowClick.run());
        } else {
            h.sw.setClickable(false);
            h.sw.setFocusable(false);
            h.itemView.setOnClickListener(v -> h.sw.setChecked(!h.sw.isChecked()));
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView    icon;
        TextView     title, summary;
        SwitchCompat sw;

        VH(View v) {
            super(v);
            icon    = v.findViewById(R.id.switchIcon);
            title   = v.findViewById(R.id.switchTitle);
            summary = v.findViewById(R.id.switchSummary);
            sw      = v.findViewById(R.id.switchWidget);
            sw.setClickable(false);
            sw.setFocusable(false);
        }
    }
}
