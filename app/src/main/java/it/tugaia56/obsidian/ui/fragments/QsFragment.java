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
import it.tugaia56.obsidian.utils.AppUtils;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Quick Settings sub-screen: solid QS background toggle.
 */
public class QsFragment extends Fragment {

    private static final String PREF_QS_BG = "DST_QS_BG_ENABLED";

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

        SwitchWidgetAdapter.SwitchItem qsItem = new SwitchWidgetAdapter.SwitchItem(
                getString(R.string.dst_qs_solid_bg),
                getString(R.string.dst_qs_solid_bg_summary),
                R.drawable.ic_qs,
                ObsidianPrefs.getBoolean(PREF_QS_BG, false),
                null);
        qsItem.onChanged = () -> {
            ObsidianPrefs.putBoolean(PREF_QS_BG, qsItem.checked);
            AppUtils.restartSystemUI();
        };

        rv.setAdapter(new ConcatAdapter(
                new SectionTitleAdapter(List.of(getString(R.string.nav_quick_settings))),
                new SwitchWidgetAdapter(List.of(qsItem))
        ));
    }
}
