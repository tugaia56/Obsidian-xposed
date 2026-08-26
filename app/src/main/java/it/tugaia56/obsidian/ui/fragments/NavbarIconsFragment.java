package it.tugaia56.obsidian.ui.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.List;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.adapters.ListWidgetAdapter;
import it.tugaia56.obsidian.utils.AppUtils;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;
import it.tugaia56.obsidian.utils.overlay.OverlayUtil;
import it.tugaia56.obsidian.utils.overlay.compiler.NavbarIconsCompiler;

/**
 * Pack Icone Barra di Navigazione — porting reale di OC: icone vere (copiate dai veri overlay
 * asset di OC, assets/Overlays/com.android.systemui/NB1..NB9) compilate in un overlay per
 * com.android.systemui via lo stesso motore di "Icone Impostazioni" (aapt2/zipalign/firma
 * custom, installato da root come overlay di partizione — vedi NavbarIconsCompiler). Un solo
 * overlay (NIP1): scegliere un pack diverso ricompila e sovrascrive lo stesso file. Come per
 * Icone Impostazioni, la prima attivazione richiede un riavvio prima che PackageManagerService
 * scansioni /system/product/overlay.
 */
public class NavbarIconsFragment extends Fragment {

    private static final String TAG = "NavbarIconsFragment";
    private static final String OVERLAY_NAME = "ObsidianComponentNIP1.overlay";
    private static final String KEY_SELECTED_PACK = "navbar_icons_selected_pack";
    private static final String KEY_PENDING_REBOOT = "navbar_icons_pending_reboot";

    private record Pack(String title, int back, int home, int recent) {}

    private final List<Pack> mPacks = List.of(
            new Pack("Android", R.drawable.navbar_pack_android_back, R.drawable.navbar_pack_android_home, R.drawable.navbar_pack_android_recent),
            new Pack("Asus", R.drawable.navbar_pack_asus_back, R.drawable.navbar_pack_asus_home, R.drawable.navbar_pack_asus_recent),
            new Pack("Dora", R.drawable.navbar_pack_dora_back, R.drawable.navbar_pack_dora_home, R.drawable.navbar_pack_dora_recent),
            new Pack("Moto", R.drawable.navbar_pack_moto_back, R.drawable.navbar_pack_moto_home, R.drawable.navbar_pack_moto_recent),
            new Pack("Nexus", R.drawable.navbar_pack_nexus_back, R.drawable.navbar_pack_nexus_home, R.drawable.navbar_pack_nexus_recent),
            new Pack("Old", R.drawable.navbar_pack_old_back, R.drawable.navbar_pack_old_home, R.drawable.navbar_pack_old_recent),
            new Pack("One UI", R.drawable.navbar_pack_oneui_back, R.drawable.navbar_pack_oneui_home, R.drawable.navbar_pack_oneui_recent),
            new Pack("Sammy", R.drawable.navbar_pack_sammy_back, R.drawable.navbar_pack_sammy_home, R.drawable.navbar_pack_sammy_recent),
            new Pack("Tecno", R.drawable.navbar_pack_tecno_back, R.drawable.navbar_pack_tecno_home, R.drawable.navbar_pack_tecno_recent)
    );

    private int mSelected = -1;
    private int mApplied = -1;
    private boolean mPendingReboot = false;
    private boolean mActive = false;
    private boolean mBusy = false;
    private RecyclerView mRv;

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

        int appliedPack = ObsidianPrefs.getInt(KEY_SELECTED_PACK, -1);
        mPendingReboot = ObsidianPrefs.getBoolean(KEY_PENDING_REBOOT, false);
        mActive = appliedPack >= 0 && OverlayUtil.isOverlayEnabled(OVERLAY_NAME);
        if (mActive) mPendingReboot = false;
        mApplied = appliedPack >= 1 ? appliedPack - 1 : -1;
        mSelected = mApplied;

        rebuild();
    }

    private void rebuild() {
        rebuild(false);
    }

    private void rebuild(boolean scrollToSelection) {
        ListWidgetAdapter.ListItem notice = new ListWidgetAdapter.ListItem(
                getString(R.string.navbar_icons_preview_title), getString(R.string.navbar_icons_preview_body), null);
        notice.useAccentColor = false;

        java.util.List<RecyclerView.Adapter<?>> chain = new java.util.ArrayList<>();
        chain.add(new ListWidgetAdapter(List.of(notice)));
        int scrollTargetPos = -1;

        if (mSelected < 0) {
            java.util.List<Integer> all = new java.util.ArrayList<>();
            for (int i = 0; i < mPacks.size(); i++) all.add(i);
            chain.add(new PackAdapter(all));
            chain.add(new ButtonsAdapter());
        } else {
            java.util.List<Integer> before = new java.util.ArrayList<>();
            for (int i = 0; i < mSelected; i++) before.add(i);
            java.util.List<Integer> after = new java.util.ArrayList<>();
            for (int i = mSelected + 1; i < mPacks.size(); i++) after.add(i);

            int runningCount = 1; // notice
            if (!before.isEmpty()) {
                chain.add(new PackAdapter(before));
                runningCount += before.size();
            }
            scrollTargetPos = runningCount;
            chain.add(new PackAdapter(List.of(mSelected)));
            chain.add(new ButtonsAdapter());
            if (!after.isEmpty()) chain.add(new PackAdapter(after));
        }

        mRv.setAdapter(new ConcatAdapter(chain));

        if (scrollToSelection && scrollTargetPos >= 0) {
            RecyclerView.LayoutManager lm = mRv.getLayoutManager();
            if (lm instanceof LinearLayoutManager) {
                ((LinearLayoutManager) lm).scrollToPositionWithOffset(scrollTargetPos, dp(8));
            }
        }
    }

    private void onPackTapped(int pos) {
        if (mBusy) return;
        mSelected = (mSelected == pos) ? -1 : pos;
        rebuild(true);
    }

    private void onApplyClicked() {
        if (mBusy || mSelected < 0) return;

        if (!AppUtils.hasStoragePermission()) {
            AppUtils.requestStoragePermission(requireContext());
            return;
        }

        setBusy(true);
        int pack = mSelected + 1;
        new Thread(() -> {
            boolean erroredOut;
            try {
                erroredOut = NavbarIconsCompiler.buildOverlay(pack);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                erroredOut = true;
            }
            boolean success = !erroredOut;
            boolean nowActive = success && OverlayUtil.isOverlayEnabled(OVERLAY_NAME);
            if (success) {
                ObsidianPrefs.putInt(KEY_SELECTED_PACK, pack);
                ObsidianPrefs.putBoolean(KEY_PENDING_REBOOT, !nowActive);
            }
            boolean finalSuccess = success;
            boolean finalActive = nowActive;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                setBusy(false);
                if (finalSuccess) {
                    mApplied = mSelected;
                    mActive = finalActive;
                    mPendingReboot = !finalActive;
                    rebuild();
                    Toast.makeText(requireContext(),
                            finalActive ? R.string.toast_applied : R.string.navbar_icons_build_ok,
                            finalActive ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), R.string.toast_error, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void onDisableClicked() {
        if (mBusy || mApplied < 0) return;

        setBusy(true);
        new Thread(() -> {
            OverlayUtil.disableOverlays(OVERLAY_NAME);
            ObsidianPrefs.putInt(KEY_SELECTED_PACK, -1);
            ObsidianPrefs.putBoolean(KEY_PENDING_REBOOT, false);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                setBusy(false);
                mApplied = -1;
                mPendingReboot = false;
                mActive = false;
                mSelected = -1;
                rebuild();
                Toast.makeText(requireContext(), R.string.toast_applied, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void setBusy(boolean busy) {
        mBusy = busy;
        rebuild();
    }

    // ── Pack cards ───────────────────────────────────────────────────────────

    private class PackAdapter extends RecyclerView.Adapter<PackAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView title;
            ImageView check;
            ImageView[] previews = new ImageView[3];
            VH(View v) { super(v); }
        }

        private final List<Integer> indices;
        PackAdapter(List<Integer> indices) { this.indices = indices; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(dp(12), dp(5), dp(12), dp(5));
            card.setLayoutParams(cardLp);
            card.setRadius(dp(18));
            card.setCardElevation(0);
            card.setCardBackgroundColor(ObsidianTheme.cardColor());
            card.setStrokeColor(ObsidianTheme.textColor(0x33));
            card.setStrokeWidth(dp(1));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));

            LinearLayout titleCol = new LinearLayout(requireContext());
            titleCol.setOrientation(LinearLayout.HORIZONTAL);
            titleCol.setGravity(Gravity.CENTER_VERTICAL);
            titleCol.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView title = new TextView(requireContext());
            title.setTextColor(ObsidianTheme.textColor());
            title.setTextSize(15);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            titleCol.addView(title);

            ImageView check = new ImageView(requireContext());
            LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(dp(18), dp(18));
            checkLp.setMarginStart(dp(8));
            check.setLayoutParams(checkLp);
            check.setImageDrawable(requireContext().getDrawable(android.R.drawable.checkbox_on_background));
            check.setImageTintList(ColorStateList.valueOf(0xFF7C4DFF));
            check.setVisibility(View.INVISIBLE);
            titleCol.addView(check);

            row.addView(titleCol);

            View spacerBefore = new View(requireContext());
            spacerBefore.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));
            row.addView(spacerBefore);

            LinearLayout iconsRow = new LinearLayout(requireContext());
            iconsRow.setOrientation(LinearLayout.HORIZONTAL);
            ImageView[] previews = new ImageView[3];
            for (int i = 0; i < 3; i++) {
                FrameLayout cell = new FrameLayout(requireContext());
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(dp(52), dp(52));
                if (i > 0) cellLp.setMarginStart(dp(6));
                cell.setLayoutParams(cellLp);

                ImageView iv = new ImageView(requireContext());
                FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dp(36), dp(36));
                ivLp.gravity = Gravity.CENTER;
                iv.setLayoutParams(ivLp);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setImageTintList(ColorStateList.valueOf(ObsidianTheme.textColor()));
                cell.addView(iv);
                iconsRow.addView(cell);
                previews[i] = iv;
            }
            row.addView(iconsRow);

            View spacerAfter = new View(requireContext());
            spacerAfter.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));
            row.addView(spacerAfter);

            card.addView(row);

            VH h = new VH(card);
            h.card = card;
            h.title = title;
            h.check = check;
            h.previews = previews;
            return h;
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            int idx = indices.get(pos);
            Pack pack = mPacks.get(idx);
            h.title.setText(pack.title());
            h.previews[0].setImageResource(pack.back());
            h.previews[1].setImageResource(pack.home());
            h.previews[2].setImageResource(pack.recent());

            boolean selected = idx == mSelected;
            boolean isActive = idx == mApplied;
            h.check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            h.check.setImageTintList(ColorStateList.valueOf(isActive ? 0xFF7C4DFF : ObsidianTheme.textColor()));
            h.card.setStrokeColor(selected ? 0xFF7C4DFF : ObsidianTheme.textColor(0x33));
            h.card.setAlpha(mBusy ? 0.6f : 1f);

            h.card.setOnClickListener(v -> onPackTapped(idx));
        }

        @Override public int getItemCount() { return indices.size(); }
    }

    // ── Status + Applica/Disabilita ─────────────────────────────────────────

    private class ButtonsAdapter extends RecyclerView.Adapter<ButtonsAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView status;
            MaterialButton apply;
            MaterialButton disable;
            VH(View v) { super(v); }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout root = new LinearLayout(requireContext());
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(16), dp(8), dp(16), dp(16));

            TextView status = new TextView(requireContext());
            status.setTextColor(ObsidianTheme.textColor(0xCC));
            status.setTextSize(13);
            status.setPadding(0, 0, 0, dp(10));
            root.addView(status);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            int dstBg = ObsidianTheme.bgColor();
            int accent = ObsidianTheme.accentColor();

            MaterialButton apply = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            apply.setText(R.string.navbar_icons_apply);
            apply.setAllCaps(false);
            apply.setTextColor(ObsidianTheme.textColor());
            apply.setTextSize(14);
            apply.setPadding(dp(4), 0, dp(4), 0);
            LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(0, dp(52), 1f);
            applyLp.setMarginEnd(dp(8));
            apply.setLayoutParams(applyLp);
            apply.setInsetTop(0);
            apply.setInsetBottom(0);
            apply.setCornerRadius(dp(26));
            apply.setBackgroundTintList(ColorStateList.valueOf(dstBg));
            apply.setStrokeColor(ColorStateList.valueOf(accent));
            apply.setStrokeWidth(dpF(1.5f));

            MaterialButton disable = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            disable.setText(R.string.navbar_icons_disable);
            disable.setAllCaps(false);
            disable.setTextColor(ObsidianTheme.textColor());
            disable.setTextSize(14);
            disable.setPadding(dp(4), 0, dp(4), 0);
            LinearLayout.LayoutParams disableLp = new LinearLayout.LayoutParams(0, dp(52), 1f);
            disable.setLayoutParams(disableLp);
            disable.setInsetTop(0);
            disable.setInsetBottom(0);
            disable.setCornerRadius(dp(26));
            disable.setBackgroundTintList(ColorStateList.valueOf(dstBg));
            disable.setStrokeColor(ColorStateList.valueOf(ObsidianTheme.textColor()));
            disable.setStrokeWidth(dpF(1.5f));

            row.addView(apply);
            row.addView(disable);
            root.addView(row);

            VH h = new VH(root);
            h.status = status;
            h.apply = apply;
            h.disable = disable;
            return h;
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            boolean selectedIsApplied = mSelected >= 0 && mSelected == mApplied;

            if (mBusy) {
                h.status.setVisibility(View.VISIBLE);
                h.status.setText(R.string.loading_dialog_wait);
            } else if (mApplied < 0) {
                h.status.setVisibility(View.VISIBLE);
                h.status.setText(R.string.navbar_icons_status_none);
            } else if (selectedIsApplied && mActive) {
                h.status.setVisibility(View.VISIBLE);
                h.status.setText(R.string.navbar_icons_status_active);
            } else if (selectedIsApplied && mPendingReboot) {
                h.status.setVisibility(View.VISIBLE);
                h.status.setText(R.string.navbar_icons_status_pending_reboot);
            } else {
                h.status.setVisibility(View.GONE);
            }

            h.disable.setVisibility(selectedIsApplied ? View.VISIBLE : View.GONE);
            LinearLayout.LayoutParams applyLp = (LinearLayout.LayoutParams) h.apply.getLayoutParams();
            applyLp.setMarginEnd(selectedIsApplied ? dp(8) : 0);
            h.apply.setLayoutParams(applyLp);

            h.apply.setEnabled(!mBusy && mSelected >= 0);
            h.disable.setEnabled(!mBusy && selectedIsApplied);
            h.apply.setAlpha(h.apply.isEnabled() ? 1f : 0.5f);
            h.disable.setAlpha(h.disable.isEnabled() ? 1f : 0.5f);

            h.apply.setOnClickListener(v -> onApplyClicked());
            h.disable.setOnClickListener(v -> onDisableClicked());
        }

        @Override public int getItemCount() { return 1; }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int dpF(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
