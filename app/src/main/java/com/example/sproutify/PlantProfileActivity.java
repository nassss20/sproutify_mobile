package com.example.sproutify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.File;
import java.util.List;

public class PlantProfileActivity extends AppCompatActivity {

    private ImageView imgPlant;
    private TextView tvName, tvDate, tvHeight, tvWater, tvFert, tvNotes;
    private RecyclerView rvDiary;
    private PlantDatabaseHelper dbHelper;
    private int plantId;
    private boolean isDataUpdated = false;
    private FirebaseAuth mAuth;

    // Add FirestoreManager instance
    private FirestoreManager firestoreManager;

    private final ActivityResultLauncher<Intent> editPlantLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    isDataUpdated = true;
                    if (plantId != -1) {
                        loadPlantData(plantId);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_profile);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbHelper = new PlantDatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        firestoreManager = new FirestoreManager(); // Initialize Manager

        // Bind Views
        imgPlant = findViewById(R.id.imgProfilePlant);
        tvName = findViewById(R.id.tvProfileName);
        tvDate = findViewById(R.id.tvProfileDate);
        tvHeight = findViewById(R.id.tvProfileHeight);
        tvWater = findViewById(R.id.tvProfileWater);
        tvFert = findViewById(R.id.tvProfileFert);
        tvNotes = findViewById(R.id.tvProfileNotes);
        rvDiary = findViewById(R.id.rvDiaryList);

        ImageButton btnBack = findViewById(R.id.btnProfileBack);
        ImageButton btnEdit = findViewById(R.id.btnProfileEdit);
        ImageButton btnDelete = findViewById(R.id.btnProfileDelete);
        Button btnAddDiary = findViewById(R.id.btnAddDiary);
        ExtendedFloatingActionButton fabGrowth = findViewById(R.id.fabProfileGrowth);

        // Standard LayoutManager allows scrolling inside the fixed container
        rvDiary.setLayoutManager(new LinearLayoutManager(this));

        plantId = getIntent().getIntExtra("PLANT_ID", -1);
        if (plantId == -1) {
            finish();
            return;
        }

        // Listeners
        btnBack.setOnClickListener(v -> onBackPressed());
        btnDelete.setOnClickListener(v -> confirmDelete());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditPlantActivity.class);
            intent.putExtra("PLANT_ID", plantId);
            editPlantLauncher.launch(intent);
        });

        fabGrowth.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrackGrowthActivity.class);
            intent.putExtra("PLANT_ID", plantId);
            startActivity(intent);
        });

        btnAddDiary.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddDiaryActivity.class);
            intent.putExtra("PLANT_ID", plantId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (plantId != -1) {
            loadPlantData(plantId);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDataUpdated) {
            setResult(RESULT_OK);
        }
        super.onBackPressed();
    }

    private void loadPlantData(int id) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish(); // No user logged in, so can't show a plant.
            return;
        }
        String userId = currentUser.getUid();
        Plant plant = dbHelper.getPlant(id, userId);
        if (plant != null) {
            tvName.setText(plant.getName());
            tvDate.setText("Planted on: " + plant.datePlanted);
            tvHeight.setText(String.format("%.2f cm", plant.getHeight()));
            tvWater.setText(plant.lastWater.isEmpty() ? "N/A" : plant.lastWater);
            tvFert.setText(plant.lastFertilize.isEmpty() ? "N/A" : plant.lastFertilize);
            tvNotes.setText(plant.notes.isEmpty() ? "No notes added." : plant.notes);

            if (plant.getImagePath() != null && !plant.getImagePath().isEmpty()) {
                Glide.with(this)
                        .load(new File(plant.getImagePath()))
                        .into(imgPlant);
            }

            List<DiaryEntry> entries = dbHelper.getPlantDiary(id);
            DiaryAdapter adapter = new DiaryAdapter(this, entries);
            rvDiary.setAdapter(adapter);
        } else {
            // Plant not found for this user, or plantId is invalid
            Toast.makeText(this, "Plant not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Plant")
                .setMessage("Are you sure you want to remove this plant?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        String userId = currentUser.getUid();
                        dbHelper.deletePlant(plantId, userId);

                        // Sync Deletion to Firestore
                        firestoreManager.deletePlantFromCloud(plantId);

                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "You must be logged in to delete a plant.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}