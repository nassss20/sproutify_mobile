package com.example.sproutify;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditReminderActivity extends AppCompatActivity {

    private EditText etTitle, etDate, etTime, etDesc;
    private String reminderId;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reminder);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get Data from Intent
        reminderId = getIntent().getStringExtra("REMINDER_ID");
        String title = getIntent().getStringExtra("TITLE");
        String date = getIntent().getStringExtra("DATE");
        String time = getIntent().getStringExtra("TIME");
        String desc = getIntent().getStringExtra("DESC");

        // Bind Views
        etTitle = findViewById(R.id.etEditTitle);
        etDate = findViewById(R.id.etEditDate);
        etTime = findViewById(R.id.etEditTime);
        etDesc = findViewById(R.id.etEditDesc);
        ImageButton btnBack = findViewById(R.id.btnBackEdit);
        Button btnUpdate = findViewById(R.id.btnUpdateReminder);
        Button btnDelete = findViewById(R.id.btnDeleteReminder);

        // Pre-fill data
        etTitle.setText(title);
        etDate.setText(date);
        etTime.setText(time);
        etDesc.setText(desc);

        // Listeners
        btnBack.setOnClickListener(v -> finish());

        etDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etDate.setText(year + "-" + (month + 1) + "-" + day),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTime.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                int hour12 = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                if (hour12 == 0) hour12 = 12;
                etTime.setText(String.format("%02d:%02d %s", hour12, minute, amPm));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        btnUpdate.setOnClickListener(v -> updateReminder());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void updateReminder() {
        String title = etTitle.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("date", date);
        updates.put("time", time);
        updates.put("description", desc);

        // Trigger update
        db.collection("users").document(userId).collection("reminders").document(reminderId)
                .update(updates)
                .addOnFailureListener(e -> Toast.makeText(this, "Sync Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // Give feedback and go back to dashboard
        Toast.makeText(this, "Reminder Updated", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(EditReminderActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> {

                    // Trigger delete
                    db.collection("users").document(userId).collection("reminders").document(reminderId)
                            .delete()
                            .addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Sync Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                    // Give feedback and go back to dashboard
                    Toast.makeText(getApplicationContext(), "Reminder Deleted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EditReminderActivity.this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
