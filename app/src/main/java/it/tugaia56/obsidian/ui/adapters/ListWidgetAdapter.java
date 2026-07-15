package it.tugaia56.obsidian.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.tugaia56.obsidian.R;

public class ListWidgetAdapter extends RecyclerView.Adapter<ListWidgetAdapter.VH> {

    public static class ListItem {
        public final String title;
        public String valueSummary; // shown as tinted caption below the title
        public final Runnable onClick;

        public ListItem(String title, String valueSummary, Runnable onClick) {
            this.title        = title;
            this.valueSummary = valueSummary;
            this.onClick      = onClick;
        }
    }

    private final List<ListItem> items;

    public ListWidgetAdapter(List<ListItem> items) { this.items = items; }

    public List<ListItem> getItems() { return items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_widget_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ListItem item = items.get(pos);
        h.title.setText(item.title);
        h.value.setText(item.valueSummary != null ? item.valueSummary : "");
        h.itemView.setOnClickListener(v -> { if (item.onClick != null) item.onClick.run(); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, value;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.listTitle);
            value = v.findViewById(R.id.listValue);
        }
    }
}
