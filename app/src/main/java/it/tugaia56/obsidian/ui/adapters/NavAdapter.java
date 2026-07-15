package it.tugaia56.obsidian.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import it.tugaia56.obsidian.R;

/**
 * Adapter for home-screen navigation cards.
 * Each card shows a coloured icon container, a title, and an optional subtitle.
 */
public class NavAdapter extends RecyclerView.Adapter<NavAdapter.VH> {

    /**
     * Icon accent colours — one per nav item position.
     * These are ARGB values with 25% fill + full-opacity border drawn in code.
     */
    private static final int[] ACCENT_COLORS = {
            0xFF7C4DFF,  // DST Colors       — purple
            0xFFE91E63,  // System Colors     — pink
            0xFF00BCD4,  // Quick Settings    — cyan
            0xFF4CAF50,  // Lock Screen       — green
            0xFFFF9800,  // Statusbar         — amber
    };

    public static class NavItem {
        public final @DrawableRes int iconRes;
        public final String title;
        public final String subtitle; // nullable
        public final Runnable onClick;

        public NavItem(@DrawableRes int iconRes, String title, String subtitle, Runnable onClick) {
            this.iconRes  = iconRes;
            this.title    = title;
            this.subtitle = subtitle;
            this.onClick  = onClick;
        }
    }

    private final List<NavItem> items;

    public NavAdapter(List<NavItem> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nav, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        NavItem item = items.get(pos);

        // Per-item accent colour
        int accent = pos < ACCENT_COLORS.length ? ACCENT_COLORS[pos] : 0xFF6200EE;

        float dp = h.itemView.getContext().getResources().getDisplayMetrics().density;

        // Card border: accent color at ~35% alpha
        int strokeColor = Color.argb(204, Color.red(accent), Color.green(accent), Color.blue(accent));
        h.card.setStrokeColor(strokeColor);

        // Icon container: rounded square, 18% alpha fill + accent stroke
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(14 * dp);
        bg.setColor(Color.argb(46, Color.red(accent), Color.green(accent), Color.blue(accent))); // ~18%
        bg.setStroke(Math.round(1.5f * dp), accent);
        h.iconContainer.setBackground(bg);

        h.icon.setImageResource(item.iconRes);
        h.icon.setImageTintList(android.content.res.ColorStateList.valueOf(accent));

        h.title.setText(item.title);

        if (item.subtitle != null && !item.subtitle.isEmpty()) {
            h.subtitle.setVisibility(View.VISIBLE);
            h.subtitle.setText(item.subtitle);
        } else {
            h.subtitle.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> { if (item.onClick != null) item.onClick.run(); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final FrameLayout iconContainer;
        final ImageView icon;
        final TextView title, subtitle;

        VH(View v) {
            super(v);
            card          = (MaterialCardView) v;
            iconContainer = v.findViewById(R.id.navIconContainer);
            icon          = v.findViewById(R.id.navIcon);
            title         = v.findViewById(R.id.navTitle);
            subtitle      = v.findViewById(R.id.navSubtitle);
        }
    }
}
