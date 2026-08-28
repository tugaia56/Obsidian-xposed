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
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.NavAdapter;
import it.tugaia56.obsidian.ui.adapters.SectionTitleAdapter;
import it.tugaia56.obsidian.ui.adapters.SliderWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.SwitchWidgetAdapter;
import it.tugaia56.obsidian.ui.adapters.WarningBannerAdapter;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.utils.Constants;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;
import it.tugaia56.obsidian.utils.overlay.FabricatedUtil;

/**
 * Launcher hub — full mirror of OC's launcher_mods.xml (Home Layout, Folder Layout, Drawer,
 * Dock Background, Recents, Themed Icons, Miscellaneous). Pref keys match OC's exactly.
 *
 * Real, working hooks so far: Recents button color (Fabricated RRO Overlay, not a hook) and,
 * via LauncherMod (ported from OC's Launcher.java): hide app labels (Home/Drawer), the whole
 * Recenti section (Apri Dettagli App, Disabilita Pagina Recenti Precedente, Sostituisci
 * Blocco), Rimuovi Impaginazione, Nascondi Scroller, and Comportamento Personalizzato Swipe
 * Destro. Everything else is UI/prefs only, marked with [[wip_inline_suffix]] in the row
 * summary — being ported one section at a time from OC's
 * Launcher.java/DockBackground.java/ThemedIcons.java so each lands as its own testable build.
 *
 * Icone a Tema was attempted and reverted 2026-08-28 (see [[project_launcher_mods_rollout]]
 * memory) — the OEM's own themed-icon pipeline barely fires on the test device, so it never
 * worked reliably and the user asked to drop it rather than keep chasing it.
 */
public class LauncherFragment extends Fragment {

    // ── Pref keys (identical to OC's launcher_mods.xml) ────────────────────────
    private static final String KEY_REARRANGE_HOME       = "rearrange_home";
    private static final String KEY_LAUNCHER_COLUMNS      = "launcher_columns";
    private static final String KEY_LAUNCHER_ROWS         = "launcher_rows";
    private static final String KEY_DESKTOP_HIDE_LABELS   = "desktop_hide_app_labels";
    private static final String KEY_FORCE_DOCK_COLUMNS    = "force_dock_as_columns";

    private static final String KEY_REARRANGE_FOLDER      = "rearrange_folder";
    private static final String KEY_FOLDER_MAX_COLUMNS    = "folder_max_columns";
    private static final String KEY_FOLDER_MAX_ROWS       = "folder_max_rows";
    private static final String KEY_REARRANGE_PREVIEW     = "rearrange_preview";
    private static final String KEY_REMOVE_FOLDER_PAGE    = "remove_folder_pagination";

    private static final String KEY_REARRANGE_DRAWER      = "rearrange_drawer";
    private static final String KEY_DRAWER_COLUMNS        = "drawer_columns";
    private static final String KEY_DRAWER_HIDE_LABELS    = "drawer_hide_app_labels";

    private static final String KEY_OPEN_APP_DETAILS      = "launcher_open_app_details";
    private static final String KEY_DISABLE_PREV_RECENTS  = "disable_previous_recents";
    private static final String KEY_REPLACE_LOCK          = "replace_lock";

    private static final String KEY_FORCE_THEMED_ICONS    = "force_themed_launcher_icons";
    private static final String KEY_ALT_MONOCHROME        = "alternative_monochrome";
    private static final String KEY_CUSTOM_THEMED_WHERE   = "custom_themed_icons_where";

    private static final String KEY_REMOVE_HOME_PAGE      = "remove_home_pagination";
    private static final String KEY_HIDE_SCROLLER         = "hide_scroller";

    private static final String KEY_SWIPE_RIGHT_ENABLED = "launcher_custom_shelf_switch";
    private static final String KEY_SWIPE_RIGHT_MODE    = "laucher_shelf_custom"; // pref key matches OC exactly (typo included)
    private static final int SHELF_DISABLE_DISCOVER = 0;
    private static final int SHELF_REPLACE_DISCOVER = 1;
    private static final int SHELF_STOCK            = 2;

    private static final String KEY_RECENTS_BTN_COLOR = "LAUNCHER_RECENTS_BTN_COLOR";

    // Fabricated overlay names — color resource (OOS16) + the two OOS15 drawable
    // resources named by the user, so the feature works across OOS versions.
    private static final String OVERLAY_RECENTS_COLOR     = "LAUNCHER_RECENTS_0";
    private static final String OVERLAY_RECENTS_DRAWABLE1 = "LAUNCHER_RECENTS_1";
    private static final String OVERLAY_RECENTS_DRAWABLE2 = "LAUNCHER_RECENTS_2";
    private static final String[] OVERLAY_RECENTS_NAMES = {
            OVERLAY_RECENTS_COLOR, OVERLAY_RECENTS_DRAWABLE1, OVERLAY_RECENTS_DRAWABLE2};

    private static final int ACCENT_DOCK = 0xFF7C4DFF; // purple

    private static final int RECENTS_COLOR_DIALOG_ID = KEY_RECENTS_BTN_COLOR.hashCode();

    private RecyclerView mRv;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
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

    private void rebuild() {
        List<RecyclerView.Adapter<?>> chain = new ArrayList<>();

        // ── Warning banner, first thing on screen ────────────────────────────
        chain.add(new WarningBannerAdapter(getString(R.string.launcher_reboot_banner)));

        // ── Recents (first, per request) ────────────────────────────────────
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.launcher_recents))));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.launcher_app_details_title, R.string.launcher_app_details_summary, KEY_OPEN_APP_DETAILS),
                boolItem(R.string.launcher_disable_recents_previous_page_title, R.string.launcher_disable_recents_previous_page_summary, KEY_DISABLE_PREV_RECENTS),
                boolItem(R.string.launcher_replace_lock_title, R.string.launcher_replace_lock_summary, KEY_REPLACE_LOCK))));
        chain.add(new SwitchWidgetAdapter(List.of(recentsButtonColorSwitch())));
        if (ObsidianPrefs.getBoolean(KEY_RECENTS_BTN_COLOR + "_on", false)) {
            chain.add(recentsButtonColorPickerRow());
        }

        // ── Home Layout ──────────────────────────────────────────────────────
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.launcher_layout))));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.launcher_edit_layout, null, KEY_REARRANGE_HOME, false))));
        chain.add(sliderRow(getString(R.string.launcher_columns), KEY_LAUNCHER_COLUMNS, 4, 8, 4, false));
        chain.add(sliderRow(getString(R.string.launcher_rows), KEY_LAUNCHER_ROWS, 3, 10, 4, false));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.hide_app_labels, R.string.hide_app_labels_desktop, KEY_DESKTOP_HIDE_LABELS),
                boolItem(R.string.launcher_force_dock, R.string.launcher_force_dock_summary, KEY_FORCE_DOCK_COLUMNS, false))));

        // ── Folder Layout ────────────────────────────────────────────────────
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.launcher_folder_layout))));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.launcher_folder_edit_layout, null, KEY_REARRANGE_FOLDER, false))));
        chain.add(sliderRow(getString(R.string.launcher_folder_columns), KEY_FOLDER_MAX_COLUMNS, 3, 7, 3, false));
        chain.add(sliderRow(getString(R.string.launcher_folder_rows), KEY_FOLDER_MAX_ROWS, 3, 7, 3, false));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.launcher_folder_update_preview, null, KEY_REARRANGE_PREVIEW, false),
                boolItem(R.string.remove_folder_pagination_title, null, KEY_REMOVE_FOLDER_PAGE))));

        // ── Drawer ───────────────────────────────────────────────────────────
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.drawer))));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.launcher_drawer_edit_columns, null, KEY_REARRANGE_DRAWER, false))));
        chain.add(sliderRow(getString(R.string.drawer_columns), KEY_DRAWER_COLUMNS, 3, 7, 4, false));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.hide_app_labels, R.string.hide_app_labels_drawer, KEY_DRAWER_HIDE_LABELS))));

        // ── Dock background (sub-screen) ────────────────────────────────────
        chain.add(new NavAdapter(List.of(new NavAdapter.NavItem(
                R.drawable.ic_mods_tools,
                getString(R.string.dock_background),
                null,
                () -> navigate(new LauncherDockBackgroundFragment(), getString(R.string.dock_background)),
                ACCENT_DOCK))));

        // ── Themed icons ─────────────────────────────────────────────────────
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.launcher_themed_icons))));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.force_themed_launcher_icons, R.string.force_themed_launcher_icons_summary, KEY_FORCE_THEMED_ICONS, false),
                boolItem(R.string.alternative_themed_icons_title, R.string.alternative_themed_icons_summary, KEY_ALT_MONOCHROME, false))));
        chain.add(themedIconsWhereRow());

        // ── Miscellaneous ────────────────────────────────────────────────────
        chain.add(new SectionTitleAdapter(List.of(getString(R.string.misc_category))));
        chain.add(new SwitchWidgetAdapter(List.of(
                boolItem(R.string.remove_home_pagination, null, KEY_REMOVE_HOME_PAGE),
                boolItem(R.string.hide_scroller, R.string.hide_scroller_summary, KEY_HIDE_SCROLLER))));
        chain.add(swipeRightRow());

        android.os.Parcelable scrollState = mRv.getLayoutManager() != null
                ? mRv.getLayoutManager().onSaveInstanceState() : null;
        mRv.setAdapter(new ConcatAdapter(chain.toArray(new RecyclerView.Adapter<?>[0])));
        if (scrollState != null && mRv.getLayoutManager() != null) {
            mRv.getLayoutManager().onRestoreInstanceState(scrollState);
        }
    }

    // ── Pulsante Recenti color — real Fabricated RRO Overlay on com.android.launcher,
    // resource "toggle_bar_apply_btn_enabled_color" (color, OOS16) plus the two OOS15
    // drawable resources of the same name / "recent_clear_circle" (drawables holding a
    // raw color value render as a solid ColorDrawable, same trick used for the circle bg). ──

    /** Enable switch — no persistent swatch shown; the colour picker is a separate row below,
     *  only visible while enabled (same pattern as Illuminazione Bordi's colour mode). */
    private SwitchWidgetAdapter.SwitchItem recentsButtonColorSwitch() {
        boolean on = ObsidianPrefs.getBoolean(KEY_RECENTS_BTN_COLOR + "_on", false);
        SwitchWidgetAdapter.SwitchItem item = new SwitchWidgetAdapter.SwitchItem(
                getString(R.string.launcher_recents_button_color_title),
                getString(R.string.launcher_recents_color_no_reboot), on, null);
        item.onChanged = () -> {
            ObsidianPrefs.putBoolean(KEY_RECENTS_BTN_COLOR + "_on", item.checked);
            if (item.checked) {
                new Thread(() -> applyRecentsBtnColor(ObsidianPrefs.getInt(KEY_RECENTS_BTN_COLOR, 0xFF6200EE))).start();
            } else {
                new Thread(this::disableRecentsBtnColor).start();
            }
            rebuild();
        };
        return item;
    }

    private ListWidgetAdapter recentsButtonColorPickerRow() {
        ListWidgetAdapter.ListItem item = new ListWidgetAdapter.ListItem(
                getString(R.string.launcher_recents_button_color_title), recentsColorLabel(),
                this::showRecentsColorModeDialog);
        return new ListWidgetAdapter(List.of(item));
    }

    private String recentsColorLabel() {
        if (ObsidianPrefs.getBoolean(KEY_RECENTS_BTN_COLOR + "_use_accent", false)) {
            return getString(R.string.color_mode_accent);
        }
        return String.format("#%06X", 0xFFFFFF & ObsidianPrefs.getInt(KEY_RECENTS_BTN_COLOR, 0xFF6200EE));
    }

    /** RRO overlay applied once at selection time (no live Xposed hook to re-resolve at draw
     *  time), so "Accento" bakes the CURRENT accent colour in — same as "Personalizzato", if the
     *  global accent changes later this needs re-selecting to pick up the new value. */
    private void showRecentsColorModeDialog() {
        String[] entries = { getString(R.string.color_mode_accent), getString(R.string.color_mode_custom) };
        int current = ObsidianPrefs.getBoolean(KEY_RECENTS_BTN_COLOR + "_use_accent", false) ? 0 : 1;
        final int[] selected = {current};
        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.launcher_recents_button_color_title)
                .setSingleChoiceItems(entries, current, (d, which) -> selected[0] = which)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    boolean useAccent = selected[0] == 0;
                    ObsidianPrefs.putBoolean(KEY_RECENTS_BTN_COLOR + "_use_accent", useAccent);
                    if (useAccent) {
                        int color = ObsidianTheme.accentColor();
                        ObsidianPrefs.putInt(KEY_RECENTS_BTN_COLOR, color);
                        new Thread(() -> applyRecentsBtnColor(color)).start();
                        rebuild();
                    } else if (getActivity() instanceof MainActivity) {
                        int currentColor = ObsidianPrefs.getInt(KEY_RECENTS_BTN_COLOR, 0xFF6200EE);
                        ((MainActivity) getActivity()).showColorPickerDialog(RECENTS_COLOR_DIALOG_ID, currentColor, true, true, true);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        if (event.dialogId() != RECENTS_COLOR_DIALOG_ID) return;
        ObsidianPrefs.putInt(KEY_RECENTS_BTN_COLOR, event.color());
        new Thread(() -> applyRecentsBtnColor(event.color())).start();
        rebuild();
    }

    private void applyRecentsBtnColor(int color) {
        String hex = fmt(color);
        String pkg = Constants.LAUNCHER;
        FabricatedUtil.buildAndEnableOverlays(
                new Object[]{pkg, OVERLAY_RECENTS_COLOR,     "color",    "toggle_bar_apply_btn_enabled_color", hex},
                new Object[]{pkg, OVERLAY_RECENTS_DRAWABLE1, "drawable", "toggle_bar_apply_btn_enabled_color", hex},
                new Object[]{pkg, OVERLAY_RECENTS_DRAWABLE2, "drawable", "recent_clear_circle",                hex});
    }

    private void disableRecentsBtnColor() {
        FabricatedUtil.disableOverlays(OVERLAY_RECENTS_NAMES);
    }

    private static String fmt(int color) {
        return String.format("0x%08X", 0xFFFFFFFFL & color);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SwitchWidgetAdapter.SwitchItem boolItem(int titleRes, Integer summaryRes, String key) {
        return boolItem(titleRes, summaryRes, key, true);
    }

    /** implemented=false appends a "coming soon" marker to the summary — the switch still
     *  saves its pref (so the value is ready once the hook lands), it just does nothing yet. */
    private SwitchWidgetAdapter.SwitchItem boolItem(int titleRes, Integer summaryRes, String key, boolean implemented) {
        String summary = summaryRes != null ? getString(summaryRes) : null;
        if (!implemented) summary = (summary != null ? summary : "") + getString(R.string.wip_inline_suffix);
        SwitchWidgetAdapter.SwitchItem item = new SwitchWidgetAdapter.SwitchItem(
                getString(titleRes), summary,
                ObsidianPrefs.getBoolean(key, false),
                null);
        item.onChanged = () -> ObsidianPrefs.putBoolean(key, item.checked);
        return item;
    }

    /** A row with an inline slider for an integer pref (mirrors OC's slider prefs). */
    private SliderWidgetAdapter sliderRow(String title, String key, int min, int max, int def) {
        return sliderRow(title, key, min, max, def, true);
    }

    private SliderWidgetAdapter sliderRow(String title, String key, int min, int max, int def, boolean implemented) {
        if (!implemented) title = title + getString(R.string.wip_inline_suffix);
        int current = ObsidianPrefs.getInt(key, def);
        SliderWidgetAdapter.SliderItem item = new SliderWidgetAdapter.SliderItem(
                title, current, min, max, "", def,
                value -> ObsidianPrefs.putInt(key, value));
        return new SliderWidgetAdapter(List.of(item));
    }

    /** Multi-select dialog for "where to apply themed icons" (workspace/drawer/folder/search/taskbar). */
    private ListWidgetAdapter themedIconsWhereRow() {
        String[] entries = {
                getString(R.string.themed_icons_workspace), getString(R.string.themed_icons_drawer),
                getString(R.string.themed_icons_folder), getString(R.string.themed_icons_search),
                getString(R.string.themed_icons_taskbar)
        };
        final ListWidgetAdapter[] adapterRef = new ListWidgetAdapter[1];
        ListWidgetAdapter.ListItem item = new ListWidgetAdapter.ListItem(
                getString(R.string.themed_icons_where_switch) + getString(R.string.wip_inline_suffix),
                themedIconsWhereSummary(entries),
                () -> showThemedIconsWhereDialog(entries, adapterRef[0]));
        ListWidgetAdapter adapter = new ListWidgetAdapter(List.of(item));
        adapterRef[0] = adapter;
        return adapter;
    }

    private String themedIconsWhereSummary(String[] entries) {
        boolean[] selected = themedIconsWhereSelection(entries.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.length; i++) {
            if (selected[i]) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entries[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : getString(R.string.general_off);
    }

    private boolean[] themedIconsWhereSelection(int count) {
        String stored = ObsidianPrefs.getString(KEY_CUSTOM_THEMED_WHERE, "0,1,2,3,4");
        boolean[] result = new boolean[count];
        for (String s : stored.split(",")) {
            try {
                int idx = Integer.parseInt(s.trim());
                if (idx >= 0 && idx < count) result[idx] = true;
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private void showThemedIconsWhereDialog(String[] entries, ListWidgetAdapter adapter) {
        boolean[] checked = themedIconsWhereSelection(entries.length);
        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.themed_icons_where_title)
                .setMultiChoiceItems(entries, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(i);
                        }
                    }
                    ObsidianPrefs.putString(KEY_CUSTOM_THEMED_WHERE, sb.toString());
                    adapter.getItems().get(0).valueSummary = themedIconsWhereSummary(entries);
                    adapter.notifyItemChanged(0);
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    /** Collapses OC's separate enable-switch + 3-way radio group into a single single-choice
     *  dialog — any explicit pick sets [[KEY_SWIPE_RIGHT_ENABLED]]=true, matching what "Discover
     *  (default)" already behaves like when the switch is off, so there's no separate off state. */
    private ListWidgetAdapter swipeRightRow() {
        ListWidgetAdapter.ListItem item = new ListWidgetAdapter.ListItem(
                getString(R.string.custom_swipe_right_behavior_title),
                swipeRightSummary(),
                this::showSwipeRightDialog);
        return new ListWidgetAdapter(List.of(item));
    }

    private String swipeRightSummary() {
        int mode = ObsidianPrefs.getBoolean(KEY_SWIPE_RIGHT_ENABLED, false)
                ? ObsidianPrefs.getInt(KEY_SWIPE_RIGHT_MODE, SHELF_STOCK) : SHELF_STOCK;
        switch (mode) {
            case SHELF_DISABLE_DISCOVER: return getString(R.string.swipe_right_disable_discover);
            case SHELF_REPLACE_DISCOVER: return getString(R.string.swipe_right_replace_discover);
            default: return getString(R.string.swipe_right_enable_discover);
        }
    }

    private void showSwipeRightDialog() {
        String[] entries = {
                getString(R.string.swipe_right_enable_discover),
                getString(R.string.swipe_right_disable_discover),
                getString(R.string.swipe_right_replace_discover)
        };
        int[] values = { SHELF_STOCK, SHELF_DISABLE_DISCOVER, SHELF_REPLACE_DISCOVER };
        int current = ObsidianPrefs.getBoolean(KEY_SWIPE_RIGHT_ENABLED, false)
                ? ObsidianPrefs.getInt(KEY_SWIPE_RIGHT_MODE, SHELF_STOCK) : SHELF_STOCK;
        int checkedIndex = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == current) checkedIndex = i;

        ObsidianTheme.themeDialog(new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.custom_swipe_right_behavior_title)
                .setSingleChoiceItems(entries, checkedIndex, (d, which) -> {
                    ObsidianPrefs.putBoolean(KEY_SWIPE_RIGHT_ENABLED, true);
                    ObsidianPrefs.putInt(KEY_SWIPE_RIGHT_MODE, values[which]);
                    d.dismiss();
                    rebuild();
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    private void navigate(Fragment fragment, String title) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(fragment, title);
        }
    }
}
