package it.tugaia56.obsidian.ui.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import it.tugaia56.obsidian.R;

/**
 * Placeholder for sections not implemented yet — swap for the real fragment once built.
 */
public class WipFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(48, 96, 48, 96);
        tv.setText(getString(R.string.section_wip_summary));
        tv.setTextColor(0x99FFFFFF);
        tv.setTextSize(15);
        return tv;
    }
}
