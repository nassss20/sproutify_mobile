package com.example.sproutify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sproutify_local.db";
    private static final int DATABASE_VERSION = 5; // Incremented version

    // Tables
    public static final String TABLE_PLANTS = "plants";
    public static final String TABLE_DIARY = "diary";
    public static final String TABLE_GROWTH = "growth";
    public static final String TABLE_DIARY_IMAGES = "diary_images";

    // Columns
    public static final String COL_ID = "id";
    public static final String COL_USER_ID = "user_id"; // New column for Firebase User ID
    public static final String COL_NAME = "name";
    public static final String COL_DATE = "date_planted";
    public static final String COL_IMAGE = "image_path";
    public static final String COL_HEIGHT = "height";
    public static final String COL_WATER = "last_water";
    public static final String COL_FERTILIZE = "last_fertilize";
    public static final String COL_NOTES = "notes";

    // Foreign Keys & Other Columns
    public static final String COL_PLANT_ID_FK = "plant_id";
    public static final String COL_DIARY_TITLE = "title";
    public static final String COL_DIARY_CONTENT = "content";
    public static final String COL_DIARY_DATE = "date";
    public static final String COL_DIARY_ID_FK = "diary_id";
    public static final String COL_DIARY_IMG_PATH = "image_path";
    public static final String COL_GROWTH_HEIGHT = "height";
    public static final String COL_GROWTH_DATE = "date";

    public PlantDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PLANTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_ID + " TEXT, " + // Added user ID
                COL_NAME + " TEXT, " + COL_DATE + " TEXT, " + COL_IMAGE + " TEXT, " +
                COL_HEIGHT + " REAL, " + COL_WATER + " TEXT, " + COL_FERTILIZE + " TEXT, " + COL_NOTES + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_DIARY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PLANT_ID_FK + " INTEGER, " +
                COL_DIARY_TITLE + " TEXT, " + COL_DIARY_CONTENT + " TEXT, " + COL_DIARY_DATE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_GROWTH + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PLANT_ID_FK + " INTEGER, " +
                COL_GROWTH_HEIGHT + " REAL, " + COL_GROWTH_DATE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_DIARY_IMAGES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DIARY_ID_FK + " INTEGER, " +
                COL_DIARY_IMG_PATH + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_PLANTS + " ADD COLUMN " + COL_HEIGHT + "_new REAL");
            db.execSQL("UPDATE " + TABLE_PLANTS + " SET " + COL_HEIGHT + "_new = CAST(" + COL_HEIGHT + " AS REAL)");
            db.execSQL("ALTER TABLE " + TABLE_PLANTS + " RENAME TO " + TABLE_PLANTS + "_old");
            db.execSQL("CREATE TABLE " + TABLE_PLANTS + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_NAME + " TEXT, " + COL_DATE + " TEXT, " + COL_IMAGE + " TEXT, " +
                    COL_HEIGHT + " REAL, " + COL_WATER + " TEXT, " + COL_FERTILIZE + " TEXT, " + COL_NOTES + " TEXT)");
            db.execSQL("INSERT INTO " + TABLE_PLANTS + " (" + COL_ID + ", " + COL_NAME + ", " + COL_DATE + ", " + COL_IMAGE + ", " + COL_HEIGHT + ", " + COL_WATER + ", " + COL_FERTILIZE + ", " + COL_NOTES + ") " +
                    "SELECT " + COL_ID + ", " + COL_NAME + ", " + COL_DATE + ", " + COL_IMAGE + ", " + COL_HEIGHT + "_new, " + COL_WATER + ", " + COL_FERTILIZE + ", " + COL_NOTES + " FROM " + TABLE_PLANTS + "_old");
            db.execSQL("DROP TABLE " + TABLE_PLANTS + "_old");

            db.execSQL("ALTER TABLE " + TABLE_GROWTH + " ADD COLUMN " + COL_GROWTH_HEIGHT + "_new REAL");
            db.execSQL("UPDATE " + TABLE_GROWTH + " SET " + COL_GROWTH_HEIGHT + "_new = CAST(" + COL_GROWTH_HEIGHT + " AS REAL)");
            db.execSQL("ALTER TABLE " + TABLE_GROWTH + " RENAME TO " + TABLE_GROWTH + "_old");
            db.execSQL("CREATE TABLE " + TABLE_GROWTH + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_PLANT_ID_FK + " INTEGER, " +
                    COL_GROWTH_HEIGHT + " REAL, " + COL_GROWTH_DATE + " TEXT)");
            db.execSQL("INSERT INTO " + TABLE_GROWTH + " (" + COL_ID + ", " + COL_PLANT_ID_FK + ", " + COL_GROWTH_HEIGHT + ", " + COL_GROWTH_DATE + ") " +
                    "SELECT " + COL_ID + ", " + COL_PLANT_ID_FK + ", " + COL_GROWTH_HEIGHT + "_new, " + COL_GROWTH_DATE + " FROM " + TABLE_GROWTH + "_old");
            db.execSQL("DROP TABLE " + TABLE_GROWTH + "_old");
        } 
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_PLANTS + " ADD COLUMN " + COL_USER_ID + " TEXT");
        }
    }

    // --- PLANTS CRUD ---
    public long addPlant(String userId, String name, String date, String imagePath, float height, String water, String fert, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_ID, userId);
        values.put(COL_NAME, name);
        values.put(COL_DATE, date);
        values.put(COL_IMAGE, imagePath);
        values.put(COL_HEIGHT, height);
        values.put(COL_WATER, water);
        values.put(COL_FERTILIZE, fert);
        values.put(COL_NOTES, notes);
        return db.insert(TABLE_PLANTS, null, values);
    }

    public List<Plant> getAllPlantsForUser(String userId) {
        List<Plant> plants = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PLANTS + " WHERE " + COL_USER_ID + " = ?", new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                Plant plant = new Plant(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_HEIGHT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_WATER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_FERTILIZE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES))
                );
                plants.add(plant);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return plants;
    }

    public void updatePlant(int id, String userId, String name, String date, float height, String water, String fert, String notes, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_DATE, date);
        values.put(COL_HEIGHT, height);
        values.put(COL_WATER, water);
        values.put(COL_FERTILIZE, fert);
        values.put(COL_NOTES, notes);

        // Only update image if a new one was selected (not empty)
        if (imagePath != null && !imagePath.isEmpty()) {
            values.put(COL_IMAGE, imagePath);
        }

        db.update(TABLE_PLANTS, values, COL_ID + " = ? AND " + COL_USER_ID + " = ?", new String[]{String.valueOf(id), userId});
        db.close();
    }

    public Plant getPlant(int id, String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLANTS, null, COL_ID + " = ? AND " + COL_USER_ID + " = ?", new String[]{String.valueOf(id), userId}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Plant p = new Plant(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_HEIGHT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_WATER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_FERTILIZE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES))
                );
                cursor.close();
                return p;
            }
            cursor.close();
        }
        return null;
    }

    public void deletePlant(int id, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PLANTS, COL_ID + " = ? AND " + COL_USER_ID + " = ?", new String[]{String.valueOf(id), userId});
        db.close();
    }

    // --- DIARY ---
    public long addDiary(int plantId, String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PLANT_ID_FK, plantId);
        values.put(COL_DIARY_TITLE, title);
        values.put(COL_DIARY_CONTENT, content);
        values.put(COL_DIARY_DATE, date);
        long id = db.insert(TABLE_DIARY, null, values);
        db.close();
        return id;
    }

    public void addDiaryImage(long diaryId, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DIARY_ID_FK, diaryId);
        values.put(COL_DIARY_IMG_PATH, imagePath);
        db.insert(TABLE_DIARY_IMAGES, null, values);
        db.close();
    }

    // --- GROWTH ---
    public void addGrowth(int plantId, float height, String date, String waterDate, String fertDate) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 1. Add Height to History
        if (height > 0) {
            ContentValues historyValues = new ContentValues();
            historyValues.put(COL_PLANT_ID_FK, plantId);
            historyValues.put(COL_GROWTH_HEIGHT, height);
            historyValues.put(COL_GROWTH_DATE, date);
            db.insert(TABLE_GROWTH, null, historyValues);
        }

        // 2. Update Main Plant Status
        ContentValues plantValues = new ContentValues();
        if (height > 0) plantValues.put(COL_HEIGHT, height);
        if (!waterDate.isEmpty()) plantValues.put(COL_WATER, waterDate);
        if (!fertDate.isEmpty()) plantValues.put(COL_FERTILIZE, fertDate);

        if (plantValues.size() > 0) {
            db.update(TABLE_PLANTS, plantValues, COL_ID + "=?", new String[]{String.valueOf(plantId)});
        }
        db.close();
    }

    public List<Map<String, String>> getGrowthHistory(int plantId) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GROWTH, null, COL_PLANT_ID_FK + "=?",
                new String[]{String.valueOf(plantId)}, null, null, COL_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> map = new HashMap<>();
                map.put("height", String.valueOf(cursor.getFloat(cursor.getColumnIndexOrThrow(COL_GROWTH_HEIGHT))));
                map.put("date", cursor.getString(cursor.getColumnIndexOrThrow(COL_GROWTH_DATE)));
                list.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
    
    public void deleteLatestGrowthEntry(int plantId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Find the ID of the most recent entry for this plant
        Cursor cursor = db.query(TABLE_GROWTH, new String[]{COL_ID}, COL_PLANT_ID_FK + "=?",
                new String[]{String.valueOf(plantId)}, null, null, COL_ID + " DESC", "1");

        if (cursor.moveToFirst()) {
            long latestId = cursor.getLong(0);
            db.delete(TABLE_GROWTH, COL_ID + "=?", new String[]{String.valueOf(latestId)});
        }
        cursor.close();
        db.close();
    }

    public List<DiaryEntry> getPlantDiary(int plantId) {
        List<DiaryEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. Get all diary entries for this plant
        Cursor cursor = db.query(TABLE_DIARY, null, COL_PLANT_ID_FK + "=?",
                new String[]{String.valueOf(plantId)}, null, null, COL_DIARY_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_DIARY_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_DIARY_CONTENT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DIARY_DATE));

                // 2. For each entry, get its images
                List<String> images = new ArrayList<>();
                Cursor imgCursor = db.query(TABLE_DIARY_IMAGES, null, COL_DIARY_ID_FK + "=?",
                        new String[]{String.valueOf(id)}, null, null, null);

                if (imgCursor.moveToFirst()) {
                    do {
                        images.add(imgCursor.getString(imgCursor.getColumnIndexOrThrow(COL_DIARY_IMG_PATH)));
                    } while (imgCursor.moveToNext());
                }
                imgCursor.close();

                entries.add(new DiaryEntry(id, title, content, date, images));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entries;
    }

    public void deleteDiaryEntry(int diaryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete images associated with this entry first
        db.delete(TABLE_DIARY_IMAGES, COL_DIARY_ID_FK + "=?", new String[]{String.valueOf(diaryId)});
        // Delete the entry itself
        db.delete(TABLE_DIARY, COL_ID + "=?", new String[]{String.valueOf(diaryId)});
        db.close();
    }
}
