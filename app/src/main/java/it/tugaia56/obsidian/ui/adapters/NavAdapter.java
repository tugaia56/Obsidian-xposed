package it.tugaia56.obsidian.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        public final int accentColor;  // -1 = use position default
        public final String[] keywords; // extra searchable keywords, not displayed

        public NavItem(@DrawableRes int iconRes, String title, String subtitle, Runnable onClick) {
            this(iconRes, title, subtitle, onClick, -1);
        }

        public NavItem(@DrawableRes int iconRes, String title, String subtitle,
                       Runnable onClick, int accentColor, String... keywords) {
            this.iconRes     = iconRes;
            this.title       = title;
            this.subtitle    = subtitle;
            this.onClick     = onClick;
            this.accentColor = accentColor;
            this.keywords    = keywords != null ? keywords : new String[0];
        }

        public NavItem(@DrawableRes int iconRes, String title, String subtitle,
                       Runnable onClick, String... keywords) {
            this(iconRes, title, subtitle, onClick, -1, keywords);
        }
    }

    private final List<NavItem> fullItems;
    private List<NavItem> filteredItems;

    public NavAdapter(List<NavItem> items) {
        this.fullItems      = new ArrayList<>(items);
        this.filteredItems  = new ArrayList<>(items);
    }

    /**
     * Filter items by query — matches title or subtitle, case/locale-insensitive.
     * Empty query restores the full list.
     */
    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredItems = new ArrayList<>(fullItems);
        } else {
            String q = query.toLowerCase(Locale.getDefault()).trim();
            filteredItems = new ArrayList<>();
            for (NavItem item : fullItems) {
                boolean match = (item.title    != null && item.title.toLowerCase(Locale.getDefault()).contains(q))
                             || (item.subtitle != null && item.subtitle.toLowerCase(Locale.getDefault()).contains(q));
                if (!match) {
                    for (String kw : item.keywords) {
                        if (kw != null && kw.toLowerCase(Locale.getDefault()).contains(q)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) filteredItems.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nav, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        NavItem item = filteredItems.get(pos);

        // Per-item accent colour — item can override the positional default
        int accent = (item.accentColor != -1) ? item.accentColor
                   : (pos < ACCENT_COLORS.length ? ACCENT_COLORS[pos] : 0xFF6200EE);

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

        h.card.setCardBackgroundColor(it.tugaia56.obsidian.utils.ObsidianTheme.cardColor());
        h.title.setText(item.title);
        h.title.setTextColor(it.tugaia56.obsidian.utils.ObsidianTheme.textColor());

        if (item.subtitle != null && !item.subtitle.isEmpty()) {
            h.subtitle.setVisibility(View.VISIBLE);
            h.subtitle.setText(item.subtitle);
            h.subtitle.setTextColor(it.tugaia56.obsidian.utils.ObsidianTheme.textColor(0x99));
        } else {
            h.subtitle.setVisibility(View.GONE);
        }

        if (h.chevron != null) {
            h.chevron.setTextColor(it.tugaia56.obsidian.utils.ObsidianTheme.textColor(0x66));
        }

        h.itemView.setOnClickListener(v -> { if (item.onClick != null) item.onClick.run(); });
    }

    @Override public int getItemCount() { return filteredItems.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final FrameLayout iconContainer;
        final ImageView icon;
        final TextView title, subtitle, chevron;

        VH(View v) {
            super(v);
            card          = (MaterialCardView) v;
            iconContainer = v.findViewById(R.id.navIconContainer);
            icon          = v.findViewById(R.id.navIcon);
            title         = v.findViewById(R.id.navTitle);
            subtitle      = v.findViewById(R.id.navSubtitle);
            chevron       = v.findViewById(R.id.navChevron);
        }
    }

    // ── Search bar ────────────────────────────────────────────────────────────

    /**
     * Returns a single-item adapter that renders a pill-shaped search bar
     * wired to this NavAdapter's {@link #filter(String)} method.
     *
     * The bar is styled with a GradientDrawable (transparent fill + white
     * border at ~30% alpha), so it adapts to any background including DST
     * substratums without depending on colour resources.
     *
     * Usage:
     * <pre>
     *   NavAdapter nav = new NavAdapter(items);
     *   rv.setAdapter(new ConcatAdapter(headerAdapter, nav.createSearchAdapter(hint), nav));
     * </pre>
     */
    public RecyclerView.Adapter<SearchBarVH> createSearchAdapter(String hint) {
        NavAdapter self = this;
        return new RecyclerView.Adapter<SearchBarVH>() {
            @NonNull @Override
            public SearchBarVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return SearchBarVH.create(parent, self, hint);
            }
            @Override public void onBindViewHolder(@NonNull SearchBarVH h, int pos) {}
            @Override public int getItemCount() { return 1; }
        };
    }

    /**
     * ViewHolder for the search bar.  Created programmatically — no XML layout.
     */
    public static class SearchBarVH extends RecyclerView.ViewHolder {

        SearchBarVH(View v) { super(v); }

        static SearchBarVH create(ViewGroup parent, NavAdapter navAdapter, String hint) {
            Context ctx = parent.getContext();
            float dp = ctx.getResources().getDisplayMetrics().density;
            int p16 = Math.round(16 * dp);
            int p10 = Math.round(10 * dp);
            int p8  = Math.round(8  * dp);
            int p4  = Math.round(4  * dp);

            // Pill-shaped border drawable: semi-transparent fill + border, both derived
            // from the theme's text colour so the pill stays visible in light mode too.
            GradientDrawable border = new GradientDrawable();
            border.setShape(GradientDrawable.RECTANGLE);
            border.setCornerRadius(28 * dp);
            border.setColor(it.tugaia56.obsidian.utils.ObsidianTheme.textColor(20));  // ~8% fill
            border.setStroke(Math.round(1.5f * dp),
                             it.tugaia56.obsidian.utils.ObsidianTheme.textColor(80));  // ~31% border

            EditText et = new EditText(ctx);
            et.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            et.setBackground(border);
            et.setHint(hint);
            et.setHintTextColor(it.tugaia56.obsidian.utils.ObsidianTheme.textColor(128));
            et.setTextColor(it.tugaia56.obsidian.utils.ObsidianTheme.textColor());
            et.setSingleLine(true);
            et.setPadding(p16, p10, p16, p10);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            et.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_FULLSCREEN);

            // Search icon as start compound drawable
            try {
                android.graphics.drawable.Drawable ic = ctx.getDrawable(R.drawable.ic_search);
                if (ic != null) {
                    ic = ic.mutate();
                    ic.setTint(it.tugaia56.obsidian.utils.ObsidianTheme.textColor(180));
                    int sz = Math.round(18 * dp);
                    ic.setBounds(0, 0, sz, sz);
                    et.setCompoundDrawablesRelative(ic, null, null, null);
                    et.setCompoundDrawablePadding(p8);
                }
            } catch (Throwable ignored) {}

            et.setOnEditorActionListener((v, actionId, event) -> {
                // Consume the search action — without this Android invokes SearchManager
                // which crashes if the activity is not declared as searchable.
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    InputMethodManager imm = (InputMethodManager)
                            ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            });

            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    navAdapter.filter(s.toString());
                }
            });

            FrameLayout container = new FrameLayout(ctx);
            container.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            container.setPadding(p16, p4, p16, p8);
            container.addView(et);

            return new SearchBarVH(container);
        }
    }
}
