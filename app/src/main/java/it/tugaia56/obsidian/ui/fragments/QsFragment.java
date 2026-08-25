package it.tugaia56.obsidian.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.NavAdapter;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/**
 * Intestazione Impostazioni Rapide — Sfondo Solido, Header Image, QS Header Clock.
 * "Sfondo Solido" è tornato qui (era stato spostato per errore in Pannello Impostazioni
 * Rapide) con lo stesso bordo/aspetto originale (QsSolidBgFragment invariato).
 */
public class QsFragment extends Fragment {

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

        boolean bgEnabled     = ObsidianPrefs.getBoolean("DST_QS_BG_ENABLED",    false);
        boolean headerEnabled = ObsidianPrefs.getBoolean("OBS_QS_HEADER_ENABLED", false);

        NavAdapter adapter = new NavAdapter(List.of(
                new NavAdapter.NavItem(
                        R.drawable.ic_qs,
                        getString(R.string.dst_qs_solid_bg),
                        bgEnabled ? getString(R.string.enabled) : getString(R.string.disabled),
                        () -> navigate(new QsSolidBgFragment(),
                                getString(R.string.dst_qs_solid_bg))),

                new NavAdapter.NavItem(
                        R.drawable.ic_qs,
                        getString(R.string.qs_header_section),
                        headerEnabled ? getString(R.string.enabled) : getString(R.string.disabled),
                        () -> navigate(new QsHeaderImageFragment(),
                                getString(R.string.qs_header_section))),

                new NavAdapter.NavItem(
                        R.drawable.ic_qs,
                        getString(R.string.qs_header_clock_section),
                        ObsidianPrefs.getBoolean("OBS_QS_CLOCK_ENABLED", false)
                                ? getString(R.string.enabled) : getString(R.string.disabled),
                        () -> navigate(new QsHeaderClockFragment(),
                                getString(R.string.qs_header_clock_section)))
        ));

        rv.setAdapter(adapter);
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(fragment, title);
        }
    }
}
