package it.tugaia56.obsidian.ui.fragments;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
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

import static it.tugaia56.obsidian.utils.DarkShadowUtils.PREF_PREFIX;

/**
 * DST Colors screen.
 *
 * Colors are applied via XResources.setReplacement() in MonetFreeze.
 * Preset section mirrors OC's presets (same names + colors).
 */
public class DarkShadowThemeFragment extends Fragment {

    // ── Preset: pref keys ─────────────────────────────────────────────────────
    private static final String PREF_BG_PRESET  = "DST_BG_PRESET_NAME";
    private static final String PREF_AC_PRESET  = "DST_AC_PRESET_NAME";
    // PREF_DLG_PRESET moved to StatusbarFragment

    // ── Background presets (identical to OC) ──────────────────────────────────
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

    // ── Accent presets (identical to OC) ─────────────────────────────────────
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

    // ── State ─────────────────────────────────────────────────────────────────
    private FragmentDstBinding binding;
    private final List<DarkShadowItem> colorItems = new ArrayList<>();
    private DarkShadowColorListener accentAdapter;
    private DarkShadowColorListener bgAdapter;
    private DstPresetAdapter presetAdapter;
    /** dialog ID for the swatch color-picker (from DarkShadowColorListener) */
    private int pendingDialogId = -1;
    /** dialog IDs for the "Custom" option inside accent/background preset dialogs */
    private int mBgCustomDialogId;
    private int mAcCustomDialogId;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
        mAcCustomDialogId = View.generateViewId();

        colorItems.clear();
        colorItems.addAll(DarkShadowUtils.getItems(requireContext()));

        // colorItems order: [ACCENT1(0), ACCENT2(1), ACCENT3(2), BACKGROUND(3)]
        // Dialog style is handled by DstDialogStyle Xposed hook (no DarkShadowItem needed)
        List<DarkShadowItem> accentItems = colorItems.subList(0, 3);
        List<DarkShadowItem> bgItems     = colorItems.subList(3, 4);

        presetAdapter = new DstPresetAdapter();
        accentAdapter = new DarkShadowColorListener(
                accentItems, this::onItemEnabled, this::onItemDisabled, this::onColorSwatch);
        bgAdapter = new DarkShadowColorListener(
                bgItems, this::onItemEnabled, this::onItemDisabled, this::onColorSwatch);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setBackground(null);
        binding.recyclerView.setAdapter(new ConcatAdapter(
                // Preset pickers (Sfondo + Accento)
                new SectionTitleAdapter(List.of(getString(R.string.dst_section_colore_preset))),
                presetAdapter,
                // Fine-grained color swatches (ACCENT1/2/3 + BACKGROUND)
                new SectionTitleAdapter(List.of(getString(R.string.dst_section_preset_accent))),
                accentAdapter,
                new SectionTitleAdapter(List.of(getString(R.string.dst_section_preset_sfondo))),
                bgAdapter
        ));
    }

    // ── Preset dialog ─────────────────────────────────────────────────────────

    /**
     * Shows a preset-picker dialog for Background (isBg=true) or Accent1 (isBg=false).
     * First entry is always "Personalizzato/Custom" → opens the color picker.
     * Neutral "Disabilita" removes the Xposed hook for that item.
     */
    private void showPresetDialog(boolean isBg) {
        String[] names  = isBg ? BG_NAMES : AC_NAMES;
        int[]    colors = isBg ? BG_COLORS : AC_COLORS;
        DarkShadowItem item = isBg ? colorItems.get(3) : colorItems.get(0);
        String  prefKey = isBg ? PREF_BG_PRESET : PREF_AC_PRESET;
        int customId    = isBg ? mBgCustomDialogId : mAcCustomDialogId;
        String  title   = getString(isBg
                ? R.string.dst_section_preset_sfondo
                : R.string.dst_section_preset_accent);

        // Prepend "Custom" to the preset list
        String[] dialogNames = new String[names.length + 1];
        dialogNames[0] = getString(R.string.dst_preset_custom);
        System.arraycopy(names, 0, dialogNames, 1, names.length);

        // Find currently selected index
        String current = ObsidianPrefs.getString(prefKey, null);
        int currentIdx = -1;
        if ("Custom".equals(current)) {
            currentIdx = 0;
        } else if (current != null) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(current)) { currentIdx = i + 1; break; }
            }
        }

        final int[] selected = {currentIdx};
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(dialogNames, currentIdx,
                        (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    if (selected[0] < 0) return;
                    if (selected[0] == 0) {
                        // Open color picker; result handled in onColorSelected
                        pendingDialogId = customId;
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showColorPickerDialog(
                                    customId, item.getColor(), false, true, true);
                        }
                    } else {
                        // Apply preset immediately
                        item.setColor(colors[selected[0] - 1]);
                        item.setEnabled(true);
                        DarkShadowUtils.saveColor(item);
                        ObsidianPrefs.putString(prefKey, names[selected[0] - 1]);
                        notifyAdapters();
                        DstFabricatedUtil.applyThenRun(item, AppUtils::restartSystemUI);
                    }
                })
                .setNeutralButton(R.string.dst_disable, (d, w) -> {
                    onItemDisabled(item);
                    ObsidianPrefs.putString(prefKey, null);
                    if (presetAdapter != null) presetAdapter.notifyDataSetChanged();
                })
                .setNegativeButton(R.string.close, null)
                .show();
        fixButtonCaps(dialog);
        applyDialogBg(dialog);
    }

    // ── Color toggle callbacks ────────────────────────────────────────────────

    private void onItemEnabled(DarkShadowItem item) {
        item.setEnabled(true);
        DarkShadowUtils.saveColor(item);
        // Fabricate THEN restart — restart must happen after all shell cmds finish
        DstFabricatedUtil.applyThenRun(item, AppUtils::restartSystemUI);
    }

    private void onItemDisabled(DarkShadowItem item) {
        item.setEnabled(false);
        ObsidianPrefs.putBoolean(PREF_PREFIX + item.getOverlayName() + "_on", false);
        DstFabricatedUtil.disableThenRun(item, AppUtils::restartSystemUI);
    }

    // ── Color swatch (DarkShadowColorListener callback) ───────────────────────

    private void onColorSwatch(DarkShadowItem item, int dialogId) {
        pendingDialogId = dialogId;
        if (getActivity() instanceof MainActivity) {
            boolean needAlpha = "ACCENT3".equals(item.getOverlayName());
            ((MainActivity) getActivity()).showColorPickerDialog(
                    dialogId, item.getColor(), needAlpha, true, true);
        }
    }

    // ── EventBus ──────────────────────────────────────────────────────────────

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        int id    = event.dialogId();
        int color = event.color();

        if (id == mBgCustomDialogId) {
            DarkShadowItem bgItem = colorItems.get(3);
            bgItem.setColor(color);
            bgItem.setEnabled(true);
            DarkShadowUtils.saveColor(bgItem);
            ObsidianPrefs.putString(PREF_BG_PRESET, "Custom");
            notifyAdapters();
            DstFabricatedUtil.applyThenRun(bgItem, AppUtils::restartSystemUI);

        } else if (id == mAcCustomDialogId) {
            DarkShadowItem acItem = colorItems.get(0);
            acItem.setColor(color);
            acItem.setEnabled(true);
            DarkShadowUtils.saveColor(acItem);
            ObsidianPrefs.putString(PREF_AC_PRESET, getString(R.string.dst_preset_custom));
            notifyAdapters();
            DstFabricatedUtil.applyThenRun(acItem, AppUtils::restartSystemUI);

        } else if (id == pendingDialogId) {
            // Color from swatch tap
            for (DarkShadowItem item : colorItems) {
                if (System.identityHashCode(item) == id) {
                    item.setColor(color);
                    DarkShadowUtils.saveColor(item);
                    if (item.isEnabled()) {
                        DstFabricatedUtil.applyThenRun(item, AppUtils::restartSystemUI);
                    }
                    break;
                }
            }
            if (accentAdapter != null) accentAdapter.notifyDataSetChanged();
            if (bgAdapter     != null) bgAdapter.notifyDataSetChanged();
            if (presetAdapter != null) presetAdapter.notifyDataSetChanged();
        }
    }

    private void notifyAdapters() {
        if (presetAdapter  != null) presetAdapter.notifyDataSetChanged();
        if (accentAdapter  != null) accentAdapter.notifyDataSetChanged();
        if (bgAdapter      != null) bgAdapter.notifyDataSetChanged();
    }

    // ── Lifecycle cleanup ─────────────────────────────────────────────────────

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }

    @Override
    public void onDestroy() { super.onDestroy(); EventBus.getDefault().unregister(this); }

    // ── DstPresetAdapter ──────────────────────────────────────────────────────

    /**
     * Shows 2 rows: [Preset Sfondo, Preset Accento].
     * Each row has a color circle, title, and current preset name as subtitle.
     * Tapping opens the preset-picker dialog.
     */
    private class DstPresetAdapter extends RecyclerView.Adapter<DstPresetAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dark_shadow, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            // pos 0 = Sfondo (BACKGROUND)
            // pos 1 = Accento (ACCENT1)
            // Dialog Style preset moved to StatusbarFragment
            DarkShadowItem item;
            String prefKey;
            String rowTitle;
            Runnable onTap;

            if (pos == 0) {
                item     = colorItems.get(3);
                prefKey  = PREF_BG_PRESET;
                rowTitle = getString(R.string.dst_section_preset_sfondo);
                onTap    = () -> showPresetDialog(true);
            } else {
                item     = colorItems.get(0);
                prefKey  = PREF_AC_PRESET;
                rowTitle = getString(R.string.dst_section_preset_accent);
                onTap    = () -> showPresetDialog(false);
            }

            // Color swatch fill
            Drawable bg = h.swatch.getBackground().mutate();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg).setColor(item.getColor());
            }
            h.swatchContainer.setVisibility(View.VISIBLE);
            h.swatchContainer.setOnClickListener(v -> onTap.run());

            h.name.setText(rowTitle);

            String saved = ObsidianPrefs.getString(prefKey, null);
            h.hex.setText(saved != null ? saved : getString(R.string.dst_none));

            // Preset rows have no toggle switch
            h.sw.setVisibility(View.GONE);

            h.itemView.setOnClickListener(v -> onTap.run());
        }

        @Override public int getItemCount() { return 2; }

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
     * Disables ALL_CAPS on all buttons; gives only the neutral button weight=1
     * so "Esci" expands to fill the middle without truncating "Disabilita"/"Applica".
     */
    private static void fixButtonCaps(AlertDialog d) {
        Button pos = d.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neg = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neu = d.getButton(AlertDialog.BUTTON_NEUTRAL);
        for (Button b : new Button[]{pos, neg, neu}) {
            if (b == null) continue;
            b.setAllCaps(false);
        }
        if (neu != null && neu.getParent() instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.gravity = Gravity.CENTER;
            neu.setLayoutParams(lp);
            neu.setGravity(Gravity.CENTER);
        }
    }

    /** Applies obs_dialog_bg drawable to the dialog window for Substratum theming. */
    private void applyDialogBg(AlertDialog d) {
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawableResource(R.drawable.obs_dialog_bg);
        }
    }
}
