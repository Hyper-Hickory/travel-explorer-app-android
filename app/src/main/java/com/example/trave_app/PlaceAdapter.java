package com.example.trave_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.VH> {

    public interface OnPlaceClickListener {
        void onPlaceClicked(PlaceDirectoryActivity.PlaceItem item);
    }

    private List<PlaceDirectoryActivity.PlaceItem> data;
    private final OnPlaceClickListener listener;

    public PlaceAdapter(List<PlaceDirectoryActivity.PlaceItem> data, OnPlaceClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    public void updateData(List<PlaceDirectoryActivity.PlaceItem> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PlaceDirectoryActivity.PlaceItem item = data.get(position);
        holder.txtName.setText(item.name);
        String meta = capitalize(item.category) + " • " + item.state +
                String.format(" • %.4f, %.4f", item.latitude, item.longitude);
        holder.txtMeta.setText(meta);
        holder.txtAddress.setText(item.address);
        holder.itemView.setOnClickListener(v -> listener.onPlaceClicked(item));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtName, txtMeta, txtAddress;
        VH(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtMeta = itemView.findViewById(R.id.txtMeta);
            txtAddress = itemView.findViewById(R.id.txtAddress);
        }
    }
}
