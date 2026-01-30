package com.example.sproutify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

public class PlantGridAdapter extends BaseAdapter {
    private Context context;
    private List<Plant> plantList;

    public PlantGridAdapter(Context context, List<Plant> plantList) {
        this.context = context;
        this.plantList = plantList;
    }

    @Override
    public int getCount() { return plantList.size(); }

    @Override
    public Object getItem(int position) { return plantList.get(position); }

    @Override
    public long getItemId(int position) { return plantList.get(position).id; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_plant_grid, parent, false);
        }

        ImageView imgPlant = convertView.findViewById(R.id.imgPlant);
        TextView tvName = convertView.findViewById(R.id.tvPlantName);

        Plant plant = plantList.get(position);
        tvName.setText(plant.getName());

        // Load image from local path
        if (plant.getImagePath() != null && !plant.getImagePath().isEmpty()) {
            Glide.with(context).load(new File(plant.getImagePath())).into(imgPlant);
        } else {
            imgPlant.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }
}