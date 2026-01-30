package com.example.sproutify;

public class Plant {
    int id;
    String name;
    String datePlanted;
    String imagePath;
    float height;
    String lastWater;
    String lastFertilize;
    String notes;

    public Plant() {
    }

    // Constructor matching the database columns
    public Plant(int id, String name, String datePlanted, String imagePath, float height, String lastWater, String lastFertilize, String notes) {
        this.id = id;
        this.name = name;
        this.datePlanted = datePlanted;
        this.imagePath = imagePath;
        this.height = height;
        this.lastWater = lastWater;
        this.lastFertilize = lastFertilize;
        this.notes = notes;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDatePlanted() { return datePlanted; }
    public String getImagePath() { return imagePath; }
    public float getHeight() { return height; }
    public String getLastWater() { return lastWater; }
    public String getLastFertilize() { return lastFertilize; }
    public String getNotes() { return notes; }
}
