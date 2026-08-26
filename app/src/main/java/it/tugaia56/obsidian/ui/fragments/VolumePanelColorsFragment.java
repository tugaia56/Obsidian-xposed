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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.DarkShadowColorListener;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.ui.models.DarkShadowItem;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;

/**
 * Color pickers for the volume slider (progress bar + background) e per l'icona del
 * cursore (colore icona — stessa struttura di qs_brightness_icon_mode in
 * QsTilesCustomizeFragment, su richiesta esplicita dell'utente).
 * Pref keys match {@link it.tugaia56.obsidian.xposed.hooks.systemui.VolumePanelMod}.
 */
public class VolumePanelColorsFragment extends Fragment {

    private static final String PREF_CUSTOM_PROGRESS = "volume_panel_seekbar_color_enabled";
    private static final String PREF_PROGRESS_COLOR  = "volume_panel_seekbar_color";
    private static final String PREF_CUSTOM_BG       = "volume_panel_seekbar_bg_color_enabled";
    private static final String PREF_BG_COLOR        = "volume_panel_seekbar_bg_color";
    private static final String PREF_ICON_MODE       = "qs_volume_icon_mode";
    private static final String PREF_ICON_COLOR      = "qs_volume_icon_custom_color";

    private final List<DarkShadowItem> mProgressItems = new ArrayList<>();
    private final List<DarkShadowItem> mBgItems       = new ArrayList<>();
    private DarkShadowColorListener mProgressAdapter;
    private DarkShadowColorListener mBgAdapter;

    private int     mPendingDialogId = -1;
    private boolean mIsProgress      = true; // true=progress, false=bg
    private RecyclerView mRv;
    private final java.util.Map<Integer, String> mSingleColorKeys = new java.util.HashMap<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
        mRv = (RecyclerView) view;
        rebuild();
    }

    private void rebuild() {
        // ── Progress color ────────────────────────────────────────────────────
        int     progressColor = ObsidianPrefs.getInt(PREF_PROGRESS_COLOR, 0xFFFFFFFF);
        boolean progressOn    = ObsidianPrefs.getBoolean(PREF_CUSTOM_PROGRESS, false);
        mProgressItems.clear();
        mProgressItems.add(new DarkShadowItem(
                getString(R.string.vol_panel_progress_color), "VOL_PROGRESS",
                Collections.emptyList(), Collections.emptyList(),
                null, progressColor, progressOn));

        DarkShadowColorListener.OnEnabled onProgressEnabled = item -> {
            item.setEnabled(true);
            ObsidianPrefs.putBoolean(PREF_CUSTOM_PROGRESS, true);
            ObsidianPrefs.putInt(PREF_PROGRESS_COLOR, item.getColor());
        };
        mProgressAdapter = new DarkShadowColorListener(mProgressItems,
                onProgressEnabled,
                item -> {
                    item.setEnabled(false);
                    ObsidianPrefs.putBoolean(PREF_CUSTOM_PROGRESS, false);
                },
                (item, dialogId) -> {
                    mPendingDialogId = dialogId;
                    mIsProgress = true;
                    showColorAccentChoice(item, dialogId, PREF_PROGRESS_COLOR, onProgressEnabled, mProgressAdapter);
                });

        // ── Background color ──────────────────────────────────────────────────
        int     bgColor = ObsidianPrefs.getInt(PREF_BG_COLOR, 0xFF808080);
        boolean bgOn    = ObsidianPrefs.getBoolean(PREF_CUSTOM_BG, false);
        mBgItems.clear();
        mBgItems.add(new DarkShadowItem(
                getString(R.string.vol_panel_bg_color), "VOL_BG",
                Collections.emptyList(), Collections.emptyList(),
                null, bgColor, bgOn));

        DarkShadowColorListener.OnEnabled onBgEnabled = item -> {
            item.setEnabled(true);
            ObsidianPrefs.putBoolean(PREF_CUSTOM_BG, true);
            ObsidianPrefs.putInt(PREF_BG_COLOR, item.getColor());
        };
        mBgAdapter = new DarkShadowColorListener(mBgItems,
                onBgEnabled,
                item -> {
                    item.setEnabled(false);
                    ObsidianPrefs.putBoolean(PREF_CUSTOM_BG, false);
                },
                (item, dialogId) -> {
                    mPendingDialogId = dialogId;
                    mIsProgress = false;
                    // No Accento here — this is a track/background colour, same treatment as
                    // "Inattivo" in Personalizza Riquadri: dark presets, not the accent option.
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showColorPickerDialog(
                                dialogId, item.getColor(), true, true, true, ObsidianTheme.bgDerivedPresets());
                    }
                });

        // ── Colore icona ─────────────────────────────────────────────────────
        List<RecyclerView.Adapter<?>> chain = new ArrayList<>();
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.vol_panel_progress_color))));
        chain.add(mProgressAdapter);
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.vol_panel_bg_color))));
        chain.add(mBgAdapter);
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.qs_tiles_brightness_icon_title_volume))));
        int iconMode = 0;
        try { iconMode = Integer.parseInt(ObsidianPrefs.getString(PREF_ICON_MODE, "0")); } catch (NumberFormatException ignored) {}
        List<Object> iconRows = new ArrayList<>();
        iconRows.add(iconModeItem());
        if (iconMode == 4) {
            iconRows.add(singleIconColorItem(getString(R.string.qs_tiles_brightness_icon_color_title), PREF_ICON_COLOR, 901));
        }
        it.tugaia56.obsidian.ui.adapters.GroupUtils.addGroup(chain, iconRows);

        android.os.Parcelable scrollState = mRv.getLayoutManager() != null
                ? mRv.getLayoutManager().onSaveInstanceState() : null;
        mRv.setAdapter(new ConcatAdapter(chain.toArray(new RecyclerView.Adapter<?>[0])));
        if (scrollState != null && mRv.getLayoutManager() != null) {
            mRv.getLayoutManager().onRestoreInstanceState(scrollState);
        }
    }

    private ListWidgetAdapter.ListItem iconModeItem() {
        String[] entries = getResources().getStringArray(R.array.qs_brightness_icon_entries);
        int idx = 0;
        try { idx = Integer.parseInt(ObsidianPrefs.getString(PREF_ICON_MODE, "0")); } catch (NumberFormatException ignored) {}
        String summary = (idx >= 0 && idx < entries.length) ? entries[idx] : entries[0];
        return new ListWidgetAdapter.ListItem(
                getString(R.string.qs_tiles_brightness_icon_title_volume), summary,
                this::showIconModeDialog);
    }

    private void showIconModeDialog() {
        String[] entries = getResources().getStringArray(R.array.qs_brightness_icon_entries);
        int current = 0;
        try { current = Integer.parseInt(ObsidianPrefs.getString(PREF_ICON_MODE, "0")); } catch (NumberFormatException ignored) {}
        final int[] selected = {current};
        ObsidianTheme.themeDialog(new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.qs_tiles_brightness_icon_title_volume)
                .setSingleChoiceItems(entries, current, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    ObsidianPrefs.putString(PREF_ICON_MODE, String.valueOf(selected[0]));
                    rebuild();
                    // Apre subito il picker su "Personalizzata" invece di lasciare una riga
                    // separata da toccare a parte — stessa correzione già fatta nella copia
                    // di questa opzione dentro QsTilesCustomizeFragment.
                    if (selected[0] == 4) {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showColorPickerDialog(
                                    901, ObsidianPrefs.getInt(PREF_ICON_COLOR, 0xFFFFFFFF), true, true, true);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    private ListWidgetAdapter.ListItem singleIconColorItem(String title, String key, int dialogId) {
        mSingleColorKeys.put(dialogId, key);
        return new ListWidgetAdapter.ListItem(title, null, () -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showColorPickerDialog(
                        dialogId, ObsidianPrefs.getInt(key, 0xFFFFFFFF), true, true, true);
            }
        });
    }

    // ── Color picker ──────────────────────────────────────────────────────────

    private void showColorPicker(int dialogId, int color) {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).showColorPickerDialog(dialogId, color, true, true, true);
    }

    /** Accento/Personalizzato inserted before the swatch opens the raw picker — Accento resolves
     *  immediately (reuses the existing onXxxEnabled save path), Personalizzato opens the picker
     *  as before. Baked at selection time (no live re-resolve), same as every other picker. */
    private void showColorAccentChoice(DarkShadowItem item, int dialogId, String colorKey,
                                        DarkShadowColorListener.OnEnabled onEnabled,
                                        DarkShadowColorListener adapter) {
        String[] entries = { getString(R.string.color_mode_accent), getString(R.string.color_mode_custom) };
        boolean currentAccent = ObsidianPrefs.getBoolean(colorKey + "_use_accent", false);
        final int[] selected = {currentAccent ? 0 : 1};
        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getName())
                .setSingleChoiceItems(entries, selected[0], (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    boolean useAccent = selected[0] == 0;
                    ObsidianPrefs.putBoolean(colorKey + "_use_accent", useAccent);
                    if (useAccent) {
                        item.setColor(ObsidianTheme.accentColor());
                        onEnabled.run(item);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    } else {
                        showColorPicker(dialogId, item.getColor());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        String singleKey = mSingleColorKeys.get(event.dialogId());
        if (singleKey != null) {
            ObsidianPrefs.putInt(singleKey, event.color());
            return;
        }
        if (event.dialogId() != mPendingDialogId) return;
        mPendingDialogId = -1;

        if (mIsProgress) {
            DarkShadowItem item = mProgressItems.get(0);
            ObsidianPrefs.putBoolean(PREF_PROGRESS_COLOR + "_use_accent", false); // picking implies custom
            item.setColor(event.color());
            ObsidianPrefs.putInt(PREF_PROGRESS_COLOR, event.color());
            if (mProgressAdapter != null) mProgressAdapter.notifyDataSetChanged();
        } else {
            DarkShadowItem item = mBgItems.get(0);
            item.setColor(event.color());
            ObsidianPrefs.putInt(PREF_BG_COLOR, event.color());
            if (mBgAdapter != null) mBgAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
