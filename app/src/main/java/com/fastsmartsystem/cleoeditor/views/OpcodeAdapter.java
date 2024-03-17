package com.fastsmartsystem.cleoeditor.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fastsmartsystem.cleoeditor.R;

import java.util.ArrayList;
import java.util.List;

public class OpcodeAdapter extends RecyclerView.Adapter<OpcodeAdapter.OpcodeViewHolder> {
    private List<OpcodeItem> dataList;
    private List<OpcodeItem> filteredDataList;
    private OnOpcodeItemListener clickListener;

    public OpcodeAdapter(List<OpcodeItem> dataList, OnOpcodeItemListener listener) {
        this.dataList = dataList;
        this.filteredDataList = new ArrayList<>(dataList);
        this.clickListener = listener;
    }

    public void filter(String query) {
        filteredDataList.clear();
        if (query.trim().isEmpty()) {
            filteredDataList.addAll(dataList);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (OpcodeItem item : dataList) {
                if (item.name.toLowerCase().contains(filterPattern)) {
                    filteredDataList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OpcodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_opcode_item, parent, false);
        return new OpcodeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull OpcodeViewHolder holder, int position) {
        OpcodeItem data = filteredDataList.get(position);
        holder.textView.setText(data.name);
    }

    public class OpcodeViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public OpcodeViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            textView.setOnClickListener((v) -> {
                int pos = getAdapterPosition();
                if(pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.OnClick(filteredDataList.get(pos));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return filteredDataList.size();
    }
}
