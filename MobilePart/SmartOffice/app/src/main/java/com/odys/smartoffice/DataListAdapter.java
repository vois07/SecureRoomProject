package com.odys.smartoffice;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class DataListAdapter extends RecyclerView.Adapter<DataListAdapter.ViewHolder> {

    private List<DataModel> data;

    public DataListAdapter(List<DataModel> data) {
        this.data = data;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView valueTextView;
        public TextView c, degree;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            valueTextView = itemView.findViewById(R.id.valueTextView);
            degree = itemView.findViewById(R.id.degreeTextView);
            c = itemView.findViewById(R.id.cTextView);
        }
    }

    @NonNull
    @Override
    public DataListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.data_row, parent, false);
        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull DataListAdapter.ViewHolder viewHolder, int position) {
        DataModel dataModel = data.get(position);
        TextView nameTextView = viewHolder.nameTextView;
        TextView valueTextView = viewHolder.valueTextView;
        TextView c = viewHolder.c;
        TextView deg = viewHolder.degree;
        if(!dataModel.getName().contains("emp")) {
            c.setVisibility(View.INVISIBLE);
            deg.setVisibility(View.INVISIBLE);
        }
        nameTextView.setText(dataModel.getName());
        valueTextView.setText(dataModel.getValue());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
