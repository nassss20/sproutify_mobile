package com.example.sproutify;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

public class DashboardPlantAdapter extends RecyclerView.Adapter<DashboardPlantAdapter.ViewHolder> {
    private Context context;
    private List<Plant> list;

    public DashboardPlantAdapter(Context context, List<Plant> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_plant_dashboard, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Plant p = list.get(position);
        holder.name.setText(p.getName());

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            Glide.with(context).load(new File(p.getImagePath())).into(holder.img);
        } else {
            holder.img.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // --- LINK TO NEW PROFILE PAGE ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlantProfileActivity.class);
            intent.putExtra("PLANT_ID", p.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name;
        public ViewHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgPlant);
            name = itemView.findViewById(R.id.tvPlantName);
        }
    }
}