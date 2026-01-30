package com.example.sproutify;

import java.util.List;

public class DiaryEntry {
    int id;
    String title;
    String content;
    String date;
    List<String> imagePaths;

    public DiaryEntry(int id, String title, String content, String date, List<String> imagePaths) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.imagePaths = imagePaths;
    }
}