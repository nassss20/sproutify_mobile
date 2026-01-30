package com.example.sproutify;

public class Reminder {
    private String id;
    private String title;
    private String date;
    private String time;
    private String description;
    private boolean isDone; // This field controls the checkbox state

    public Reminder() {
        // Empty constructor needed for Firestore
    }

    public Reminder(String title, String date, String time, String description, boolean isDone) {
        this.title = title;
        this.date = date;
        this.time = time;
        this.description = description;
        this.isDone = isDone;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getDescription() { return description; }
    public boolean isDone() { return isDone; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setDescription(String description) { this.description = description; }

    public void setDone(boolean done) { this.isDone = done; }
}