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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.GroupUtils;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.ui.adapters.SliderWidgetAdapter;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;

/**
 * "Preferenza Icona Ricarica Batteria" — reale OC categoria battery_charging_icon:
 * 21 icone di ricarica (stesso set/ordine di OC), sostituiscono il fulmine standard
 * sull'icona batteria in stato bar quando in carica. Margini + dimensione, stessi
 * range di OC.
 */
public class BatteryChargingIconFragment extends Fragment {

    public static final String PREF_STYLE         = "battery_charging_icon_style";
    public static final String PREF_USE_ACCENT    = "battery_charging_icon_use_accent";
    public static final String PREF_CUSTOM_COLOR  = "battery_charging_icon_custom_color";
    private static final String PREF_MARGIN_LEFT  = "battery_charging_icon_margin_left";
    private static final String PREF_MARGIN_RIGHT = "battery_charging_icon_margin_right";
    private static final String PREF_ICON_SIZE    = "battery_charging_icon_size";
    private static final int DIALOG_CUSTOM_COLOR  = PREF_CUSTOM_COLOR.hashCode();

    private RecyclerView mRv;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        if (event.dialogId() != DIALOG_CUSTOM_COLOR) return;
        ObsidianPrefs.putInt(PREF_CUSTOM_COLOR, event.color());
        rebuild();
    }

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
        mRv = (RecyclerView) view;
        rebuild();
    }

    @Override
    public void onResume() {
        super.onResume();
        rebuild(); // refresh style label after returning from the picker
    }

    private void rebuild() {
        List<RecyclerView.Adapter<?>> chain = new ArrayList<>();

        chain.add(new SectionTitleAdapter(List.of(getString(R.string.battery_charging_icon_style_title))));
        ListWidgetAdapter.ListItem styleItem = new ListWidgetAdapter.ListItem(
                getString(R.string.battery_charging_icon_style_title),
                currentStyleLabel(), this::openStylePicker);
        GroupUtils.addGroup(chain, List.of((Object) styleItem));

        GroupUtils.addGroup(chain, List.of((Object) colorModeItem()));

        chain.add(new SectionTitleAdapter(List.of(getString(R.string.battery_charging_icon_layout_section))));
        List<Object> rows = List.of(
                slider(getString(R.string.battery_charging_icon_margin_left), PREF_MARGIN_LEFT, 0, 6, 1, "dp"),
                slider(getString(R.string.battery_charging_icon_margin_right), PREF_MARGIN_RIGHT, 0, 6, 1, "dp"),
                slider(getString(R.string.battery_charging_icon_size), PREF_ICON_SIZE, 8, 20, 14, "dp")
        );
        GroupUtils.addGroup(chain, rows);

        android.os.Parcelable scrollState = mRv.getLayoutManager() != null
                ? mRv.getLayoutManager().onSaveInstanceState() : null;
        mRv.setAdapter(new ConcatAdapter(chain.toArray(new RecyclerView.Adapter<?>[0])));
        if (scrollState != null && mRv.getLayoutManager() != null) {
            mRv.getLayoutManager().onRestoreInstanceState(scrollState);
        }
    }

    private String currentStyleLabel() {
        String[] entries = getResources().getStringArray(R.array.battery_charging_icon_style_entries);
        int idx = ObsidianPrefs.getInt(PREF_STYLE, 0);
        return (idx >= 0 && idx < entries.length) ? entries[idx] : entries[0];
    }

    private ListWidgetAdapter.ListItem colorModeItem() {
        return new ListWidgetAdapter.ListItem(
                getString(R.string.battery_charging_icon_color_title),
                colorModeLabel(), this::showColorModeDialog);
    }

    private String colorModeLabel() {
        boolean useAccent = ObsidianPrefs.getBoolean(PREF_USE_ACCENT, true);
        if (useAccent) return getString(R.string.battery_charging_icon_use_accent);
        return String.format("#%06X", 0xFFFFFF & ObsidianPrefs.getInt(PREF_CUSTOM_COLOR, 0xFFFFFFFF));
    }

    private void showColorModeDialog() {
        String[] entries = {
                getString(R.string.battery_charging_icon_use_accent),
                getString(R.string.battery_charging_icon_custom_color)
        };
        int current = ObsidianPrefs.getBoolean(PREF_USE_ACCENT, true) ? 0 : 1;
        final int[] selected = {current};
        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.battery_charging_icon_color_title)
                .setSingleChoiceItems(entries, current, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    boolean useAccent = selected[0] == 0;
                    ObsidianPrefs.putBoolean(PREF_USE_ACCENT, useAccent);
                    rebuild();
                    // "Personalizzato" non ha una riga persistente propria — sceglierlo apre
                    // subito il color picker, come altrove nell'app (es. AodEdgeLightFragment).
                    if (!useAccent && getActivity() instanceof MainActivity) {
                        int currentColor = ObsidianPrefs.getInt(PREF_CUSTOM_COLOR, 0xFFFFFFFF);
                        ((MainActivity) getActivity()).showColorPickerDialog(DIALOG_CUSTOM_COLOR, currentColor, true, true, true);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    private void openStylePicker() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(new BatteryChargingIconStyleFragment(),
                    getString(R.string.battery_charging_icon_style_title));
        }
    }

    private SliderWidgetAdapter.SliderItem slider(String title, String key, int min, int max, int def, String unit) {
        int current = ObsidianPrefs.getInt(key, def);
        return new SliderWidgetAdapter.SliderItem(title, current, min, max, unit, def,
                value -> ObsidianPrefs.putInt(key, value));
    }
}
