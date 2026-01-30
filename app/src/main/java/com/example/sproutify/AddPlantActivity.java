package com.example.sproutify;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import java.util.Locale;

public class AddPlantActivity extends AppCompatActivity {

    private ImageView imgSelectedPlant;
    private EditText etName, etDatePlanted, etLastWatered, etLastFertilized, etHeight, etNotes;
    private String currentImagePath = "";
    private PlantDatabaseHelper dbHelper;
    private String cameraPhotoPath;
    private FirebaseAuth mAuth;

    // Permission Launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
                }
            });

    // Camera Result Launcher
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    currentImagePath = cameraPhotoPath;
                    loadImageIntoView(currentImagePath);
                }
            });

    // Gallery Selection Launcher
    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String imagePath = getPathFromUri(uri);
                    if (imagePath != null) {
                        currentImagePath = imagePath;
                        loadImageIntoView(currentImagePath);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbHelper = new PlantDatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        imgSelectedPlant = findViewById(R.id.imgSelectedPlant);
        etName = findViewById(R.id.etPlantName);
        etDatePlanted = findViewById(R.id.etDatePlanted);
        etLastWatered = findViewById(R.id.etLastWatered);
        etLastFertilized = findViewById(R.id.etLastFertilized);
        etHeight = findViewById(R.id.etHeight);
        etNotes = findViewById(R.id.etNotes);

        Button btnSave = findViewById(R.id.btnSavePlant);
        ImageButton btnBack = findViewById(R.id.btnBackPlant);

        // Setup Listeners
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.cardImagePicker).setOnClickListener(v -> showImagePickerDialog());
        btnSave.setOnClickListener(v -> savePlant());

        // Setup Date Pickers
        etDatePlanted.setOnClickListener(v -> showDatePicker(etDatePlanted));
        etLastWatered.setOnClickListener(v -> showDatePicker(etLastWatered));
        etLastFertilized.setOnClickListener(v -> showDatePicker(etLastFertilized));
    }

    // --- HELPER TO LOAD IMAGE CORRECTLY ---
    private void loadImageIntoView(String path) {
        // 1. Remove green overlay so preview jadi elok
        imgSelectedPlant.setImageTintList(null);

        // 2. Remove padding (so image fills the box)
        imgSelectedPlant.setPadding(0, 0, 0, 0);

        // 3. Set ScaleType to crop center
        imgSelectedPlant.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // 4. Load Image
        Glide.with(this).load(new File(path)).into(imgSelectedPlant);
    }

    private void showDatePicker(EditText targetField) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = year + "-" + (month + 1) + "-" + day;
            targetField.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Add Plant Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        selectImageLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraPhotoPath = photoFile.getAbsolutePath();
            Uri cameraImageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(cameraImageUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file for camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
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

    private void savePlant() {
        String name = etName.getText().toString().trim();
        String datePlanted = etDatePlanted.getText().toString().trim();
        String lastWatered = etLastWatered.getText().toString().trim();
        String lastFertilized = etLastFertilized.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (name.isEmpty() || datePlanted.isEmpty()) {
            Toast.makeText(this, "Name and Date Planted are required", Toast.LENGTH_SHORT).show();
            return;
        }

        float height = 0.0f;
        if (!heightStr.isEmpty()) {
            try {
                height = Float.parseFloat(heightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid height format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to add a plant.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        long result = dbHelper.addPlant(userId, name, datePlanted, currentImagePath, height, lastWatered, lastFertilized, notes);

        if (result != -1) {
            Toast.makeText(this, "Plant Added! 🌱", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Error Saving Plant", Toast.LENGTH_SHORT).show();
        }
    }
}
