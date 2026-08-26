package it.tugaia56.obsidian.ui.fragments;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.databinding.FragmentDstBinding;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.DarkShadowColorListener;
import it.tugaia56.obsidian.ui.adapters.NavAdapter;
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.ui.models.DarkShadowItem;
import it.tugaia56.obsidian.utils.AppUtils;
import it.tugaia56.obsidian.utils.DarkShadowUtils;
import it.tugaia56.obsidian.utils.DstFabricatedUtil;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;

/**
 * Preset Accento — split out of DarkShadowThemeFragment (was one combined screen,
 * now its own screen reached from the DST Colori hub, mirroring Schermata di Blocco's
 * hub → dedicated-screen pattern).
 */
public class DstAccentFragment extends Fragment {

    private static final String PREF_AC_PRESET = "DST_AC_PRESET_NAME";

    private static final String[] AC_NAMES = {
        "Blue", "Blue Android", "Blue Gray 300", "Blue Gray 400", "Blue Gray 500",
        "Blue Gray 600", "Blue Gray 700", "Blue Green", "Coral", "Coral Vivid",
        "Crayola", "Cyano", "Deep Purple", "Fuchsia", "Fuchsia Light",
        "Gray 400", "Gray 500", "Green Android", "Green Dark", "Green Fluorescent",
        "Green Fluorish", "Green New", "Indigo", "Lime", "McLaren",
        "Orange", "Orange Android", "Pale Blue", "Pink", "Pixel Blue",
        "Purple", "Purple Dark", "Purple Fluorescent", "Red", "Red Dark",
        "Red Deep", "Red Deep Dark", "Red Umbrella", "Sky Blue", "Star Wars",
        "Taupe", "Teal", "Teal Dark", "Turquoise", "Violet",
        "Violet Dark", "Yellow", "Yellow Dark",
    };
    private static final int[] AC_COLORS = {
        0xFF0097ff, 0xFF4285f4, 0xFF90A4AE, 0xFF78909C, 0xFF607d8b,
        0xFF546e7a, 0xFF455a64, 0xFF2E61F5, 0xFFEF5350, 0xFFff404c,
        0xFFFBA723, 0xFF0097A7, 0xFF78038C, 0xFFC51162, 0xFFe70ca5,
        0xFFbdbdbd, 0xFF9E9E9E, 0xFF3ddc84, 0xFF557a52, 0xFF80ff00,
        0xFF1cff12, 0xFF7DB695, 0xFF304FFE, 0xFF9dd200, 0xFFff7514,
        0xFFE0610E, 0xFFf86734, 0xFFA1B6ED, 0xFFF39DCC, 0xFF5e97f6,
        0xFF880e4f, 0xFF6200EA, 0xFFd401e9, 0xFFff0000, 0xFFd50000,
        0xFFcc0000, 0xFFbb0000, 0xFFa92c2c, 0xFF2962FF, 0xFFff2837,
        0xFFB37AA3, 0xFF00897b, 0xFF00695c, 0xFF26a69a, 0xFF908dff,
        0xFF7268fc, 0xFFffd600, 0xFFffc107,
    };

    private FragmentDstBinding binding;
    private List<DarkShadowItem> accentItems;
    private DarkShadowItem bgItemForDialogBg; // only used to colour dialog backgrounds
    private DarkShadowColorListener accentAdapter;
    private PresetRowAdapter presetAdapter;
    private int mAcCustomDialogId;
    private int pendingDialogId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAcCustomDialogId = View.generateViewId();
        // colorItems order: [ACCENT1(0), ACCENT2(1), ACCENT3(2), BACKGROUND(3)]
        List<DarkShadowItem> all = DarkShadowUtils.getItems(requireContext());
        accentItems = all.subList(0, 3);
        bgItemForDialogBg = all.get(3);

        presetAdapter = new PresetRowAdapter();
        accentAdapter = new DarkShadowColorListener(
                accentItems, this::onItemEnabled, this::onItemDisabled, this::onColorSwatch);

        NavAdapter systemColorNav = new NavAdapter(List.of(
                new NavAdapter.NavItem(
                        R.drawable.ic_palette,
                        getString(R.string.nav_system_colors),
                        getString(R.string.nav_system_colors_summary),
                        () -> navigate(new SystemColorsFragment(), getString(R.string.nav_system_colors)),
                        0xFFE91E63, // pink
                        "monet", "colore", "color", "accento", "accent",
                        "sistema", "system", "material you")
        ));

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setBackground(null);
        binding.recyclerView.setAdapter(new ConcatAdapter(
                new SectionTitleAdapter(List.of(getString(R.string.dst_section_colore_preset))),
                presetAdapter,
                new SectionTitleAdapter(List.of(getString(R.string.dst_section_preset_accent))),
                accentAdapter,
                systemColorNav
        ));
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(fragment, title);
        }
    }

    // ── Preset dialog ─────────────────────────────────────────────────────────

    private void showPresetDialog() {
        DarkShadowItem accent1 = accentItems.get(0);
        String[] dialogNames = new String[AC_NAMES.length + 1];
        dialogNames[0] = getString(R.string.dst_preset_custom);
        System.arraycopy(AC_NAMES, 0, dialogNames, 1, AC_NAMES.length);

        String current = ObsidianPrefs.getString(PREF_AC_PRESET, null);
        int currentIdx = -1;
        if ("Custom".equals(current)) {
            currentIdx = 0;
        } else if (current != null) {
            for (int i = 0; i < AC_NAMES.length; i++) {
                if (AC_NAMES[i].equals(current)) { currentIdx = i + 1; break; }
            }
        }

        final int[] selected = {currentIdx};
        ArrayAdapter<String> listAdapter = buildPresetListAdapter(dialogNames, AC_COLORS, accent1.getColor(), selected);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dst_section_preset_accent)
                .setSingleChoiceItems(listAdapter, currentIdx,
                        (d, which) -> { selected[0] = which; listAdapter.notifyDataSetChanged(); })
                .setPositiveButton(R.string.apply, (d, w) -> {
                    if (selected[0] < 0) return;
                    if (selected[0] == 0) {
                        pendingDialogId = mAcCustomDialogId;
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showColorPickerDialog(
                                    mAcCustomDialogId, accent1.getColor(), true, true, true);
                        }
                    } else {
                        accent1.setColor(AC_COLORS[selected[0] - 1]);
                        accent1.setEnabled(true);
                        DarkShadowUtils.saveColor(accent1);
                        ObsidianPrefs.putString(PREF_AC_PRESET, AC_NAMES[selected[0] - 1]);
                        notifyAdapters();
                        DstFabricatedUtil.applyThenRun(accent1, () -> AppUtils.showRestartReminder(requireContext()));
                    }
                })
                .setNeutralButton(R.string.dst_disable, (d, w) -> {
                    onItemDisabled(accent1);
                    ObsidianPrefs.putString(PREF_AC_PRESET, null);
                    if (presetAdapter != null) presetAdapter.notifyDataSetChanged();
                })
                .setNegativeButton(R.string.close, null)
                .show();
        applyDialogBg(dialog);
        fixButtonCaps(dialog);
    }

    // ── Color toggle callbacks ────────────────────────────────────────────────

    private void onItemEnabled(DarkShadowItem item) {
        item.setEnabled(true);
        DarkShadowUtils.saveColor(item);
        DstFabricatedUtil.applyThenRun(item, () -> AppUtils.showRestartReminder(requireContext()));
    }

    private void onItemDisabled(DarkShadowItem item) {
        item.setEnabled(false);
        ObsidianPrefs.putBoolean(DarkShadowUtils.PREF_PREFIX + item.getOverlayName() + "_on", false);
        DstFabricatedUtil.disableThenRun(item, () -> AppUtils.showRestartReminder(requireContext()));
    }

    private void onColorSwatch(DarkShadowItem item, int dialogId) {
        pendingDialogId = dialogId;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showColorPickerDialog(
                    dialogId, item.getColor(), true, true, true);
        }
    }

    // ── EventBus ──────────────────────────────────────────────────────────────

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        int id    = event.dialogId();
        int color = event.color();

        if (id == mAcCustomDialogId) {
            DarkShadowItem accent1 = accentItems.get(0);
            accent1.setColor(color);
            accent1.setEnabled(true);
            DarkShadowUtils.saveColor(accent1);
            ObsidianPrefs.putString(PREF_AC_PRESET, getString(R.string.dst_preset_custom));
            notifyAdapters();
            DstFabricatedUtil.applyThenRun(accent1, () -> AppUtils.showRestartReminder(requireContext()));

        } else if (id == pendingDialogId) {
            for (DarkShadowItem item : accentItems) {
                if (System.identityHashCode(item) == id) {
                    item.setColor(color);
                    DarkShadowUtils.saveColor(item);
                    if (item.isEnabled()) {
                        DstFabricatedUtil.applyThenRun(item, () -> AppUtils.showRestartReminder(requireContext()));
                    }
                    break;
                }
            }
            notifyAdapters();
        }
    }

    private void notifyAdapters() {
        if (presetAdapter != null) presetAdapter.notifyDataSetChanged();
        if (accentAdapter != null) accentAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }

    @Override
    public void onDestroy() { super.onDestroy(); EventBus.getDefault().unregister(this); }

    // ── Preset row adapter (single row: Accento) ─────────────────────────────

    private class PresetRowAdapter extends RecyclerView.Adapter<PresetRowAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dark_shadow, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DarkShadowItem accent1 = accentItems.get(0);
            h.sw.setVisibility(View.GONE);
            Drawable swBg = h.swatch.getBackground().mutate();
            if (swBg instanceof GradientDrawable) {
                ((GradientDrawable) swBg).setColor(accent1.getColor());
            }
            h.swatchContainer.setVisibility(View.VISIBLE);
            h.swatchContainer.setOnClickListener(v -> showPresetDialog());
            h.name.setText(getString(R.string.dst_section_preset_accent));
            h.name.setTextColor(ObsidianTheme.textColor());
            String saved = ObsidianPrefs.getString(PREF_AC_PRESET, null);
            h.hex.setText(saved != null ? saved : getString(R.string.dst_none));
            h.hex.setTextColor(ObsidianTheme.textColor(0x66));
            h.itemView.setOnClickListener(v -> showPresetDialog());
        }

        @Override public int getItemCount() { return 1; }

        class VH extends RecyclerView.ViewHolder {
            FrameLayout  swatchContainer;
            View         swatch;
            TextView     name, hex;
            SwitchCompat sw;

            VH(View v) {
                super(v);
                swatch          = v.findViewById(R.id.colorSwatch);
                swatchContainer = (FrameLayout) swatch.getParent();
                name            = v.findViewById(R.id.itemName);
                hex             = v.findViewById(R.id.itemHex);
                sw              = v.findViewById(R.id.itemSwitch);
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Preset list rows show a colored dot matching the preset's actual color (like
     * Substratum) instead of a plain radio button. names[0] is "Custom" — it gets
     * customColor (the current live color) since it has no fixed entry in colors[].
     */
    private ArrayAdapter<String> buildPresetListAdapter(String[] names, int[] colors, int customColor, int[] selectedRef) {
        int dot = dp(24);
        int pad = dp(16);
        return new ArrayAdapter<String>(requireContext(), 0, names) {
            @NonNull @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                ImageView dotView;
                TextView label;
                LinearLayout row;
                if (convertView instanceof LinearLayout) {
                    row = (LinearLayout) convertView;
                    dotView = (ImageView) row.getChildAt(0);
                    label = (TextView) row.getChildAt(1);
                } else {
                    row = new LinearLayout(getContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(pad, pad, pad, pad);

                    dotView = new ImageView(getContext());
                    LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dot, dot);
                    dotLp.setMarginEnd(pad);
                    dotView.setLayoutParams(dotLp);
                    GradientDrawable circle = new GradientDrawable();
                    circle.setShape(GradientDrawable.OVAL);
                    dotView.setBackground(circle);
                    row.addView(dotView);

                    label = new TextView(getContext());
                    label.setTextSize(16);
                    row.addView(label);
                }
                int color = (position == 0) ? customColor : colors[position - 1];
                ((GradientDrawable) dotView.getBackground()).setColor(color);
                label.setText(getItem(position));
                label.setTextColor(position == selectedRef[0] ? ObsidianTheme.accentColor() : ObsidianTheme.textColor());
                return row;
            }
        };
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static void fixButtonCaps(AlertDialog d) {
        Button pos = d.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neg = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neu = d.getButton(AlertDialog.BUTTON_NEUTRAL);
        // Forcing minWidth to 0 squeezed "Disabilita" so hard it broke mid-word instead of
        // wrapping — leave the natural min-width so Android's button bar can stack the
        // buttons vertically when 3 don't fit on one row.
        for (Button b : new Button[]{pos, neg, neu}) {
            if (b == null) continue;
            b.setAllCaps(false);
            b.setSingleLine(false);
            b.setMaxLines(2);
            b.setEllipsize(null);
        }
    }

    private void applyDialogBg(AlertDialog d) {
        if (d.getWindow() == null) return;
        if (bgItemForDialogBg != null && bgItemForDialogBg.isEnabled()) {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(bgItemForDialogBg.getColor());
            float r = 28 * getResources().getDisplayMetrics().density;
            bg.setCornerRadius(r);
            d.getWindow().setBackgroundDrawable(bg);
        } else {
            d.getWindow().setBackgroundDrawable(it.tugaia56.obsidian.utils.ObsidianTheme.dialogBackground(requireContext()));
        }
    }
}
