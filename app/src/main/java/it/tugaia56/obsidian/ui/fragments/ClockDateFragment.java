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

import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.NavAdapter;
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Clock & Date customisation sub-screen:
 *  – Clock position (left / center / right)
 *  – Clock font size
 *  – Clock extra padding
 *  – Link to Ora & Data sub-screen (date display, AM/PM, advanced format)
 *
 * Note: Custom clock color lives in StatusbarSbiFragment (alongside icon colors).
 */
public class ClockDateFragment extends Fragment {

    // ── Pref keys (matching StatusbarClock hook) ───────────────────────────────
    private static final String PREF_POSITION = "status_bar_clock";
    private static final String PREF_SIZE     = "status_bar_clock_size";
    private static final String PREF_PADDING  = "status_bar_clock_padding";

    // ── Adapters ──────────────────────────────────────────────────────────────
    private ListWidgetAdapter positionAdapter;
    private ListWidgetAdapter sizeAdapter;
    private ListWidgetAdapter paddingAdapter;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

        // ── Position ─────────────────────────────────────────────────────────
        positionAdapter = new ListWidgetAdapter(List.of(
                new ListWidgetAdapter.ListItem(
                        getString(R.string.clock_position_title),
                        positionLabel(),
                        this::showPositionDialog)));

        // ── Font size ─────────────────────────────────────────────────────────
        sizeAdapter = new ListWidgetAdapter(List.of(
                new ListWidgetAdapter.ListItem(
                        getString(R.string.clock_size_title),
                        sizeLabel(),
                        this::showSizeDialog)));

        // ── Extra padding ─────────────────────────────────────────────────────
        paddingAdapter = new ListWidgetAdapter(List.of(
                new ListWidgetAdapter.ListItem(
                        getString(R.string.clock_padding_title),
                        paddingLabel(),
                        this::showPaddingDialog)));

        buildRecyclerView(rv);
    }

    /** Assemble the ConcatAdapter. */
    private void buildRecyclerView(RecyclerView rv) {
        NavAdapter oraDataNav = new NavAdapter(List.of(
                new NavAdapter.NavItem(
                        R.drawable.ic_clock,
                        getString(R.string.nav_clock_date),
                        getString(R.string.nav_clock_date_summary),
                        () -> {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).navigateTo(
                                        new ClockOraDataFragment(),
                                        getString(R.string.nav_clock_date));
                            }
                        })
        ));

        rv.setAdapter(new ConcatAdapter(
                new SectionTitleAdapter(List.of(getString(R.string.section_clock_date))),
                positionAdapter,
                sizeAdapter,
                paddingAdapter,
                oraDataNav
        ));
    }

    // ── Position dialog ───────────────────────────────────────────────────────

    private void showPositionDialog() {
        String[] entries = requireContext().getResources().getStringArray(R.array.clock_position_entries);
        String[] values  = requireContext().getResources().getStringArray(R.array.clock_position_values);
        String   cur     = ObsidianPrefs.getString(PREF_POSITION, "2");
        int curIdx = indexOf(values, cur, 0);

        final int[] sel = {curIdx};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clock_position_title)
                .setSingleChoiceItems(entries, curIdx, (d, w) -> sel[0] = w)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    ObsidianPrefs.putString(PREF_POSITION, values[sel[0]]);
                    positionAdapter.getItems().get(0).valueSummary = entries[sel[0]];
                    positionAdapter.notifyItemChanged(0);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Size dialog ───────────────────────────────────────────────────────────

    private void showSizeDialog() {
        String[] entries = requireContext().getResources().getStringArray(R.array.clock_size_entries);
        int[]    values  = requireContext().getResources().getIntArray(R.array.clock_size_values);
        int      cur     = ObsidianPrefs.getInt(PREF_SIZE, 12);
        int curIdx = indexOfInt(values, cur, 0);

        final int[] sel = {curIdx};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clock_size_title)
                .setSingleChoiceItems(entries, curIdx, (d, w) -> sel[0] = w)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    ObsidianPrefs.putInt(PREF_SIZE, values[sel[0]]);
                    sizeAdapter.getItems().get(0).valueSummary = entries[sel[0]];
                    sizeAdapter.notifyItemChanged(0);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Padding dialog ────────────────────────────────────────────────────────

    private void showPaddingDialog() {
        String[] entries = requireContext().getResources().getStringArray(R.array.clock_padding_entries);
        int[]    values  = requireContext().getResources().getIntArray(R.array.clock_padding_values);
        int      cur     = ObsidianPrefs.getInt(PREF_PADDING, 0);
        int curIdx = indexOfInt(values, cur, 0);

        final int[] sel = {curIdx};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clock_padding_title)
                .setSingleChoiceItems(entries, curIdx, (d, w) -> sel[0] = w)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    ObsidianPrefs.putInt(PREF_PADDING, values[sel[0]]);
                    paddingAdapter.getItems().get(0).valueSummary = entries[sel[0]];
                    paddingAdapter.notifyItemChanged(0);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Summary label helpers ─────────────────────────────────────────────────

    private String positionLabel() {
        String[] entries = requireContext().getResources().getStringArray(R.array.clock_position_entries);
        String[] values  = requireContext().getResources().getStringArray(R.array.clock_position_values);
        return entries[indexOf(values, ObsidianPrefs.getString(PREF_POSITION, "2"), 0)];
    }

    private String sizeLabel() {
        String[] entries = requireContext().getResources().getStringArray(R.array.clock_size_entries);
        int[]    values  = requireContext().getResources().getIntArray(R.array.clock_size_values);
        return entries[indexOfInt(values, ObsidianPrefs.getInt(PREF_SIZE, 12), 0)];
    }

    private String paddingLabel() {
        String[] entries = requireContext().getResources().getStringArray(R.array.clock_padding_entries);
        int[]    values  = requireContext().getResources().getIntArray(R.array.clock_padding_values);
        return entries[indexOfInt(values, ObsidianPrefs.getInt(PREF_PADDING, 0), 0)];
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static int indexOf(String[] arr, String val, int def) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(val)) return i;
        return def;
    }

    private static int indexOfInt(int[] arr, int val, int def) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return def;
    }
}
