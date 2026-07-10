package com.example.sproutify;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class TrackGrowthActivity extends AppCompatActivity {

    private PlantDatabaseHelper db;
    private int plantId;
    private RecyclerView rvHistory;
    private EditText etHeight, etDate, etWater, etFert;
    private FirebaseAuth mAuth;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_growth);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new PlantDatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        firestoreManager = new FirestoreManager(); // Initialize Cloud Manager
        plantId = getIntent().getIntExtra("PLANT_ID", -1);

        // Bind Views
        etHeight = findViewById(R.id.etGrowthHeight);
        etDate = findViewById(R.id.etGrowthDate);
        etWater = findViewById(R.id.etGrowthWater);
        etFert = findViewById(R.id.etGrowthFert);
        rvHistory = findViewById(R.id.rvGrowthHistory);
        ImageButton btnBack = findViewById(R.id.btnBackGrowth);
        Button btnSave = findViewById(R.id.btnSaveGrowth);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // Verify the user owns this plant
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || db.getPlant(plantId, currentUser.getUid()) == null) {
            Toast.makeText(this, "Plant not found or access denied.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup Date Pickers
        setupDatePicker(etDate);
        setupDatePicker(etWater);
        setupDatePicker(etFert);

        // Default "Date Recorded" to today
        Calendar c = Calendar.getInstance();
        etDate.setText(c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH)+1) + "-" + c.get(Calendar.DAY_OF_MONTH));

        loadHistory();

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String heightStr = etHeight.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String water = etWater.getText().toString().trim();
            String fert = etFert.getText().toString().trim();

            if (date.isEmpty()) {
                Toast.makeText(this, "Date is required", Toast.LENGTH_SHORT).show();
                return;
            }

            float height = 0f;
            if (!heightStr.isEmpty()) {
                try {
                    height = Float.parseFloat(heightStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid number format for height.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Save to Local DB
            db.addGrowth(plantId, height, date, water, fert);

            // Sync to Cloud
            firestoreManager.saveGrowthToCloud(plantId, height, date, water, fert);

            Toast.makeText(this, "Progress Updated & Synced!", Toast.LENGTH_SHORT).show();

            // Clear inputs and refresh list
            etHeight.setText("");
            etWater.setText("");
            etFert.setText("");
            loadHistory();
        });
    }

    private void setupDatePicker(EditText field) {
        field.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                String date = y + "-" + (m + 1) + "-" + d;
                field.setText(date);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadHistory() {
        List<Map<String, String>> history = db.getGrowthHistory(plantId);
        rvHistory.setAdapter(new HistoryAdapter(history));
    }

    // Inner Adapter for History List
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
        List<Map<String, String>> list;
        HistoryAdapter(List<Map<String, String>> l) { list = l; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            TextView tv = new TextView(p.getContext());
            tv.setPadding(0, 16, 0, 16);
            tv.setTextSize(16f);
            tv.setTextColor(getResources().getColor(R.color.sprout_dark));
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new Holder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int i) {
            Map<String, String> data = list.get(i);
            h.tv.setText("📅 " + data.get("date") + "  —  📏 " + data.get("height") + " cm");

            // Add long-press listener to the most recent item
            if (i == 0) {
                h.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(TrackGrowthActivity.this)
                            .setTitle("Delete Latest Entry?")
                            .setMessage("Are you sure you want to remove the most recent growth entry?")
                            .setPositiveButton("Delete", (dialog, which) -> {

                                // Delete Locally
                                db.deleteLatestGrowthEntry(plantId);

                                // Sync Deletion to Cloud
                                firestoreManager.deleteLatestGrowthFromCloud(plantId);

                                loadHistory(); // Refresh the list
                                Toast.makeText(TrackGrowthActivity.this, "Latest entry removed.", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                });
            } else {
                h.itemView.setOnLongClickListener(null); // Remove listener from other items
            }
        }

        @Override
        public int getItemCount() { return list.size(); }
        class Holder extends RecyclerView.ViewHolder { TextView tv; Holder(View v) { super(v); tv = (TextView) v; } }
    }
}