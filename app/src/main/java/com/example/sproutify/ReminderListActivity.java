package com.example.sproutify;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ReminderListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView tvEmpty;
    ReminderAdapter adapter;
    List<Reminder> reminderList;
    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        reminderList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerReminders);
        tvEmpty = findViewById(R.id.tvEmptyList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderAdapter(reminderList);
        recyclerView.setAdapter(adapter);

        // Back Button
        findViewById(R.id.btnBackList).setOnClickListener(v -> finish());

        // Floating Add Button
        FloatingActionButton fab = findViewById(R.id.fabAddReminder);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddReminderActivity.class)));

        loadReminders();
    }

    private void loadReminders() {
        db.collection("users").document(userId).collection("reminders")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    reminderList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Reminder r = doc.toObject(Reminder.class);
                            r.setId(doc.getId());
                            reminderList.add(r);
                        }
                    }

                    if (reminderList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    // --- INNER ADAPTER CLASS ---
    class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {
        List<Reminder> list;

        public ReminderAdapter(List<Reminder> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Reminder r = list.get(position);
            holder.title.setText(r.getTitle());
            holder.date.setText(r.getDate() + " " + r.getTime());

            // 1. Set Initial State
            holder.isDone.setOnCheckedChangeListener(null); // Clear previous listener
            holder.isDone.setChecked(r.isDone());
            toggleStrikeThrough(holder.title, r.isDone()); // Apply visual style

            // 2. Handle Toggle
            holder.isDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Update Local UI instantly
                r.setDone(isChecked);
                toggleStrikeThrough(holder.title, isChecked);

                // Update Firestore
                db.collection("users").document(userId).collection("reminders")
                        .document(r.getId())
                        .update("done", isChecked);
            });

            // 3. Handle Click -> Edit Page
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ReminderListActivity.this, EditReminderActivity.class);
                intent.putExtra("REMINDER_ID", r.getId());
                intent.putExtra("TITLE", r.getTitle());
                intent.putExtra("DATE", r.getDate());
                intent.putExtra("TIME", r.getTime());
                intent.putExtra("DESC", r.getDescription());
                startActivity(intent);
            });
        }

        // --- HELPER METHOD FOR STRIKETHROUGH ---
        private void toggleStrikeThrough(TextView tv, boolean isDone) {
            if (isDone) {
                tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tv.setAlpha(0.5f);
            } else {
                tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tv.setAlpha(1.0f);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, date;
            CheckBox isDone;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvRemTitle);
                date = itemView.findViewById(R.id.tvRemDate);
                isDone = itemView.findViewById(R.id.cbDone);
            }
        }
    }
}