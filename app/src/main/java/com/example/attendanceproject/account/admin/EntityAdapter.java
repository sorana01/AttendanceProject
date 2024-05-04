package com.example.attendanceproject.account.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceproject.R;

import java.util.ArrayList;

public class EntityAdapter extends RecyclerView.Adapter<EntityAdapter.EntityViewHolder> {
    protected ArrayList<EntityItem> entityItems;
    private Context context;
    private OnItemClickListener listener;

    public ArrayList<EntityItem> getEntityItems() {
        return entityItems;
    }

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(EntityItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public EntityAdapter(Context context, ArrayList<EntityItem> entityItems) {
        this.entityItems = entityItems;
        this.context = context;
    }

    public static class EntityViewHolder extends RecyclerView.ViewHolder{

        private TextView entityName;
        private TextView entityDetail;


        public EntityViewHolder(@NonNull View itemView) {
            super(itemView);
            entityName = itemView.findViewById(R.id.entity_name_textview);
            entityDetail = itemView.findViewById(R.id.entity_detail_textview);
        }
    }


    @NonNull
    @Override
    public EntityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.entity_item, parent, false);
        return new EntityViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EntityViewHolder holder, int position) {
        holder.entityName.setText(entityItems.get(position).getEntityName());
        holder.entityDetail.setText(entityItems.get(position).getEntityDetail());

        EntityItem item = entityItems.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entityItems.size();
    }

}
