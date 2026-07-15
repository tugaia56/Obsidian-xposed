package it.tugaia56.obsidian.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import it.tugaia56.obsidian.R;
import it.tugaia56.obsidian.ui.widgets.FooterWidget;
import java.util.List;

public class FooterWidgetAdapter extends RecyclerView.Adapter<FooterWidgetAdapter.VH> {
    private final List<FooterWidget> widgets;
    public FooterWidgetAdapter(List<FooterWidget> widgets) { this.widgets = widgets; }
    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_footer, parent, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        FooterWidget w = widgets.get(pos);
        h.text.setText(w.text());
        if (w.onClick() != null) h.root.setOnClickListener(v -> w.onClick().run());
    }
    @Override public int getItemCount() { return widgets.size(); }
    static class VH extends RecyclerView.ViewHolder {
        View root; TextView text;
        VH(View v) { super(v); root = v.findViewById(R.id.footerRoot); text = v.findViewById(R.id.footerText); }
    }
}
