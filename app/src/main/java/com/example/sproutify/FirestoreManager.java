package com.example.sproutify;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void savePlantToCloud(int localId, String name, String datePlanted, String lastWatered, String lastFertilized, float height, String notes) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.w(TAG, "User not logged in. Cannot sync to cloud.");
            return;
        }

        Map<String, Object> plantData = new HashMap<>();
        plantData.put("localId", localId);
        plantData.put("name", name);
        plantData.put("datePlanted", datePlanted);
        plantData.put("lastWatered", lastWatered);
        plantData.put("lastFertilized", lastFertilized);
        plantData.put("height", height);
        plantData.put("notes", notes);
        plantData.put("timestamp", System.currentTimeMillis());

        // Structure: users -> [userId] -> plants -> [localId]
        db.collection("users").document(user.getUid())
                .collection("plants").document(String.valueOf(localId))
                .set(plantData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Plant successfully synced to Firestore!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing plant to Firestore", e));
    }

    public void updatePlantInCloud(int localId, String name, String datePlanted, String lastWatered, String lastFertilized, float height, String notes) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in. Cannot update cloud.");
            return;
        }

        Map<String, Object> plantData = new HashMap<>();
        plantData.put("name", name);
        plantData.put("datePlanted", datePlanted);
        plantData.put("lastWatered", lastWatered);
        plantData.put("lastFertilized", lastFertilized);
        plantData.put("height", height);
        plantData.put("notes", notes);
        plantData.put("lastUpdated", System.currentTimeMillis());

        db.collection("users").document(user.getUid())
                .collection("plants").document(String.valueOf(localId))
                .update(plantData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Plant successfully updated in Firestore!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating plant in Firestore", e));
    }

    public void deletePlantFromCloud(int localId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in. Cannot delete from cloud.");
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("plants").document(String.valueOf(localId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Plant successfully deleted from Firestore!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting plant from Firestore", e));
    }

    public void saveDiaryToCloud(int plantId, long diaryId, String title, String content, String date) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in. Cannot sync diary.");
            return;
        }

        Map<String, Object> diaryData = new HashMap<>();
        diaryData.put("diaryId", diaryId);
        diaryData.put("title", title);
        diaryData.put("content", content);
        diaryData.put("date", date);
        diaryData.put("timestamp", System.currentTimeMillis());

        // Structure: users -> [userId] -> plants -> [plantId] -> diaries -> [diaryId]
        db.collection("users").document(user.getUid())
                .collection("plants").document(String.valueOf(plantId))
                .collection("diaries").document(String.valueOf(diaryId))
                .set(diaryData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Diary successfully synced to Firestore!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing diary to Firestore", e));
    }

    public void saveGrowthToCloud(int plantId, float height, String date, String water, String fert) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in. Cannot sync growth.");
            return;
        }

        Map<String, Object> growthData = new HashMap<>();
        growthData.put("height", height);
        growthData.put("date", date);
        growthData.put("water", water);
        growthData.put("fert", fert);
        growthData.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(user.getUid())
                .collection("plants").document(String.valueOf(plantId))
                .collection("growth").document(String.valueOf(System.currentTimeMillis()))
                .set(growthData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Growth successfully synced to Firestore!"))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing growth", e));
    }

    public void deleteLatestGrowthFromCloud(int plantId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in. Cannot delete growth.");
            return;
        }

        // Query the cloud for the most recently added growth document and delete it
        db.collection("users").document(user.getUid())
                .collection("plants").document(String.valueOf(plantId))
                .collection("growth")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Latest growth deleted from cloud"));
                    }
                });
    }
}