package com.example.usease.myuniversity;

/**
 * Created by Usease on 3/3/2018.
 */

public class Timetable {
    private String time;
    private String room;
    private String teacher;
    private String type;
    private String name;
    private int color;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Timetable(String time, String room, String teacher, String type, String name, int color) {
        this.time = time;
        this.room = room;
        this.teacher = teacher;
        this.type = type;
        this.name = name;
        this.color = color;
    }

    public Timetable() {
        //Empty constructor
    }
}
