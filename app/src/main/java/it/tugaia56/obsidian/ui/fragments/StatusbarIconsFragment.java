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
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.ui.adapters.SwitchWidgetAdapter;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Status bar icon toggles sub-screen:
 * – Hide Bluetooth when disconnected
 * – Hide Wi-Fi activity arrows
 * – Hide mobile data activity arrows
 * – Double-tap status bar to sleep
 */
public class StatusbarIconsFragment extends Fragment {

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

        rv.setAdapter(new ConcatAdapter(
                new SectionTitleAdapter(List.of(getString(R.string.section_statusbar_icons))),
                new SwitchWidgetAdapter(buildSwitches())
        ));
    }

    private List<SwitchWidgetAdapter.SwitchItem> buildSwitches() {
        return List.of(
                makePrefSwitch(
                        getString(R.string.hide_bluetooth_disconnected),
                        getString(R.string.hide_bluetooth_disconnected_summary),
                        "hide_bluetooth_when_disconnected"),
                makePrefSwitch(
                        getString(R.string.hide_inout_wifi),
                        getString(R.string.hide_inout_wifi_summary),
                        "hide_inout_wifi"),
                makePrefSwitch(
                        getString(R.string.hide_inout_mobile),
                        getString(R.string.hide_inout_mobile_summary),
                        "hide_inout_mobile"),
                makePrefSwitch(
                        getString(R.string.double_tap_sleep_statusbar),
                        getString(R.string.double_tap_sleep_statusbar_summary),
                        "double_tap_sleep_statusbar")
        );
    }

    private SwitchWidgetAdapter.SwitchItem makePrefSwitch(String title, String summary,
                                                           String prefKey) {
        SwitchWidgetAdapter.SwitchItem item = new SwitchWidgetAdapter.SwitchItem(
                title, summary,
                ObsidianPrefs.getBoolean(prefKey, false),
                null);
        item.onChanged = () -> ObsidianPrefs.putBoolean(prefKey, item.checked);
        return item;
    }
}
