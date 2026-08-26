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

import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SwitchWidgetAdapter;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * "Aggiornamento" — UI-only placeholder (mirrors OC's Update category).
 * Nothing here actually checks for updates yet.
 */
public class SettingsUpdateFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setPadding(0, 8, 0, 24);
        rv.setClipToPadding(false);
        return rv;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rv = (RecyclerView) view;

        ListWidgetAdapter checkAdapter = new ListWidgetAdapter(List.of(
                new ListWidgetAdapter.ListItem(
                        getString(R.string.settings_check_updates),
                        BuildConfig.VERSION_NAME,
                        null)));

        SwitchWidgetAdapter.SwitchItem autoUpdateItem = new SwitchWidgetAdapter.SwitchItem(
                getString(R.string.settings_auto_update),
                getString(R.string.settings_auto_update_summary),
                ObsidianPrefs.getBoolean("PLACEHOLDER_auto_update", false), null);
        autoUpdateItem.onChanged = () ->
                ObsidianPrefs.putBoolean("PLACEHOLDER_auto_update", autoUpdateItem.checked);

        SwitchWidgetAdapter.SwitchItem wifiOnlyItem = new SwitchWidgetAdapter.SwitchItem(
                getString(R.string.settings_update_wifi_only),
                getString(R.string.settings_update_wifi_only_summary),
                ObsidianPrefs.getBoolean("PLACEHOLDER_update_wifi_only", false), null);
        wifiOnlyItem.onChanged = () ->
                ObsidianPrefs.putBoolean("PLACEHOLDER_update_wifi_only", wifiOnlyItem.checked);

        SwitchWidgetAdapter switchAdapter = new SwitchWidgetAdapter(List.of(autoUpdateItem, wifiOnlyItem));

        rv.setAdapter(new ConcatAdapter(checkAdapter, switchAdapter));
    }
}
