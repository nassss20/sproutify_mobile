package com.example.sproutify;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.EntryHolder> {

    private Context context;
    private List<DiaryEntry> list;

    public DiaryAdapter(Context context, List<DiaryEntry> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_diary_entry, parent, false);
        return new EntryHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryHolder holder, int position) {
        DiaryEntry entry = list.get(position);

        holder.tvDate.setText(entry.date);

        // --- MAKE TITLE BOLD ---
        holder.tvTitle.setText(entry.title);
        holder.tvTitle.setTypeface(null, Typeface.BOLD);

        holder.tvContent.setText(entry.content);

        // --- IMAGE LIST SETUP ---
        if (entry.imagePaths != null && !entry.imagePaths.isEmpty()) {
            holder.rvImages.setVisibility(View.VISIBLE);

            // Set horizontal layout
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            holder.rvImages.setLayoutManager(layoutManager);

            // Set inner adapter
            ImageAdapter imgAdapter = new ImageAdapter(entry.imagePaths);
            holder.rvImages.setAdapter(imgAdapter);
        } else {
            holder.rvImages.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    class EntryHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTitle, tvContent;
        RecyclerView rvImages;

        public EntryHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvEntryDate);
            tvTitle = v.findViewById(R.id.tvEntryTitle);
            tvContent = v.findViewById(R.id.tvEntryContent);
            rvImages = v.findViewById(R.id.rvEntryImages);
        }
    }

    // --- Inner Adapter for Images ---
    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImgHolder> {
        List<String> paths;
        ImageAdapter(List<String> p) { paths = p; }

        @NonNull @Override
        public ImgHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            // Using your existing item_diary_image.xml
            View v = LayoutInflater.from(context).inflate(R.layout.item_diary_image, p, false);
            return new ImgHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ImgHolder h, int i) {
            try {
                Glide.with(context)
                        .load(Uri.parse(paths.get(i)))
                        .placeholder(android.R.drawable.ic_menu_gallery) // Show while loading
                        .error(android.R.drawable.stat_notify_error)     // Show if failed
                        .centerCrop()
                        .into(h.img);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() { return paths.size(); }
        class ImgHolder extends RecyclerView.ViewHolder {
            ImageView img;
            ImgHolder(View v) { super(v); img = v.findViewById(R.id.imgDiaryThumb); }
        }
    }
}