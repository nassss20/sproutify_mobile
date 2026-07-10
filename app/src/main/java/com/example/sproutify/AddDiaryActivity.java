package com.example.sproutify;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddDiaryActivity extends AppCompatActivity {

    private int plantId;
    private PlantDatabaseHelper db;
    private EditText etTitle, etDate, etContent;
    private RecyclerView rvPhotos;
    private List<Uri> selectedImages = new ArrayList<>();
    private PhotoAdapter adapter;
    private Uri photoURI; // Camera URI
    private FirebaseAuth mAuth;

    // Add FirestoreManager instance
    private FirestoreManager firestoreManager;

    // 1. Gallery
    private final ActivityResultLauncher<String> pickImagesGallery = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null) {
                    for (Uri uri : uris) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {}
                    }
                    selectedImages.addAll(uris);
                    adapter.notifyDataSetChanged();
                    rvPhotos.setVisibility(View.VISIBLE);
                }
            }
    );

    // 2. Camera
    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    selectedImages.add(photoURI);
                    adapter.notifyDataSetChanged();
                    rvPhotos.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_diary);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = new PlantDatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        firestoreManager = new FirestoreManager(); // Initialize the cloud manager
        plantId = getIntent().getIntExtra("PLANT_ID", -1);

        // Verify the user owns this plant
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || db.getPlant(plantId, currentUser.getUid()) == null) {
            Toast.makeText(this, "Plant not found or access denied.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTitle = findViewById(R.id.etDiaryTitle);
        etDate = findViewById(R.id.etDiaryDate);
        etContent = findViewById(R.id.etDiaryContent);
        rvPhotos = findViewById(R.id.rvDiaryPhotos);

        Calendar c = Calendar.getInstance();
        etDate.setText(c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH)+1) + "-" + c.get(Calendar.DAY_OF_MONTH));
        etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, y, m, d) -> {
                etDate.setText(y + "-" + (m + 1) + "-" + d);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new PhotoAdapter();
        rvPhotos.setAdapter(adapter);

        // CHOICE DIALOG
        findViewById(R.id.btnAddPhotos).setOnClickListener(v -> showImageSourceDialog());

        findViewById(R.id.btnBackDiary).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveDiary).setOnClickListener(v -> saveEntry());
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) launchCamera();
                    else pickImagesGallery.launch("image/*");
                })
                .show();
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            photoURI = FileProvider.getUriForFile(this,
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

    private void saveEntry() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please add a title", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Save locally to SQLite
        long diaryId = db.addDiary(plantId, title, content, date);

        for (Uri uri : selectedImages) {
            db.addDiaryImage(diaryId, uri.toString());
        }

        // 2. Sync text content to Firestore
        firestoreManager.saveDiaryToCloud(plantId, diaryId, title, content, date);

        Toast.makeText(this, "Memory Saved & Synced! 📖", Toast.LENGTH_SHORT).show();
        finish();
    }

    class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(200, 200));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(8, 0, 8, 0);
            return new Holder(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Glide.with(AddDiaryActivity.this)
                    .load(selectedImages.get(position))
                    .into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() { return selectedImages.size(); }
        class Holder extends RecyclerView.ViewHolder { Holder(View v) { super(v); } }
    }
}