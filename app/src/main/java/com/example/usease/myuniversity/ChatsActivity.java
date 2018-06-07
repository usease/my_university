package com.example.usease.myuniversity;

import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.Locale;

public class ChatsActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TabViewPagerAdapater mTabViewPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mCurrentUserDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        //Initializing FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        //Getting reference to current user database
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        //Initializing and setting Toolbar
        mToolbar = (Toolbar) findViewById(R.id.chats_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.chats);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //Enabling going back button

        //Initializing ViewPager
        mViewPager = (ViewPager) findViewById(R.id.tabPager);

        Locale locale = getResources().getConfiguration().locale;


        //Initializing our adapter class
        mTabViewPagerAdapter = new TabViewPagerAdapater(getSupportFragmentManager(), getApplicationContext(), locale);
        //Setting adapter to the ViewPager
        mViewPager.setAdapter(mTabViewPagerAdapter);
        //Initializing TabLayout and creating it with ViewPager
        mTabLayout = (TabLayout) findViewById(R.id.chats_tabs);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Check if the user signed in (non-null)
        if(mCurrentUser != null) {
            //If the user is authenticated and
            //when the activity is started, we set the user online
            mCurrentUserDatabase.child(mCurrentUser.getUid()).child("online").setValue("Online");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Check if the user signed in (non-null)
        if(mCurrentUser != null) {
            //When activity is stopped or minimized, that means user is not online anymore
            mCurrentUserDatabase.child(mCurrentUser.getUid()).child("online").setValue(ServerValue.TIMESTAMP);
        }
    }
}
