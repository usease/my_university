package com.example.usease.myuniversity;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.sql.Timestamp;

public class MyUniversity extends Application {

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;
    @Override
    public void onCreate() {
        super.onCreate();

        //Enabling Firebase Database Persistence. This allows us to cashe data locally. Thus, we can benefit from offline capabilities
        //of Firebase.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        //Since Firebase only allows to sync string and int but not images. We need to handel that. To achieve this, we include OKHTTP
        //It allows (with the help of Picasso) us to store image locally.
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttpDownloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(false); //For debugging purposes, you can set it to TRUE
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null ){
            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());

            mUserDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot != null) {
                        mUserDatabase.child("online").onDisconnect().setValue(ServerValue.TIMESTAMP);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }
}

