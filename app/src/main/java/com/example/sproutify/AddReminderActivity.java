package com.example.sproutify;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddReminderActivity extends AppCompatActivity {

    private EditText etTitle, etDate, etTime, etDesc;
    private Button btnSave, btnCancel;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etTitle = findViewById(R.id.etRemTitle);
        etDate = findViewById(R.id.etRemDate);
        etTime = findViewById(R.id.etRemTime);
        etDesc = findViewById(R.id.etRemDesc);
        btnSave = findViewById(R.id.btnSaveReminder);
        btnCancel = findViewById(R.id.btnCancelRem);
        btnBack = findViewById(R.id.btnBackRem);

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        // Date Picker
        etDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                etDate.setText(year + "-" + (month + 1) + "-" + day);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time Picker
        etTime.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                int hour12 = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                if (hour12 == 0) hour12 = 12;
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        btnSave.setOnClickListener(v -> saveReminder());
    }

    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, "Title, Date, and Time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Reminder reminder = new Reminder(title, date, time, desc, false);

        // Generate a new ID locally
        DocumentReference newReminderRef = db.collection("users").document(user.getUid()).collection("reminders").document();
        String reminderId = newReminderRef.getId();

        // Set the data and wait for success before navigating away
        newReminderRef.set(reminder)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getApplicationContext(), "Reminder Synced to Cloud!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddReminderActivity.this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(getApplicationContext(), "Sync Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}