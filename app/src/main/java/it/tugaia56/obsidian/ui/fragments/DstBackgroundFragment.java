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
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.ui.models.DarkShadowItem;
import it.tugaia56.obsidian.utils.AppUtils;
import it.tugaia56.obsidian.utils.DarkShadowUtils;
import it.tugaia56.obsidian.utils.DstFabricatedUtil;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;

/**
 * Preset Sfondo — split out of DarkShadowThemeFragment (was one combined screen,
 * now its own screen reached from the DST Colori hub, mirroring Schermata di Blocco's
 * hub → dedicated-screen pattern).
 */
public class DstBackgroundFragment extends Fragment {

    private static final String PREF_BG_PRESET = "DST_BG_PRESET_NAME";

    private static final String[] BG_NAMES = {
        "Black Amoled", "Blue Gray", "Blue Gray Dark", "Dark Blue", "Dark Gray",
        "Dark Green", "Dark Pink", "Dark Purple", "Dark Steel", "Deep Purple",
        "Eerie", "Green", "Grey", "Grey Night", "Light Grey", "Night", "Onyx",
        "Taupe", "Transparency Crazy Full", "Transparency Higher",
        "Transparency Lower", "Transparency Medium",
    };
    private static final int[] BG_COLORS = {
        0xFF000000, 0xFF212232, 0xFF1b2029, 0xFF0a1236, 0xFF202026,
        0xFF001413, 0xFF270520, 0xFF1D0021, 0xFF3b4250, 0xFF1C0839,
        0xFF161117, 0xFF192f2d, 0xFF2B2E37, 0xFF353535, 0xFF2f333b,
        0xFF363844, 0xFF2D232C, 0xFF0e0e0e, 0x00000000, 0x40000000,
        0x80000000, 0x61000000,
    };

    private FragmentDstBinding binding;
    private DarkShadowItem bgItem;
    private DarkShadowColorListener bgAdapter;
    private PresetRowAdapter presetAdapter;
    private int mBgCustomDialogId;
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

        mBgCustomDialogId = View.generateViewId();
        bgItem = DarkShadowUtils.getItems(requireContext()).get(3); // BACKGROUND

        presetAdapter = new PresetRowAdapter();
        bgAdapter = new DarkShadowColorListener(
                List.of(bgItem), this::onItemEnabled, this::onItemDisabled, this::onColorSwatch);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setBackground(null);
        binding.recyclerView.setAdapter(new ConcatAdapter(
                new SectionTitleAdapter(List.of(getString(R.string.dst_section_colore_preset))),
                presetAdapter,
                new SectionTitleAdapter(List.of(getString(R.string.dst_section_preset_sfondo))),
                bgAdapter
        ));
    }

    // ── Preset dialog ─────────────────────────────────────────────────────────

    private void showPresetDialog() {
        String[] dialogNames = new String[BG_NAMES.length + 1];
        dialogNames[0] = getString(R.string.dst_preset_custom);
        System.arraycopy(BG_NAMES, 0, dialogNames, 1, BG_NAMES.length);

        String current = ObsidianPrefs.getString(PREF_BG_PRESET, null);
        int currentIdx = -1;
        if ("Custom".equals(current)) {
            currentIdx = 0;
        } else if (current != null) {
            for (int i = 0; i < BG_NAMES.length; i++) {
                if (BG_NAMES[i].equals(current)) { currentIdx = i + 1; break; }
            }
        }

        final int[] selected = {currentIdx};
        ArrayAdapter<String> listAdapter = buildPresetListAdapter(dialogNames, BG_COLORS, bgItem.getColor(), selected);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dst_section_preset_sfondo)
                .setSingleChoiceItems(listAdapter, currentIdx,
                        (d, which) -> { selected[0] = which; listAdapter.notifyDataSetChanged(); })
                .setPositiveButton(R.string.apply, (d, w) -> {
                    if (selected[0] < 0) return;
                    if (selected[0] == 0) {
                        pendingDialogId = mBgCustomDialogId;
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showColorPickerDialog(
                                    mBgCustomDialogId, bgItem.getColor(), true, true, true);
                        }
                    } else {
                        bgItem.setColor(BG_COLORS[selected[0] - 1]);
                        bgItem.setEnabled(true);
                        DarkShadowUtils.saveColor(bgItem);
                        ObsidianPrefs.putString(PREF_BG_PRESET, BG_NAMES[selected[0] - 1]);
                        notifyAdapters();
                        DstFabricatedUtil.applyThenRun(bgItem, () -> AppUtils.showRestartReminder(requireContext()));
                    }
                })
                .setNeutralButton(R.string.dst_disable, (d, w) -> {
                    onItemDisabled(bgItem);
                    ObsidianPrefs.putString(PREF_BG_PRESET, null);
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

        if (id == mBgCustomDialogId) {
            bgItem.setColor(color);
            bgItem.setEnabled(true);
            DarkShadowUtils.saveColor(bgItem);
            ObsidianPrefs.putString(PREF_BG_PRESET, getString(R.string.dst_preset_custom));
            notifyAdapters();
            DstFabricatedUtil.applyThenRun(bgItem, () -> AppUtils.showRestartReminder(requireContext()));

        } else if (id == pendingDialogId && id == System.identityHashCode(bgItem)) {
            bgItem.setColor(color);
            DarkShadowUtils.saveColor(bgItem);
            if (bgItem.isEnabled()) {
                DstFabricatedUtil.applyThenRun(bgItem, () -> AppUtils.showRestartReminder(requireContext()));
            }
            notifyAdapters();
        }
    }

    private void notifyAdapters() {
        if (presetAdapter != null) presetAdapter.notifyDataSetChanged();
        if (bgAdapter      != null) bgAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }

    @Override
    public void onDestroy() { super.onDestroy(); EventBus.getDefault().unregister(this); }

    // ── Preset row adapter (single row: Sfondo) ──────────────────────────────

    private class PresetRowAdapter extends RecyclerView.Adapter<PresetRowAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dark_shadow, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.sw.setVisibility(View.GONE);
            Drawable swBg = h.swatch.getBackground().mutate();
            if (swBg instanceof GradientDrawable) {
                ((GradientDrawable) swBg).setColor(bgItem.getColor());
            }
            h.swatchContainer.setVisibility(View.VISIBLE);
            h.swatchContainer.setOnClickListener(v -> showPresetDialog());
            h.name.setText(getString(R.string.dst_section_preset_sfondo));
            h.name.setTextColor(ObsidianTheme.textColor());
            String saved = ObsidianPrefs.getString(PREF_BG_PRESET, null);
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
        if (bgItem.isEnabled()) {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(bgItem.getColor());
            float r = 28 * getResources().getDisplayMetrics().density;
            bg.setCornerRadius(r);
            d.getWindow().setBackgroundDrawable(bg);
        } else {
            d.getWindow().setBackgroundDrawable(it.tugaia56.obsidian.utils.ObsidianTheme.dialogBackground(requireContext()));
        }
    }
}
