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

/**
 * Always-On Display hub — mirrors OC's AOD section: clock, weather, edge lighting.
 * UI-only ports for now (AodClockFragment / AodWeatherFragment / AodEdgeLightFragment) —
 * visible with previews, but not wired to a hook yet.
 */
public class AodFragment extends Fragment {

    private static final int ACCENT_CLOCK   = 0xFF00BCD4; // cyan
    private static final int ACCENT_WEATHER = 0xFF4CAF50; // green
    private static final int ACCENT_EDGE    = 0xFFFF9800; // amber

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

        List<NavAdapter.NavItem> items = List.of(

                new NavAdapter.NavItem(
                        R.drawable.ic_clock,
                        getString(R.string.nav_aod_clock),
                        getString(R.string.nav_aod_clock_summary),
                        () -> navigate(new AodClockFragment(),
                                getString(R.string.nav_aod_clock)),
                        ACCENT_CLOCK),

                new NavAdapter.NavItem(
                        R.drawable.ic_palette,
                        getString(R.string.nav_aod_weather),
                        getString(R.string.nav_aod_weather_summary),
                        () -> navigate(new AodWeatherFragment(),
                                getString(R.string.nav_aod_weather)),
                        ACCENT_WEATHER),

                new NavAdapter.NavItem(
                        R.drawable.ic_drawing,
                        getString(R.string.nav_aod_edge_lighting),
                        getString(R.string.nav_aod_edge_lighting_summary),
                        () -> navigate(new AodEdgeLightFragment(),
                                getString(R.string.nav_aod_edge_lighting)),
                        ACCENT_EDGE)
        );
        rv.setAdapter(new NavAdapter(items));
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(fragment, title);
        }
    }
}
