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

import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.adapters.SliderWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SwitchWidgetAdapter;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Dock Background sub-screen — mirrors OC's launcher_dock_background.xml.
 * UI/prefs only for now (no hook yet) — marked with [[wip_inline_suffix]] on every row.
 */
public class LauncherDockBackgroundFragment extends Fragment {

    private static final String KEY_DOCK_BG          = "dockBackground";
    private static final String KEY_DOCK_BG_MATERIAL = "dockBackgroundMaterial";
    private static final String KEY_DOCK_BG_AMOUNT   = "dockBackgroundMaterialAmount";
    private static final String KEY_DOCK_BG_RADIUS   = "dockBackgroundRadius";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setPadding(0, 12, 0, 24);
        rv.setClipToPadding(false);
        return rv;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rv = (RecyclerView) view;

        SwitchWidgetAdapter toggles = new SwitchWidgetAdapter(List.of(
                boolItem(getString(R.string.dock_background) + getString(R.string.wip_inline_suffix), KEY_DOCK_BG),
                boolItem(getString(R.string.dock_background_material) + getString(R.string.wip_inline_suffix), KEY_DOCK_BG_MATERIAL)));

        SliderWidgetAdapter amount = sliderRow(getString(R.string.dock_background_amount) + getString(R.string.wip_inline_suffix), KEY_DOCK_BG_AMOUNT, 0, 4, 0);
        SliderWidgetAdapter radius = sliderRow(getString(R.string.dock_background_radius) + getString(R.string.wip_inline_suffix), KEY_DOCK_BG_RADIUS, 0, 100, 30);

        rv.setAdapter(new ConcatAdapter(toggles, amount, radius));
    }

    private SwitchWidgetAdapter.SwitchItem boolItem(String title, String key) {
        SwitchWidgetAdapter.SwitchItem item = new SwitchWidgetAdapter.SwitchItem(
                title, null, ObsidianPrefs.getBoolean(key, false), null);
        item.onChanged = () -> ObsidianPrefs.putBoolean(key, item.checked);
        return item;
    }

    private SliderWidgetAdapter sliderRow(String title, String key, int min, int max, int def) {
        int current = ObsidianPrefs.getInt(key, def);
        SliderWidgetAdapter.SliderItem item = new SliderWidgetAdapter.SliderItem(
                title, current, min, max, "", def,
                value -> ObsidianPrefs.putInt(key, value));
        return new SliderWidgetAdapter(List.of(item));
    }
}
