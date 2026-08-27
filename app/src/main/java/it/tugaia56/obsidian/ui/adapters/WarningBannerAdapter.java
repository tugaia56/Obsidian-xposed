package it.tugaia56.obsidian.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import it.tugaia56.obsidian.R;

/**
 * Single prominent bold/uppercase banner row — for a warning the user must see before
 * interacting with the rest of the screen (e.g. "this section needs a device reboot").
 */
public class WarningBannerAdapter extends RecyclerView.Adapter<WarningBannerAdapter.VH> {

    private final String text;

    public WarningBannerAdapter(String text) { this.text = text; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_warning_banner, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.text.setText(text);
    }

    @Override public int getItemCount() { return 1; }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        VH(View v) {
            super(v);
            text = v.findViewById(R.id.warningBannerText);
        }
    }
}
