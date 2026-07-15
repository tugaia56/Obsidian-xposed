package it.tugaia56.obsidian.ui.adapters;

import android.util.TypedValue;
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

public class SwitchWidgetAdapter extends RecyclerView.Adapter<SwitchWidgetAdapter.VH> {

    public static class SwitchItem {
        public final String title;
        public final String summary;
        public final @DrawableRes int iconRes; // 0 = no icon
        public boolean checked;
        public Runnable onChanged;

        /** Without icon */
        public SwitchItem(String title, String summary, boolean checked, Runnable onChanged) {
            this(title, summary, 0, checked, onChanged);
        }

        /** With icon */
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

        // Icon
        if (item.iconRes != 0) {
            h.icon.setVisibility(View.VISIBLE);
            h.icon.setImageResource(item.iconRes);
            // Tint with colorPrimary
            TypedValue tv = new TypedValue();
            h.itemView.getContext().getTheme()
                    .resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
            h.icon.setImageTintList(
                    android.content.res.ColorStateList.valueOf(tv.data));
        } else {
            h.icon.setVisibility(View.GONE);
        }

        // Title
        h.title.setText(item.title);

        // Summary
        if (item.summary != null && !item.summary.isEmpty()) {
            h.summary.setText(item.summary);
            h.summary.setVisibility(View.VISIBLE);
        } else {
            h.summary.setVisibility(View.GONE);
        }

        // Switch
        h.sw.setOnCheckedChangeListener(null);
        h.sw.setChecked(item.checked);
        h.sw.setOnCheckedChangeListener((btn, checked) -> {
            item.checked = checked;
            if (item.onChanged != null) item.onChanged.run();
        });

        h.itemView.setOnClickListener(v -> h.sw.setChecked(!h.sw.isChecked()));
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
