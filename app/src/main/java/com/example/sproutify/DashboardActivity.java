package com.example.sproutify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    // UI Components
    private TextView tvHello;
    private RecyclerView rvPlants, rvReminders;
    private TextView tvNoPlants;
    private LinearLayout layoutNoReminders;
    private SwipeRefreshLayout swipeRefresh;

    // Adapters & Data
    private DashboardPlantAdapter plantAdapter;
    private DashboardReminderAdapter reminderAdapter;
    private List<Plant> plantList = new ArrayList<>();
    private List<Reminder> reminderList = new ArrayList<>();

    // Database
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PlantDatabaseHelper localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize DBs
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        localDb = new PlantDatabaseHelper(this);

        // 2. Bind Views
        tvHello = findViewById(R.id.tvHello);
        rvPlants = findViewById(R.id.rvPlants);
        rvReminders = findViewById(R.id.rvReminders);
        tvNoPlants = findViewById(R.id.tvNoPlants);
        layoutNoReminders = findViewById(R.id.layoutNoReminders);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // 3. Setup User Name
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            tvHello.setText("Hello, " + user.getDisplayName() + "!");
        }

        // 4. Setup RecyclerViews
        // Plant List (Horizontal)
        rvPlants.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        plantAdapter = new DashboardPlantAdapter(this, plantList);
        rvPlants.setAdapter(plantAdapter);

        // Reminder List (Vertical)
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        reminderAdapter = new DashboardReminderAdapter(this, reminderList);
        rvReminders.setAdapter(reminderAdapter);

        // 5. Load Data
        loadDashboardData();

        // 6. Interactions
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // Navigation to "View All" pages
        findViewById(R.id.btnViewAllPlants).setOnClickListener(v ->
                startActivity(new Intent(this, PlantListActivity.class))); // Lab 2 Grid Page

        findViewById(R.id.btnViewAllReminders).setOnClickListener(v ->
                startActivity(new Intent(this, ReminderListActivity.class)));

        // Navigation to "Add" pages
        findViewById(R.id.btnAddPlant).setOnClickListener(v ->
                startActivity(new Intent(this, AddPlantActivity.class)));

        findViewById(R.id.btnAddReminder).setOnClickListener(v ->
                startActivity(new Intent(this, AddReminderActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData(); // Refresh when returning from other screens
    }

    private void loadDashboardData() {
        swipeRefresh.setRefreshing(true);

        // A. Load Plants from SQLite for the current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            plantList.clear();
            plantList.addAll(localDb.getAllPlantsForUser(userId));
        }

        if (plantList.isEmpty()) {
            tvNoPlants.setVisibility(View.VISIBLE);
            rvPlants.setVisibility(View.GONE);
        } else {
            tvNoPlants.setVisibility(View.GONE);
            rvPlants.setVisibility(View.VISIBLE);
        }
        plantAdapter.notifyDataSetChanged();

        // B. Load Reminders from Firestore
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("reminders")
                .limit(5) // Only show top 5 on dashboard
                .get()
                .addOnCompleteListener(task -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (task.isSuccessful()) {
                        reminderList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Reminder r = doc.toObject(Reminder.class);
                            r.setId(doc.getId());
                            reminderList.add(r);
                        }

                        if (reminderList.isEmpty()) {
                            layoutNoReminders.setVisibility(View.VISIBLE);
                            rvReminders.setVisibility(View.GONE);
                        } else {
                            layoutNoReminders.setVisibility(View.GONE);
                            rvReminders.setVisibility(View.VISIBLE);
                        }

                        reminderAdapter.notifyDataSetChanged();
                    }
                    swipeRefresh.setRefreshing(false);
                });
    }
}