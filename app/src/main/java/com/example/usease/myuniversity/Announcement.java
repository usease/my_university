package com.example.usease.myuniversity;


public class Announcement {
    private String coverage;
    private long date;
    private String desc;
    private String image;
    private int likes;
    private String title;
    private String type;
    private String uid;

    public Announcement () {
        //Empty constructor
    }

    public Announcement(String coverage, long date, String desc, String image, int likes, String title, String type, String uid) {
        this.coverage = coverage;
        this.date = date;
        this.desc = desc;
        this.image = image;
        this.likes = likes;
        this.title = title;
        this.type = type;
        this.uid = uid;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
