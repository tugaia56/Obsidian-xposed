package it.tugaia56.obsidian.ui.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.NavAdapter;
import it.tugaia56.obsidian.utils.AppUtils;
import it.tugaia56.obsidian.utils.DstFabricatedUtil;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Status Bar sub-screen: navigation menu + DST Dialog Style preset (5th NavItem).
 */
public class StatusbarFragment extends Fragment {

    private static final String   PREF_DLG_PRESET = "DST_DLG_PRESET_NAME";
    private static final String[] DLG_STYLE_KEYS  = {
        "DSTDHT", "DSTDHTO", "DSTDLT", "DSTDLYO",
        "DSTDMT", "DSTDMTO", "DSTDS",  "DSTDSO"
    };

    private final List<NavAdapter.NavItem> mNavItems = new ArrayList<>();
    private NavAdapter mNavAdapter;

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

        mNavItems.clear();

        mNavItems.add(new NavAdapter.NavItem(
                R.drawable.ic_palette,
                getString(R.string.section_statusbar_icon_color),
                getString(R.string.section_statusbar_icon_color_summary),
                () -> navigate(new StatusbarSbiFragment(),
                        getString(R.string.section_statusbar_icon_color))));

        mNavItems.add(new NavAdapter.NavItem(
                R.drawable.ic_mods_ui,
                getString(R.string.section_statusbar_icons),
                getString(R.string.section_statusbar_icons_summary),
                () -> navigate(new StatusbarIconsFragment(),
                        getString(R.string.section_statusbar_icons))));

        mNavItems.add(new NavAdapter.NavItem(
                R.drawable.ic_notifications,
                getString(R.string.section_statusbar_notifs),
                getString(R.string.section_statusbar_notifs_summary),
                () -> navigate(new StatusbarNotifsFragment(),
                        getString(R.string.section_statusbar_notifs))));

        mNavItems.add(new NavAdapter.NavItem(
                R.drawable.ic_clock,
                getString(R.string.section_clock_date),
                getString(R.string.nav_clock_date_summary),
                () -> navigate(new ClockDateFragment(),
                        getString(R.string.section_clock_date))));

        // 5th item: dialog style preset (opens picker dialog, does not navigate)
        mNavItems.add(new NavAdapter.NavItem(
                R.drawable.ic_ui_styles,
                getString(R.string.dst_section_preset_dialog),
                getDlgPresetLabel(),
                this::showDialogStylePresetDialog));

        mNavAdapter = new NavAdapter(mNavItems);
        rv.setAdapter(mNavAdapter);
    }

    // ---- Dialog Style -------------------------------------------------------

    private void showDialogStylePresetDialog() {
        String[] names = {
            getString(R.string.dst_dlg_ht),
            getString(R.string.dst_dlg_hto),
            getString(R.string.dst_dlg_lt),
            getString(R.string.dst_dlg_lyo),
            getString(R.string.dst_dlg_mt),
            getString(R.string.dst_dlg_mto),
            getString(R.string.dst_dlg_s),
            getString(R.string.dst_dlg_so),
        };

        String saved = ObsidianPrefs.getString(PREF_DLG_PRESET, null);
        int currentIdx = -1;
        for (int i = 0; i < DLG_STYLE_KEYS.length; i++) {
            if (DLG_STYLE_KEYS[i].equals(saved)) { currentIdx = i; break; }
        }

        final int[] selected = {currentIdx};
        AlertDialog dlg = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dst_section_preset_dialog)
                .setSingleChoiceItems(names, currentIdx, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    if (selected[0] < 0) return;
                    ObsidianPrefs.putString(PREF_DLG_PRESET, DLG_STYLE_KEYS[selected[0]]);
                    refreshDlgRow();
                    new Thread(() -> {
                        DstFabricatedUtil.saveBootProps();
                        AppUtils.restartSystemUI();
                    }).start();
                })
                .setNeutralButton(R.string.dst_disable, (d, w) -> {
                    ObsidianPrefs.putString(PREF_DLG_PRESET, null);
                    refreshDlgRow();
                    new Thread(() -> {
                        DstFabricatedUtil.saveBootProps();
                        AppUtils.restartSystemUI();
                    }).start();
                })
                .setNegativeButton(R.string.close, null)
                .show();
        fixButtonCaps(dlg);
        applyDialogBg(dlg);
    }

    private String getDlgPresetLabel() {
        String saved = ObsidianPrefs.getString(PREF_DLG_PRESET, null);
        if (saved == null) return getString(R.string.dst_none);
        int[] nameRes = {
            R.string.dst_dlg_ht,  R.string.dst_dlg_hto,
            R.string.dst_dlg_lt,  R.string.dst_dlg_lyo,
            R.string.dst_dlg_mt,  R.string.dst_dlg_mto,
            R.string.dst_dlg_s,   R.string.dst_dlg_so,
        };
        for (int i = 0; i < DLG_STYLE_KEYS.length; i++) {
            if (DLG_STYLE_KEYS[i].equals(saved)) return getString(nameRes[i]);
        }
        return saved;
    }

    /** Replace the 5th NavItem subtitle to reflect the newly chosen preset. */
    private void refreshDlgRow() {
        if (mNavAdapter == null || mNavItems.size() < 5 || !isAdded()) return;
        mNavItems.set(4, new NavAdapter.NavItem(
                R.drawable.ic_ui_styles,
                getString(R.string.dst_section_preset_dialog),
                getDlgPresetLabel(),
                this::showDialogStylePresetDialog));
        mNavAdapter.notifyItemChanged(4);
    }

    // ---- Helpers ------------------------------------------------------------

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

    private void applyDialogBg(AlertDialog d) {
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawableResource(R.drawable.obs_dialog_bg);
        }
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(fragment, title);
        }
    }
}
