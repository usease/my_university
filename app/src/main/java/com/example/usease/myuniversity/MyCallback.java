package com.example.usease.myuniversity;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

public interface MyCallback {

    void onCallback(DatabaseReference databaseReference);
}
