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
 * Lock Screen hub — mirrors OC's lock screen sections (minus Now Bar, Notification Peek,
 * and random PIN numbers, per explicit request). "Opzioni" hosts what used to be the
 * standalone "Varie" card here (LockScreenButtonsFragment, now deleted).
 *
 * Order: Orologio, Meteo, Widget, Icona impronta, Opzioni SdB. Orologio/Meteo/Widget are
 * UI-only ports of OC's lockscreen clock/weather/widgets screens — visible but not wired
 * to a hook yet (LockscreenClockFragment / LockscreenWeatherFragment / LockscreenWidgetsFragment).
 *
 * Sections below marked WIP are scaffolding only — the real Xposed hooks need per-feature
 * reverse engineering (class/resource names differ per ROM, same as the status bar work).
 */
public class LockScreenFragment extends Fragment {

    private static final int ACCENT_CLOCK       = 0xFF00BCD4; // cyan
    private static final int ACCENT_WEATHER     = 0xFF4CAF50; // green
    private static final int ACCENT_WIDGETS     = 0xFF009688; // teal
    private static final int ACCENT_FINGERPRINT = 0xFFE91E63; // pink
    private static final int ACCENT_OPTIONS     = 0xFF7C4DFF; // purple

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
                        getString(R.string.nav_lock_clock_new),
                        getString(R.string.lockscreen_clock_switch),
                        () -> navigate(new LockscreenClockFragment(),
                                getString(R.string.nav_lock_clock_new)),
                        ACCENT_CLOCK),

                new NavAdapter.NavItem(
                        R.drawable.ic_palette,
                        getString(R.string.nav_lock_weather),
                        getString(R.string.nav_lock_weather_summary),
                        () -> navigate(new LockscreenWeatherFragment(),
                                getString(R.string.nav_lock_weather)),
                        ACCENT_WEATHER),

                new NavAdapter.NavItem(
                        R.drawable.ic_mods_tools,
                        getString(R.string.nav_lock_widgets),
                        getString(R.string.nav_lock_widgets_summary),
                        () -> navigate(new LockscreenWidgetsFragment(),
                                getString(R.string.nav_lock_widgets)),
                        ACCENT_WIDGETS),

                new NavAdapter.NavItem(
                        R.drawable.ic_lock,
                        getString(R.string.nav_fingerprint_icon),
                        getString(R.string.nav_fingerprint_icon_summary),
                        () -> navigate(new FingerprintIconFragment(),
                                getString(R.string.nav_fingerprint_icon)),
                        ACCENT_FINGERPRINT),

                new NavAdapter.NavItem(
                        R.drawable.ic_lock,
                        getString(R.string.nav_lock_screen_options),
                        getString(R.string.nav_lock_screen_options_summary),
                        () -> navigate(new LockScreenOptionsFragment(),
                                getString(R.string.nav_lock_screen_options)),
                        ACCENT_OPTIONS)
        );
        rv.setAdapter(new NavAdapter(items));
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(fragment, title);
        }
    }
}
