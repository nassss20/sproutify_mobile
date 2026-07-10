package com.example.sproutify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    // UI Components
    private TextView tvHello, tvWeatherTemp, tvCareAdvice, tvWeatherLocation;
    private RecyclerView rvPlants, rvReminders;
    private TextView tvNoPlants;
    private LinearLayout layoutNoReminders;
    private SwipeRefreshLayout swipeRefresh;

    // Adapters & Data
    private DashboardPlantAdapter plantAdapter;
    private DashboardReminderAdapter reminderAdapter;
    private List<Plant> plantList = new ArrayList<>();
    private List<Reminder> reminderList = new ArrayList<>();

    // Database & Network
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PlantDatabaseHelper localDb;
    private FusedLocationProviderClient fusedLocationClient;

    // API Configuration
    private static final String API_BASE_URL = "https://sproutify-backend.vercel.app/api/weather-care";
    private static final int LOCATION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. Initialize DBs & Location
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        localDb = new PlantDatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 2. Bind Views
        tvHello = findViewById(R.id.tvHello);
        tvWeatherLocation = findViewById(R.id.tvWeatherLocation);
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp);
        tvCareAdvice = findViewById(R.id.tvCareAdvice);
        rvPlants = findViewById(R.id.rvPlants);
        rvReminders = findViewById(R.id.rvReminders);
        tvNoPlants = findViewById(R.id.tvNoPlants);
        layoutNoReminders = findViewById(R.id.layoutNoReminders);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // 3. Setup User Name
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            tvHello.setText("Hello, " + user.getDisplayName() + "!");
        } else {
            tvHello.setText("Hello, Gardener!");
        }

        // 4. Setup RecyclerViews
        rvPlants.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        plantAdapter = new DashboardPlantAdapter(this, plantList);
        rvPlants.setAdapter(plantAdapter);

        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        reminderAdapter = new DashboardReminderAdapter(this, reminderList);
        rvReminders.setAdapter(reminderAdapter);

        // 5. Load Database Data and Weather API
        loadDashboardData();
        fetchLocationAndWeather();

        // 6. Interactions
        swipeRefresh.setOnRefreshListener(() -> {
            loadDashboardData();
            fetchLocationAndWeather();
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.btnViewAllPlants).setOnClickListener(v -> startActivity(new Intent(this, PlantListActivity.class)));
        findViewById(R.id.btnViewAllReminders).setOnClickListener(v -> startActivity(new Intent(this, ReminderListActivity.class)));
        findViewById(R.id.btnAddPlant).setOnClickListener(v -> startActivity(new Intent(this, AddPlantActivity.class)));
        findViewById(R.id.btnAddReminder).setOnClickListener(v -> startActivity(new Intent(this, AddReminderActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void fetchLocationAndWeather() {
        // Check for location permissions first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }

        // Get actual GPS Location
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                updateLocationName(lat, lng);
                callVercelWeatherApi(lat, lng);
            } else {
                // Fallback to Shah Alam if GPS is off on emulator
                tvWeatherLocation.setText("📍 Location: Shah Alam, Selangor (Fallback)");
                callVercelWeatherApi(3.0738, 101.5183);
            }
        });
    }

    private void updateLocationName(double lat, double lng) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(DashboardActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String city = addresses.get(0).getLocality();
                    String state = addresses.get(0).getAdminArea();
                    String locationName = (city != null) ? city : addresses.get(0).getSubAdminArea();

                    runOnUiThread(() -> {
                        if (locationName != null) {
                            tvWeatherLocation.setText("📍 Location: " + locationName + ", " + state);
                        } else {
                            tvWeatherLocation.setText("📍 Location: Coordinates Found");
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvWeatherLocation.setText("📍 Location: " + lat + ", " + lng));
            }
        }).start();
    }

    private void callVercelWeatherApi(double lat, double lng) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = API_BASE_URL + "?lat=" + lat + "&lng=" + lng;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject weatherObj = response.getJSONObject("weather");
                        JSONObject alertObj = response.getJSONObject("sproutifyAlert");

                        double temp = weatherObj.getDouble("temperature");
                        String advice = alertObj.getString("message");

                        tvWeatherTemp.setText("🌡️ Current Temp: " + temp + "°C");
                        tvCareAdvice.setText(advice);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        tvCareAdvice.setText("Error reading weather data.");
                    }
                },
                error -> {
                    Log.e("WeatherAPI", "Error: " + error.getMessage());
                    tvCareAdvice.setText("Could not connect to Sproutify Weather Server.");
                }
        );

        queue.add(jsonObjectRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndWeather(); // Try again now that we have permission
            } else {
                // Permission denied, fallback to Shah Alam
                callVercelWeatherApi(3.0738, 101.5183);
            }
        }
    }

    private void loadDashboardData() {
        swipeRefresh.setRefreshing(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            plantList.clear();
            plantList.addAll(localDb.getAllPlantsForUser(userId));

            // RESTORE LOGIC: If local DB is empty, try to restore from Cloud!
            if (plantList.isEmpty()) {
                restorePlantsFromCloud(userId);
            } else {
                tvNoPlants.setVisibility(View.GONE);
                rvPlants.setVisibility(View.VISIBLE);
                plantAdapter.notifyDataSetChanged();
            }

            // Load Reminders from Firestore
            db.collection("users").document(userId).collection("reminders")
                    .limit(5)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (isFinishing() || isDestroyed()) return;
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
        } else {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void restorePlantsFromCloud(String userId) {
        db.collection("users").document(userId).collection("plants").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            String datePlanted = doc.getString("datePlanted");
                            String lastWatered = doc.getString("lastWatered");
                            String lastFert = doc.getString("lastFertilized");
                            String notes = doc.getString("notes");

                            Double heightD = doc.getDouble("height");
                            float height = heightD != null ? heightD.floatValue() : 0f;

                            // Save to local SQLite (Image path is left empty since it didn't sync to cloud)
                            localDb.addPlant(userId, name, datePlanted, "", height, lastWatered, lastFert, notes);
                        }

                        // Reload the list now that they are saved locally
                        plantList.clear();
                        plantList.addAll(localDb.getAllPlantsForUser(userId));
                        plantAdapter.notifyDataSetChanged();

                        tvNoPlants.setVisibility(View.GONE);
                        rvPlants.setVisibility(View.VISIBLE);
                    } else {
                        // Truly no plants exist in cloud either
                        tvNoPlants.setVisibility(View.VISIBLE);
                        rvPlants.setVisibility(View.GONE);
                    }
                });
    }
}