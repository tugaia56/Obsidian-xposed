package it.tugaia56.obsidian.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.utils.Constants;
import it.tugaia56.obsidian.utils.DarkShadowUtils;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.overlay.FabricatedUtil;

import static it.tugaia56.obsidian.utils.DarkShadowUtils.PREF_PIN;
import static it.tugaia56.obsidian.utils.DarkShadowUtils.PREF_PIN_CUSTOM_COLOR;
import static it.tugaia56.obsidian.utils.DarkShadowUtils.PREF_PIN_NUM;
import static it.tugaia56.obsidian.utils.DarkShadowUtils.PREF_PIN_NUM_CUSTOM_COLOR;
import static it.tugaia56.obsidian.utils.DarkShadowUtils.PREF_PREFIX;

/**
 * PIN / Tastierino sub-screen: background and number text color pickers.
 */
public class PinStyleFragment extends Fragment {

    private static final String OVERLAY_PIN_PREFIX = "PIN_";
    private static final String OVERLAY_NUM_PREFIX = "PIN_NUM_";

    private ListWidgetAdapter pinBgAdapter;
    private ListWidgetAdapter pinNumAdapter;

    private int mPinBgCustomDialogId;
    private int mPinNumCustomDialogId;
    private int mPendingDialogId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setPadding(0, 8, 0, 8);
        rv.setClipToPadding(false);
        return rv;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rv = (RecyclerView) view;

        mPinBgCustomDialogId  = View.generateViewId();
        mPinNumCustomDialogId = View.generateViewId();

        // ── PIN Sfondo ───────────────────────────────────────────────────────
        ListWidgetAdapter.ListItem pinBgItem = new ListWidgetAdapter.ListItem(
                getString(R.string.section_pin_bg_style),
                pinBgLabel(),
                this::showPinBgDialog);
        pinBgAdapter = new ListWidgetAdapter(List.of(pinBgItem));

        // ── PIN Numeri ───────────────────────────────────────────────────────
        ListWidgetAdapter.ListItem pinNumItem = new ListWidgetAdapter.ListItem(
                getString(R.string.section_pin_num_style),
                pinNumLabel(),
                this::showPinNumDialog);
        pinNumAdapter = new ListWidgetAdapter(List.of(pinNumItem));

        rv.setAdapter(new ConcatAdapter(pinBgAdapter, pinNumAdapter));
    }

    // ── PIN Sfondo dialog ─────────────────────────────────────────────────────

    private void showPinBgDialog() {
        String[] entries = requireContext().getResources().getStringArray(R.array.pin_style_entries);
        String[] values  = requireContext().getResources().getStringArray(R.array.pin_style_values);
        String current   = ObsidianPrefs.getString(PREF_PIN, "default");
        int currentIdx   = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) { currentIdx = i; break; }

        final int[] sel = {currentIdx};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.section_pin_bg_style)
                .setSingleChoiceItems(entries, currentIdx, (d, which) -> sel[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    String chosen = values[sel[0]];
                    ObsidianPrefs.putString(PREF_PIN, chosen);

                    if ("DSTPINCustom".equals(chosen)) {
                        int saved = ObsidianPrefs.getInt(PREF_PIN_CUSTOM_COLOR, 0xFFFFFFFF);
                        mPendingDialogId = mPinBgCustomDialogId;
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showColorPickerDialog(
                                    mPinBgCustomDialogId, saved, false, true, true);
                        }
                        updatePinBgSummary(entries[sel[0]]);
                        return;
                    }

                    new Thread(() -> {
                        int accent = ObsidianPrefs.getInt(PREF_PREFIX + "ACCENT1", 0xFF6200EE);
                        applyPinBgFabricated(accent);
                    }).start();
                    updatePinBgSummary(entries[sel[0]]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── PIN Numeri dialog ─────────────────────────────────────────────────────

    private void showPinNumDialog() {
        String[] entries = requireContext().getResources().getStringArray(R.array.pin_num_style_entries);
        String[] values  = requireContext().getResources().getStringArray(R.array.pin_num_style_values);
        String current   = ObsidianPrefs.getString(PREF_PIN_NUM, "default");
        int currentIdx   = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) { currentIdx = i; break; }

        final int[] sel = {currentIdx};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.section_pin_num_style)
                .setSingleChoiceItems(entries, currentIdx, (d, which) -> sel[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    String chosen = values[sel[0]];
                    ObsidianPrefs.putString(PREF_PIN_NUM, chosen);

                    if ("DSTNUMPINCustom".equals(chosen)) {
                        int saved = ObsidianPrefs.getInt(PREF_PIN_NUM_CUSTOM_COLOR, 0xFFFFFFFF);
                        mPendingDialogId = mPinNumCustomDialogId;
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showColorPickerDialog(
                                    mPinNumCustomDialogId, saved, false, true, true);
                        }
                        updatePinNumSummary(entries[sel[0]]);
                        return;
                    }

                    new Thread(() -> {
                        int accent = ObsidianPrefs.getInt(PREF_PREFIX + "ACCENT1", 0xFF6200EE);
                        applyPinNumFabricated(chosen, accent);
                    }).start();
                    updatePinNumSummary(entries[sel[0]]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── EventBus: custom color picked ────────────────────────────────────────

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        if (event.dialogId() != mPendingDialogId) return;
        mPendingDialogId = -1;

        if (event.dialogId() == mPinBgCustomDialogId) {
            ObsidianPrefs.putInt(PREF_PIN_CUSTOM_COLOR, event.color());
            new Thread(() -> applyPinBgFabricated(0)).start();
            updatePinBgSummary(getString(R.string.pin_style_custom));

        } else if (event.dialogId() == mPinNumCustomDialogId) {
            ObsidianPrefs.putInt(PREF_PIN_NUM_CUSTOM_COLOR, event.color());
            new Thread(() -> applyPinNumFabricated("DSTNUMPINCustom", event.color())).start();
            updatePinNumSummary(getString(R.string.pin_style_custom));
        }
    }

    // ── Apply helpers ─────────────────────────────────────────────────────────

    private void applyPinBgFabricated(int accentColor) {
        String pinPref = ObsidianPrefs.getString(PREF_PIN, "default");
        if ("default".equals(pinPref)) {
            FabricatedUtil.disableOverlays(
                    OVERLAY_PIN_PREFIX + "border", OVERLAY_PIN_PREFIX + "inner1",
                    OVERLAY_PIN_PREFIX + "inner2", OVERLAY_PIN_PREFIX + "shadow",
                    OVERLAY_PIN_PREFIX + "outer1", OVERLAY_PIN_PREFIX + "outer2",
                    OVERLAY_PIN_PREFIX + "outer3", OVERLAY_PIN_PREFIX + "dotfill",
                    OVERLAY_PIN_PREFIX + "dotout", OVERLAY_PIN_PREFIX + "wordtxt",
                    OVERLAY_PIN_PREFIX + "wordtxtL");
            return;
        }

        int color;
        boolean shade;
        if ("DSTPINCustom".equals(pinPref)) {
            color = ObsidianPrefs.getInt(PREF_PIN_CUSTOM_COLOR, 0xFFFFFFFF);
            shade = false;
        } else {
            color = accentColor;
            shade = "DSTPINAccentShade".equals(pinPref);
        }

        int rgb    = color & 0x00FFFFFF;
        int full   = 0xFF000000 | rgb;
        int ripple = 0x80000000 | rgb;
        int outer1 = 0xCC000000 | rgb;
        int outer2 = 0x40000000 | rgb;
        int outer3 = 0x21000000 | rgb;
        int shadow = shade ? ripple : full;

        FabricatedUtil.buildAndEnableOverlays(
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "border",   "color",
                        "coui_numeric_keyboard_border_color",              fmt(full)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "inner1",   "color",
                        "coui_numeric_keyboard_inner_gradient_color_1",    fmt(ripple)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "inner2",   "color",
                        "coui_numeric_keyboard_inner_gradient_color_2",    fmt(ripple)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "shadow",   "color",
                        "coui_numeric_keyboard_upper_inner_shadow_color",  fmt(shadow)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "outer1",   "color",
                        "coui_numeric_keyboard_outer_gradient_color_1",    fmt(outer1)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "outer2",   "color",
                        "coui_numeric_keyboard_outer_gradient_color_2",    fmt(outer2)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "outer3",   "color",
                        "coui_numeric_keyboard_outer_gradient_color_3",    fmt(outer3)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "dotfill",  "color",
                        "coui_simple_lock_transparent_filled_rectangle_icon_color",    fmt(full)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "dotout",   "color",
                        "coui_simple_lock_transparent_outlined_rectangle_icon_color", "0x33FFFFFF"},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "wordtxt",  "color",
                        "coui_numeric_keyboard_dark_word_text_normal_color",       fmt(full)},
                new Object[]{Constants.SYSTEM_UI, OVERLAY_PIN_PREFIX + "wordtxtL", "color",
                        "coui_numeric_keyboard_dark_word_text_normal_light_color", fmt(full)}
        );
    }

    private void applyPinNumFabricated(String preset, int accentColor) {
        if ("default".equals(preset)) {
            FabricatedUtil.disableOverlays(OVERLAY_NUM_PREFIX + "color");
            return;
        }
        int color;
        if ("DSTNUMPINCustom".equals(preset)) {
            color = accentColor;
        } else if ("DSTNUMPINAccentShade".equals(preset)) {
            color = 0x80000000 | (accentColor & 0x00FFFFFF);
        } else {
            color = accentColor;
        }
        FabricatedUtil.buildAndEnableOverlays(
                new Object[]{Constants.SYSTEM_UI, OVERLAY_NUM_PREFIX + "color", "color",
                        "coui_numeric_keyboard_number_color",
                        fmt(0xFF000000 | (color & 0x00FFFFFF))}
        );
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void updatePinBgSummary(String label) {
        if (pinBgAdapter == null) return;
        pinBgAdapter.getItems().get(0).valueSummary = label;
        pinBgAdapter.notifyItemChanged(0);
    }

    private void updatePinNumSummary(String label) {
        if (pinNumAdapter == null) return;
        pinNumAdapter.getItems().get(0).valueSummary = label;
        pinNumAdapter.notifyItemChanged(0);
    }

    private String pinBgLabel() {
        String[] entries = requireContext().getResources().getStringArray(R.array.pin_style_entries);
        String[] values  = requireContext().getResources().getStringArray(R.array.pin_style_values);
        String pref = ObsidianPrefs.getString(PREF_PIN, "default");
        for (int i = 0; i < values.length; i++) if (values[i].equals(pref)) return entries[i];
        return entries[0];
    }

    private String pinNumLabel() {
        String[] entries = requireContext().getResources().getStringArray(R.array.pin_num_style_entries);
        String[] values  = requireContext().getResources().getStringArray(R.array.pin_num_style_values);
        String pref = ObsidianPrefs.getString(PREF_PIN_NUM, "default");
        for (int i = 0; i < values.length; i++) if (values[i].equals(pref)) return entries[i];
        return entries[0];
    }

    private static String fmt(int color) {
        return String.format("0x%08X", 0xFFFFFFFFL & color);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
