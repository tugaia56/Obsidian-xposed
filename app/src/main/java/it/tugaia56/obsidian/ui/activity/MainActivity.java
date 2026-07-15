package it.tugaia56.obsidian.ui.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import org.greenrobot.eventbus.EventBus;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.ui.fragments.HomeFragment;
import it.tugaia56.obsidian.utils.AppUtils;

public class MainActivity extends AppCompatActivity implements ColorPickerDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showHomeActionBar();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    // ── Restart SystemUI menu item ─────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_restart_systemui) {
            AppUtils.restartSystemUI(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    /**
     * Navigate to a sub-fragment with a back arrow and title in the ActionBar.
     * The title is stored as the back-stack entry name so onResume() can restore it.
     */
    public void navigateTo(Fragment fragment, String title) {
        setSubFragmentActionBar(title);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.frag_enter,    R.anim.frag_exit,
                        R.anim.frag_pop_enter, R.anim.frag_pop_exit)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(title)   // store title so onResume() can recover it
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // After popping, check how many entries remain
        int remaining = getSupportFragmentManager().getBackStackEntryCount();
        if (remaining == 0) {
            showHomeActionBar();
        } else {
            // Restore the title of the now-visible sub-fragment
            String title = getSupportFragmentManager()
                    .getBackStackEntryAt(remaining - 1).getName();
            setSubFragmentActionBar(title != null ? title : "");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // After SystemUI restart (or any app resume), the ActionBar may have been
        // reset. Restore back-arrow + title if we are still inside a sub-fragment.
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count > 0) {
            String title = getSupportFragmentManager()
                    .getBackStackEntryAt(count - 1).getName();
            setSubFragmentActionBar(title != null ? title : "");
        }
        // count == 0 → home ActionBar was already set in onCreate / previous onBackPressed
    }

    private void setSubFragmentActionBar(String title) {
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setCustomView(null);
        ab.setDisplayShowCustomEnabled(false);
        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowTitleEnabled(true);
        ab.setTitle(title);
    }

    private void showHomeActionBar() {
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(getLayoutInflater().inflate(R.layout.actionbar_header, null));
    }

    // ── Color picker ───────────────────────────────────────────────────────────

    public void showColorPickerDialog(int dialogId, int color, boolean alpha,
                                      boolean shades, boolean presets) {
        ColorPickerDialog.newBuilder()
                .setDialogId(dialogId)
                .setColor(color)
                .setShowAlphaSlider(alpha)
                .setShowColorShades(shades)
                .show(this);
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        EventBus.getDefault().post(new ColorSelectedEvent(dialogId, color));
    }

    @Override
    public void onDialogDismissed(int dialogId) {}
}
