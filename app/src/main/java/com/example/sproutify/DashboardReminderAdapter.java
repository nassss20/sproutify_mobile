package com.example.sproutify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class DashboardReminderAdapter extends RecyclerView.Adapter<DashboardReminderAdapter.ViewHolder> {

    private Context context;
    private List<Reminder> list;
    private FirebaseFirestore db;
    private String userId;

    public DashboardReminderAdapter(Context context, List<Reminder> list) {
        this.context = context;
        this.list = list;
        this.db = FirebaseFirestore.getInstance();
        // Check if user is logged in to avoid crashes
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse the same item layout as the main list
        View v = LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reminder r = list.get(position);

        holder.title.setText(r.getTitle());
        holder.date.setText(r.getDate() + " " + r.getTime());

        // --- 1. TOGGLE LOGIC (Checkbox) ---
        holder.isDone.setOnCheckedChangeListener(null);
        holder.isDone.setChecked(r.isDone());

        // Set visual strikethrough based on state
        toggleStrikeThrough(holder.title, r.isDone());

        holder.isDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId == null) return;

            // Update UI
            r.setDone(isChecked);
            toggleStrikeThrough(holder.title, isChecked);

            // Update Firestore
            db.collection("users").document(userId).collection("reminders")
                    .document(r.getId())
                    .update("done", isChecked);
        });

        // --- 2. CLICK LOGIC (Edit Page) ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditReminderActivity.class);
            intent.putExtra("REMINDER_ID", r.getId());
            intent.putExtra("TITLE", r.getTitle());
            intent.putExtra("DATE", r.getDate());
            intent.putExtra("TIME", r.getTime());
            intent.putExtra("DESC", r.getDescription());
            context.startActivity(intent);
        });
    }

    // Helper to strike text
    private void toggleStrikeThrough(TextView tv, boolean isDone) {
        if (isDone) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.5f); // Dim text if done
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tv.setAlpha(1.0f); // Restore opacity
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;
        CheckBox isDone;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvRemTitle);
            date = itemView.findViewById(R.id.tvRemDate);
            isDone = itemView.findViewById(R.id.cbDone);
        }
    }
}