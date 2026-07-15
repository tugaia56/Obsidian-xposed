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
 * Status bar notification removals sub-screen:
 * – Remove USB dialog
 * – Remove low battery notification
 * – Remove charging complete notification
 * – Remove flashlight notification
 * – Remove developer mode notification
 */
public class StatusbarNotifsFragment extends Fragment {

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
                new SectionTitleAdapter(List.of(getString(R.string.section_statusbar_notifs))),
                new SwitchWidgetAdapter(buildSwitches())
        ));
    }

    private List<SwitchWidgetAdapter.SwitchItem> buildSwitches() {
        return List.of(
                makePrefSwitch(
                        getString(R.string.remove_usb_dialog),
                        getString(R.string.remove_usb_dialog_summary),
                        "remove_usb_dialog"),
                makePrefSwitch(
                        getString(R.string.remove_low_battery),
                        getString(R.string.remove_low_battery_summary),
                        "remove_low_battery_notification"),
                makePrefSwitch(
                        getString(R.string.remove_charging_complete),
                        getString(R.string.remove_charging_complete_summary),
                        "remove_charging_complete_notification"),
                makePrefSwitch(
                        getString(R.string.remove_flashlight_notif),
                        getString(R.string.remove_flashlight_notif_summary),
                        "remove_flashlight_notification"),
                makePrefSwitch(
                        getString(R.string.remove_dev_mode_notif),
                        getString(R.string.remove_dev_mode_notif_summary),
                        "remove_dev_mode")
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
