package com.example.sproutify;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditPlantActivity extends AppCompatActivity {

    private EditText etName, etDate, etWater, etFert, etHeight, etNotes;
    private ImageView imgPlant;
    private RecyclerView rvDiary;
    private PlantDatabaseHelper dbHelper;
    private int plantId;
    private String currentImagePath = "";
    private String cameraPhotoPath;
    private FirebaseAuth mAuth;

    // --- 1. Gallery Launcher ---
    private final ActivityResultLauncher<String> pickImageGallery = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String imagePath = getPathFromUri(uri);
                    if (imagePath != null) {
                        currentImagePath = imagePath;
                        Glide.with(this).load(imagePath).into(imgPlant);
                    }
                }
            }
    );

    // --- 2. Camera Launcher ---
    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    currentImagePath = cameraPhotoPath;
                    Glide.with(this).load(cameraPhotoPath).into(imgPlant);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_plant);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbHelper = new PlantDatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        plantId = getIntent().getIntExtra("PLANT_ID", -1);

        imgPlant = findViewById(R.id.imgEditPlant);
        etName = findViewById(R.id.etEditName);
        etDate = findViewById(R.id.etEditDate);
        etWater = findViewById(R.id.etEditWater);
        etFert = findViewById(R.id.etEditFert);
        etHeight = findViewById(R.id.etEditHeight);
        etNotes = findViewById(R.id.etEditNotes);
        rvDiary = findViewById(R.id.rvEditDiary);

        Button btnUpdate = findViewById(R.id.btnUpdatePlant);
        ImageButton btnBack = findViewById(R.id.btnBackEdit);
        TextView btnChangePhoto = findViewById(R.id.btnChangePhoto);

        rvDiary.setLayoutManager(new LinearLayoutManager(this));

        if (plantId != -1) {
            loadPlantData();
            loadDiaryEntries();
        } else {
            finish();
        }

        // Listeners
        btnBack.setOnClickListener(v -> finish());

        // Show Choice Dialog on Click
        btnChangePhoto.setOnClickListener(v -> showImageSourceDialog());
        imgPlant.setOnClickListener(v -> showImageSourceDialog());

        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etWater.setOnClickListener(v -> showDatePicker(etWater));
        etFert.setOnClickListener(v -> showDatePicker(etFert));

        btnUpdate.setOnClickListener(v -> updatePlant());
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Change Plant Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        pickImageGallery.launch("image/*");
                    }
                })
                .show();
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraPhotoPath = photoFile.getAbsolutePath(); // Store path
            Uri photoURI = FileProvider.getUriForFile(this, // photoURI is local now
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePicture.launch(photoURI);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private String getPathFromUri(Uri uri) {
        try {
            File file = createImageFile();
            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void loadPlantData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        String userId = currentUser.getUid();
        Plant plant = dbHelper.getPlant(plantId, userId);
        if (plant != null) {
            etName.setText(plant.getName());
            etDate.setText(plant.datePlanted);
            etHeight.setText(String.valueOf(plant.height));
            etWater.setText(plant.lastWater);
            etFert.setText(plant.lastFertilize);
            etNotes.setText(plant.notes);

            if (plant.getImagePath() != null && !plant.getImagePath().isEmpty()) {
                currentImagePath = plant.getImagePath();
                Glide.with(this).load(new File(currentImagePath)).into(imgPlant);
            }
        } else {
            Toast.makeText(this, "Plant not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadDiaryEntries() {
        List<DiaryEntry> entries = dbHelper.getPlantDiary(plantId);
        EditDiaryAdapter adapter = new EditDiaryAdapter(entries);
        rvDiary.setAdapter(adapter);
    }

    private void showDatePicker(EditText field) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = year + "-" + (month + 1) + "-" + day;
            field.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updatePlant() {
        String name = etName.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String water = etWater.getText().toString().trim();
        String fert = etFert.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (name.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Name and Date are required", Toast.LENGTH_SHORT).show();
            return;
        }

        float height = 0.0f;
        if (!heightStr.isEmpty()){
            try {
                height = Float.parseFloat(heightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid height", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to update a plant.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Update DB
        dbHelper.updatePlant(plantId, userId, name, date, height, water, fert, notes, currentImagePath);

        Toast.makeText(this, "Plant Updated!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK); // Signal back to Profile that it should refresh
        finish();
    }

    // --- Adapter for Managing Diary Entries ---
    class EditDiaryAdapter extends RecyclerView.Adapter<EditDiaryAdapter.Holder> {
        List<DiaryEntry> list;
        EditDiaryAdapter(List<DiaryEntry> list) { this.list = list; }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            DiaryEntry entry = list.get(position);
            holder.text1.setText(entry.date);
            holder.text2.setText(entry.title + " (Tap to delete)");

            holder.itemView.setOnClickListener(v -> {
                new AlertDialog.Builder(EditPlantActivity.this)
                        .setTitle("Delete Entry?")
                        .setMessage("Remove this diary entry?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dbHelper.deleteDiaryEntry(entry.id);
                            list.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, list.size());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            Holder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
