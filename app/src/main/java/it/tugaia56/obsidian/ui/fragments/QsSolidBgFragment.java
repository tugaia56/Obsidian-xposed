package it.tugaia56.obsidian.ui.fragments;

import android.os.Bundle;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.ui.adapters.SliderWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SwitchWidgetAdapter;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.utils.ObsidianPrefs;

/** Solid QS background sub-screen: switch + color picker + opacity slider. */
public class QsSolidBgFragment extends Fragment {

    private static final String PREF_QS_BG       = "DST_QS_BG_ENABLED";
    private static final String PREF_QS_BG_COLOR  = "OBS_QS_BG_COLOR";
    private static final String PREF_QS_BG_ALPHA  = "OBS_QS_BG_ALPHA";
    private static final int    DEFAULT_COLOR      = 0xFF1B2029;

    private int mColorDialogId;
    private ListWidgetAdapter colorAdapter;
    private ListWidgetAdapter.ListItem colorItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        mColorDialogId = View.generateViewId();
    }

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

        // Switch
        SwitchWidgetAdapter.SwitchItem switchItem = new SwitchWidgetAdapter.SwitchItem(
                getString(R.string.dst_qs_solid_bg),
                getString(R.string.dst_qs_solid_bg_summary),
                R.drawable.ic_qs,
                ObsidianPrefs.getBoolean(PREF_QS_BG, false),
                null);
        switchItem.onChanged = () -> {
            ObsidianPrefs.putBoolean(PREF_QS_BG, switchItem.checked);
            Toast.makeText(requireContext(), R.string.obs_restart_ui_hint, Toast.LENGTH_SHORT).show();
        };

        // Color row
        colorItem = new ListWidgetAdapter.ListItem(
                getString(R.string.dst_qs_bg_color),
                colorHex(),
                () -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showColorPickerDialog(
                                mColorDialogId,
                                ObsidianPrefs.getInt(PREF_QS_BG_COLOR, DEFAULT_COLOR),
                                true, true, true);
                    }
                });
        colorItem.groupPos = it.tugaia56.obsidian.utils.ObsidianTheme.GroupPos.TOP;
        colorAdapter = new ListWidgetAdapter(List.of(colorItem));

        // Alpha slider
        SliderWidgetAdapter.SliderItem alphaItem = new SliderWidgetAdapter.SliderItem(
                getString(R.string.dst_qs_bg_alpha),
                ObsidianPrefs.getInt(PREF_QS_BG_ALPHA, 100),
                0, 100, "%", 100,
                value -> {
                    ObsidianPrefs.putInt(PREF_QS_BG_ALPHA, value);
                    Toast.makeText(requireContext(), R.string.obs_restart_ui_hint, Toast.LENGTH_SHORT).show();
                });
        alphaItem.groupPos = it.tugaia56.obsidian.utils.ObsidianTheme.GroupPos.BOTTOM;

        rv.setAdapter(new ConcatAdapter(
                new SectionTitleAdapter(List.of(getString(R.string.dst_qs_solid_bg))),
                new SwitchWidgetAdapter(List.of(switchItem)),
                new SectionTitleAdapter(List.of(getString(R.string.dst_qs_bg_customize))),
                colorAdapter,
                new SliderWidgetAdapter(List.of(alphaItem))
        ));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        if (event.dialogId() != mColorDialogId) return;
        ObsidianPrefs.putInt(PREF_QS_BG_COLOR, event.color());
        colorItem.valueSummary = colorHex();
        if (colorAdapter != null) colorAdapter.notifyDataSetChanged();
        Toast.makeText(requireContext(), R.string.obs_restart_ui_hint, Toast.LENGTH_SHORT).show();
    }

    private String colorHex() {
        int c = ObsidianPrefs.getInt(PREF_QS_BG_COLOR, DEFAULT_COLOR);
        return String.format(Locale.US, "#%06X", c & 0xFFFFFF);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
