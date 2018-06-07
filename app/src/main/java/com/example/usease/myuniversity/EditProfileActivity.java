package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;


public class EditProfileActivity extends AppCompatActivity {

    private Button mSaveBtn;
    private TextInputLayout mName, mStatus, mGroup;
    private AutoCompleteTextView mUniversity;
    private RadioButton mRadiorCR, mRadioStudent;
    private Toolbar mToolbar;
    private FirebaseUser mUser;
    private DatabaseReference mUserDatabase;
    private ProgressDialog mEditProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //Referencing fields
        mName = (TextInputLayout) findViewById(R.id.edit_name);
        mStatus = (TextInputLayout) findViewById(R.id.edit_status);
        mGroup = (TextInputLayout) findViewById(R.id.edit_groupid);
        mRadiorCR = (RadioButton) findViewById(R.id.edit_radio_cr);
        mRadioStudent = (RadioButton) findViewById(R.id.edit_radio_student);
        mSaveBtn = (Button) findViewById(R.id.edit_save_btn);
        mEditProgress = new ProgressDialog(this);

        //Initializing and loading AutoCompleteTextView with data
        mUniversity  = (AutoCompleteTextView) findViewById(R.id.edit_university);
        //Creating an array to hold names of universities
        String [] uni_list = getResources().getStringArray(R.array.Universities);
        //Creating adapter to populate AutoCompleteTextView
        final ArrayAdapter<String> autoComplete = new ArrayAdapter<>(EditProfileActivity.this,android.R.layout.simple_list_item_1, uni_list );
        //Settings adapter for AutoCompleteTextView
        mUniversity.setAdapter(autoComplete);

        //Initializing and setting toolbar
        mToolbar = (Toolbar) findViewById(R.id.edit_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Edit Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Getting current user and its id
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = mUser.getUid();
        //Via id, acquiring cloud database storage path of the user
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

        //Loading user data
        loadUserData();

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //When user clicks save button, we first need to get edited data from the fields
                String name = mName.getEditText().getText().toString();
                String groupID = mGroup.getEditText().getText().toString();
                String status = mStatus.getEditText().getText().toString();
                String university = mUniversity.getText().toString();

                //Getting data from Radio Buttons
                String studentType;
                if(mRadioStudent.isChecked()) {
                    studentType = "Student";
                } else if (mRadiorCR.isChecked()){
                    studentType = "Course Representative";
                } else {
                    //If user did not select any type, automatically assign him as a student
                    studentType = "Student";
                }
                //Checking if user did not provide any blank fields
                if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(status) || !TextUtils.isEmpty(groupID) || !TextUtils.isEmpty(studentType) || !TextUtils.isEmpty(university)) {
                    //If all of the fields are filled correctly
                    //Show progress dialog
                    mEditProgress.setTitle("Saving...");
                    mEditProgress.setMessage("Please wait until we save your changes.");
                    mEditProgress.setCanceledOnTouchOutside(false); //Not allowing user to hide dialog by touching outside
                    mEditProgress.show();
                    //Updating user data
                    updateUserData(name, status, groupID, studentType, university);
                } else {
                    //If user provided any blank fields, showing friendly message
                    Toast.makeText(EditProfileActivity.this, "Empty field(s) detected. Please fill all of fields.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    //Function is responsible for updating user data
    private void updateUserData(String name, String status, String groupID, String studentType, String university) {
        //To save complex data, it is better to use HashMap
        Map userMap = new HashMap<>();
        userMap.put("name",name);
        userMap.put("university",university);
        userMap.put("groupid", groupID);
        userMap.put("type",studentType);
        userMap.put("status", status);

        //Saving mapped data to database. NOTE: WE NEED TO UPDATE CHILDREN!!!
        mUserDatabase.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //If the user is successfully created,then we need to proceed
                if(task.isSuccessful()) {

                    //Before finishing current activity, dismiss the progress dialog
                    mEditProgress.dismiss();

                    //Finishing activity
                    finish();
                } else {
                    //In case of errors, just hide the progress dialog
                    mEditProgress.hide();
                    Toast.makeText(EditProfileActivity.this, "Could not save your changes to the cloud. Please try again.", Toast.LENGTH_LONG).show();

                }
            }
        });

    }

    //Function is responsible for loading user data into fields
    private void loadUserData() {

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Getting values from the cloud database
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String university = dataSnapshot.child("university").getValue().toString();
                String groupid = dataSnapshot.child("groupid").getValue().toString();
                String type = dataSnapshot.child("type").getValue().toString();

                //Now, setting acquired data to fields
                mName.getEditText().setText(name);
                mStatus.getEditText().setText(status);
                mUniversity.setText(university);
                mGroup.getEditText().setText(groupid);

                //Getting type of user and setting Radio Buttons accordingly
                if(type.equalsIgnoreCase("cr")) {
                    mRadiorCR.setChecked(true);
                } else {
                    mRadioStudent.setChecked(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(EditProfileActivity.this, "Could not load user data. " + databaseError, Toast.LENGTH_SHORT).show();
            }
        });



    }
}
