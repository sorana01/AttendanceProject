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

public class EntityAdapter<T extends EntityItem> extends RecyclerView.Adapter<EntityAdapter.EntityViewHolder> {
    protected ArrayList<T> entityItems;
    private Context context;
    private OnItemClickListener listener;


    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(EntityItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param entityItems ArrayList<T> containing the data to populate views to be used
     * by RecyclerView
     */
    public EntityAdapter(Context context, ArrayList<T> entityItems) {
        this.entityItems = entityItems;
        this.context = context;
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class EntityViewHolder extends RecyclerView.ViewHolder{

        private TextView entityName;
        private TextView entityDetail;


        public EntityViewHolder(@NonNull View itemView) {
            super(itemView);

            // Define click listener for the ViewHolder's View
            entityName = itemView.findViewById(R.id.entity_name_textview);
            entityDetail = itemView.findViewById(R.id.entity_detail_textview);
        }
    }


    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public EntityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.entity_item, parent, false);
        return new EntityViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull EntityViewHolder holder, int position) {
        T item = entityItems.get(position);
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        holder.entityName.setText(entityItems.get(position).getEntityName());
        holder.entityDetail.setText(entityItems.get(position).getEntityDetail());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return entityItems.size();
    }

}
