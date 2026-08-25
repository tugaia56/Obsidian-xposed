package it.tugaia56.obsidian.ui.fragments;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.activity.MainActivity;
import it.tugaia56.obsidian.ui.events.ColorSelectedEvent;
import it.tugaia56.obsidian.utils.ObsidianPrefs;
import it.tugaia56.obsidian.utils.ObsidianTheme;
import it.tugaia56.obsidian.utils.SystemColorUtils;

/**
 * "Colori Sistema" screen.
 *
 * Applies a custom primary colour to OOS 16:
 *  - writes /data/oplus/uxres/uxcolor/ux_custom_color[_night].xml
 *  - sets settings secure theme_customization_overlay_packages
 *
 * Identical mechanism to OC ColorsFragment, confirmed working on OOS 16.
 */
public class SystemColorsFragment extends Fragment {

    private static final int DIALOG_ID = 0xC010;

    private int  mColor;
    private View mSwatch;
    private TextView mHexLabel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_system_colors, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mColor    = ObsidianPrefs.getInt(SystemColorUtils.PREF_SYSTEM_COLOR, 0xFF6200EE);
        mSwatch   = view.findViewById(R.id.systemColorSwatch);
        mHexLabel = view.findViewById(R.id.systemColorHex);
        Button btnApply = view.findViewById(R.id.btnApplySystemColor);

        com.google.android.material.card.MaterialCardView previewCard =
                view.findViewById(R.id.systemColorPreviewCard);
        com.google.android.material.card.MaterialCardView infoCard =
                view.findViewById(R.id.systemColorInfoCard);
        TextView tapHint   = view.findViewById(R.id.systemColorTapHint);
        TextView infoTitle = view.findViewById(R.id.systemColorInfoTitle);
        TextView infoBody  = view.findViewById(R.id.systemColorInfoBody);

        int cardColor  = ObsidianTheme.cardColor();
        int borderColor = ObsidianTheme.textColor(0x33);
        previewCard.setCardBackgroundColor(cardColor);
        previewCard.setStrokeColor(borderColor);
        infoCard.setCardBackgroundColor(cardColor);
        infoCard.setStrokeColor(borderColor);
        mHexLabel.setTextColor(ObsidianTheme.textColor(0xCC));
        tapHint.setTextColor(ObsidianTheme.textColor(0x55));
        infoTitle.setTextColor(ObsidianTheme.textColor());
        infoBody.setTextColor(ObsidianTheme.textColor(0x99));

        updateSwatch();

        view.findViewById(R.id.systemColorSwatchContainer)
                .setOnClickListener(v -> openColorPicker());

        btnApply.setOnClickListener(v -> applyColor());
    }

    private void openColorPicker() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showColorPickerDialog(
                    DIALOG_ID, mColor, true, true, true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onColorSelected(ColorSelectedEvent event) {
        if (event.dialogId() != DIALOG_ID) return;
        mColor = event.color();
        ObsidianPrefs.putInt(SystemColorUtils.PREF_SYSTEM_COLOR, mColor);
        updateSwatch();
    }

    private void updateSwatch() {
        if (mSwatch == null) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(mColor);
        mSwatch.setBackground(gd);
        mHexLabel.setText(String.format("#%06X", mColor & 0xFFFFFF));
    }

    private void applyColor() {
        Button btn = getView() == null ? null : getView().findViewById(R.id.btnApplySystemColor);
        if (btn != null) btn.setEnabled(false);

        new Thread(() -> {
            boolean ok = SystemColorUtils.apply(mColor);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (btn != null) btn.setEnabled(true);
                Toast.makeText(requireContext(),
                        ok ? R.string.system_color_applied : R.string.system_color_error,
                        Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
