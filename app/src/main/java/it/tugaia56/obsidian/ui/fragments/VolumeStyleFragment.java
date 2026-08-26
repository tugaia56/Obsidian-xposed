package it.tugaia56.obsidian.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import it.tugaia56.obsidian.ui.activity.MainActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.adapters.NavAdapter;
import it.tugaia56.obsidian.utils.AppUtils;
import it.tugaia56.obsidian.utils.DstFabricatedUtil;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Sub-screen for Volume Adjust Icon presets.
 * Two NavItems: Round (RVD) and Square (SVD).
 * Selecting one preset automatically clears the other so they stay mutually exclusive.
 */
public class VolumeStyleFragment extends Fragment {

    private static final String PREF_RVD = "DST_PRESET_RVD";
    private static final String PREF_SVD = "DST_PRESET_SVD";

    private static final String[] RVD_OVERLAYS = {
        "DSTRVDAccent", "DSTRVDAccentShade", "DSTRVDDarkGray", "DSTRVDLightGray",
        "DSTRVDWhite",  "DSTRVDOutlined",    "DSTRVDSemiTrasp", "DSTRVDOutlinedTrasp"
    };
    private static final String[] SVD_OVERLAYS = {
        "DSTSVDAccent", "DSTSVDAccentShade", "DSTSVDDarkGray", "DSTSVDLightGray",
        "DSTSVDWhite",  "DSTSVDOutlined",    "DSTSVDSemiTrasp", "DSTSVDOutlinedTrasp"
    };

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

        String[] rvdNames = getResources().getStringArray(R.array.dst_rvd_preset_names);
        String[] svdNames = getResources().getStringArray(R.array.dst_svd_preset_names);

        List<NavAdapter.NavItem> items = List.of(

                new NavAdapter.NavItem(
                        R.drawable.ic_sysui_volume,
                        getString(R.string.nav_rvd_round),
                        getString(R.string.nav_rvd_round_summary),
                        () -> showPresetDialog(
                                getString(R.string.nav_rvd_round),
                                rvdNames, RVD_OVERLAYS, PREF_RVD, PREF_SVD)),

                new NavAdapter.NavItem(
                        R.drawable.ic_sysui_volume,
                        getString(R.string.nav_svd_square),
                        getString(R.string.nav_svd_square_summary),
                        () -> showPresetDialog(
                                getString(R.string.nav_svd_square),
                                svdNames, SVD_OVERLAYS, PREF_SVD, PREF_RVD)),

                new NavAdapter.NavItem(
                        R.drawable.ic_sysui_volume,
                        getString(R.string.vol_panel_section),
                        getString(R.string.vol_panel_section_summary),
                        () -> navigate(new VolumePanelFragment(),
                                getString(R.string.vol_panel_section)))
        );

        ((RecyclerView) view).setAdapter(new NavAdapter(items));
    }

    // ── Preset dialog ─────────────────────────────────────────────────────────

    /**
     * @param prefKey     pref key to save the chosen overlay name into
     * @param prefOtherKey pref key of the opposing style — cleared when a preset is applied
     */
    private void showPresetDialog(String title, String[] names,
                                  String[] overlayKeys, String prefKey, String prefOtherKey) {
        String current = ObsidianPrefs.getString(prefKey, null);
        int currentIdx = -1;
        for (int i = 0; i < overlayKeys.length; i++) {
            if (overlayKeys[i].equals(current)) { currentIdx = i; break; }
        }

        final int[] selected = {currentIdx};
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(names, currentIdx,
                        (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    if (selected[0] < 0) return;
                    ObsidianPrefs.putString(prefKey, overlayKeys[selected[0]]);
                    ObsidianPrefs.remove(prefOtherKey);   // mutually exclusive
                    DstFabricatedUtil.saveBootProps();    // persist to system props for early-boot read
                    AppUtils.showRestartReminder(requireContext());
                })
                .setNeutralButton(R.string.dst_disable, (d, w) -> {
                    ObsidianPrefs.remove(prefKey);
                    DstFabricatedUtil.saveBootProps();
                    AppUtils.showRestartReminder(requireContext());
                })
                .setNegativeButton(R.string.close, null)
                .show();
        applyDialogBg(dialog);
        fixButtonCaps(dialog);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(it.tugaia56.obsidian.utils.ObsidianTheme.dialogBackground(requireContext()));
        }
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).navigateTo(fragment, title);
    }
}
