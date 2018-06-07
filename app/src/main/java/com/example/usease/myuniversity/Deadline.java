package com.example.usease.myuniversity;

/**
 * Created by Usease on 2/27/2018.
 */

public class Deadline {

    String title;
    long date;
    int color;

    public Deadline(String title, long date, int color) {
        this.title = title;
        this.date = date;
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Deadline() {
        //Empty constructor
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
