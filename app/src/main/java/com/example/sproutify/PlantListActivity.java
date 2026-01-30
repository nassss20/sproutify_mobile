package com.example.sproutify;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class PlantListActivity extends AppCompatActivity {

    private GridView gridView;
    private TextView tvEmpty;
    private PlantDatabaseHelper dbHelper;
    private List<Plant> plantList;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_list);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbHelper = new PlantDatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        gridView = findViewById(R.id.gridViewPlants);
        tvEmpty = findViewById(R.id.tvEmptyGrid);
        ImageButton btnBack = findViewById(R.id.btnBackList);
        FloatingActionButton fabAddPlant = findViewById(R.id.fabAddPlant);

        btnBack.setOnClickListener(v -> finish());
        fabAddPlant.setOnClickListener(v -> {
            startActivity(new Intent(this, AddPlantActivity.class));
        });

        loadPlants();

        // Handle Click on Grid Item
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Plant selectedPlant = plantList.get(position);

                // --- LINK TO NEW PROFILE PAGE ---
                Intent intent = new Intent(PlantListActivity.this, PlantProfileActivity.class);
                intent.putExtra("PLANT_ID", selectedPlant.getId());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlants();
    }

    private void loadPlants() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            plantList = dbHelper.getAllPlantsForUser(userId);
        }

        if (plantList.isEmpty()) {
            gridView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            gridView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            PlantGridAdapter adapter = new PlantGridAdapter(this, plantList);
            gridView.setAdapter(adapter);
        }
    }
}