package it.tugaia56.obsidian.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.NavAdapter;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;

/**
 * Sub-screen "Barra volume": position, disable warning, timeout, colors.
 * Pref keys must match {@link it.tugaia56.obsidian.xposed.hooks.systemui.VolumePanelMod}.
 */
public class VolumePanelFragment extends Fragment {

    static final String PREF_POSITION = "volume_panel_position"; // "0"|"1"|"2"
    static final String PREF_TIMEOUT  = "volume_panel_timeout";  // int seconds

    private static final int IDX_POSITION = 0;
    private static final int IDX_TIMEOUT  = 1;
    private static final int IDX_COLORS   = 2;

    private final List<NavAdapter.NavItem> mNavItems = new ArrayList<>();
    private NavAdapter mAdapter;

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
        buildItems();
        mAdapter = new NavAdapter(mNavItems);
        ((RecyclerView) view).setAdapter(mAdapter);
    }

    private void buildItems() {
        mNavItems.clear();

        mNavItems.add(new NavAdapter.NavItem(             // 0 – position
                R.drawable.ic_sysui_volume,
                getString(R.string.vol_panel_position),
                positionLabel(),
                this::showPositionDialog));

        mNavItems.add(new NavAdapter.NavItem(             // 1 – timeout
                R.drawable.ic_clock,
                getString(R.string.vol_panel_timeout),
                timeoutLabel(),
                this::showTimeoutDialog));

        mNavItems.add(new NavAdapter.NavItem(             // 2 – colors
                R.drawable.ic_palette,
                getString(R.string.vol_panel_colors),
                getString(R.string.vol_panel_colors_summary),
                () -> navigate(new VolumePanelColorsFragment(),
                        getString(R.string.vol_panel_colors))));
    }

    // ── Position ──────────────────────────────────────────────────────────────

    private void showPositionDialog() {
        String[] entries = getResources().getStringArray(R.array.vol_panel_position_entries);
        int current = Integer.parseInt(ObsidianPrefs.getString(PREF_POSITION, "0"));

        final int[] selected = {current};
        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vol_panel_position)
                .setSingleChoiceItems(entries, current, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    ObsidianPrefs.putString(PREF_POSITION, String.valueOf(selected[0]));
                    refreshItem(IDX_POSITION, R.drawable.ic_sysui_volume,
                            R.string.vol_panel_position, positionLabel(), this::showPositionDialog);
                })
                .setNegativeButton(R.string.close, null)
                .show());
    }

    private String positionLabel() {
        String[] entries = getResources().getStringArray(R.array.vol_panel_position_entries);
        int idx = Integer.parseInt(ObsidianPrefs.getString(PREF_POSITION, "0"));
        return (idx >= 0 && idx < entries.length) ? entries[idx] : entries[0];
    }

    // ── Timeout ───────────────────────────────────────────────────────────────

    private void showTimeoutDialog() {
        String[] entries = getResources().getStringArray(R.array.vol_panel_timeout_entries);
        int[]    values  = getResources().getIntArray(R.array.vol_panel_timeout_values);
        int savedSec = ObsidianPrefs.getInt(PREF_TIMEOUT, 3);

        int currentIdx = 2; // default index for 3 s
        for (int i = 0; i < values.length; i++) {
            if (values[i] == savedSec) { currentIdx = i; break; }
        }

        final int[] selected = {currentIdx};
        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vol_panel_timeout)
                .setSingleChoiceItems(entries, currentIdx, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    ObsidianPrefs.putInt(PREF_TIMEOUT, values[selected[0]]);
                    refreshItem(IDX_TIMEOUT, R.drawable.ic_clock,
                            R.string.vol_panel_timeout, timeoutLabel(), this::showTimeoutDialog);
                })
                .setNegativeButton(R.string.close, null)
                .show());
    }

    private String timeoutLabel() {
        int sec = ObsidianPrefs.getInt(PREF_TIMEOUT, 3);
        return getString(R.string.vol_panel_timeout_fmt, sec);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshItem(int idx, int iconRes, int titleRes, String subtitle, Runnable onClick) {
        if (mAdapter == null || mNavItems.size() <= idx || !isAdded()) return;
        mNavItems.set(idx, new NavAdapter.NavItem(iconRes, getString(titleRes), subtitle, onClick));
        mAdapter.notifyItemChanged(idx);
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).navigateTo(fragment, title);
    }
}
