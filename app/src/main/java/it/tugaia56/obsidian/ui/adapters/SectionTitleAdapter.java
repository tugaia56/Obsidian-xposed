package it.tugaia56.obsidian.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import it.tugaia56.obsidian.R;

public class SectionTitleAdapter extends RecyclerView.Adapter<SectionTitleAdapter.VH> {
    private final List<String> titles;
    public SectionTitleAdapter(List<String> titles) { this.titles = titles; }
    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_title, parent, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.title.setText(titles.get(pos)); }
    @Override public int getItemCount() { return titles.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        VH(View v) { super(v); title = v.findViewById(R.id.sectionTitle); }
    }
}
