package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private Button mCreateAccountBtn;
    private TextInputLayout mEmail, mName, mGroupID, mPassword;
    private RadioButton mRadioStudent, mRadioCR;
    private AutoCompleteTextView mUniversity;
    private FirebaseAuth mAuth;
    private Toolbar mToolbar;
    private ProgressDialog mRegProgress;
    private DatabaseReference mDatabase;

    private String university;
    private String groupID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Referencing fields
        mEmail = (TextInputLayout) findViewById(R.id.reg_email);
        mName = (TextInputLayout) findViewById(R.id.reg_name);
        mGroupID = (TextInputLayout) findViewById(R.id.reg_groupid);
        mPassword = (TextInputLayout) findViewById(R.id.reg_password);
        mCreateAccountBtn = (Button) findViewById(R.id.reg_create_btn);
        mRadioCR = (RadioButton) findViewById(R.id.reg_radio_cr);
        mRadioStudent = (RadioButton) findViewById(R.id.reg_radio_student);
        mUniversity = (AutoCompleteTextView) findViewById(R.id.reg_university);

        //Referencing and setting up Toolbar
        mToolbar = (Toolbar) findViewById(R.id.reg_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //Enabling go back button in the Toolbar

        //Creating an array to hold names of universities
        String [] uni_list = getResources().getStringArray(R.array.Universities);
        //Creating adapter to populate AutoCompleteTextView
        final ArrayAdapter<String> autoComplete = new ArrayAdapter<>(RegisterActivity.this,android.R.layout.simple_list_item_1, uni_list );
        //Settings adapter for AutoCompleteTextView
        mUniversity.setAdapter(autoComplete);

        //Referencing FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        //Initializing progress dialog
        mRegProgress = new ProgressDialog(this);

        mCreateAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Getting data from user
                String name = mName.getEditText().getText().toString();
                String email = mEmail.getEditText().getText().toString();
                groupID = mGroupID.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();
                university = mUniversity.getText().toString();

                //Getting data from Radio Buttons
                String studentType;
                if(mRadioStudent.isChecked()) {
                     studentType = "Student";
                } else if (mRadioCR.isChecked()){
                     studentType = "Course Representative";
                } else {
                    //If user did not select any type, automatically assign him as a student
                    studentType = "Student";
                }

                //Checking if user did not provide any blank fields
                if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(email) || !TextUtils.isEmpty(groupID) || !TextUtils.isEmpty(password) || !TextUtils.isEmpty(university)) {
                    //If all of the fields are filled correctly
                    //Show progress dialog
                    mRegProgress.setTitle("Registering...");
                    mRegProgress.setMessage("Please wait until we create your profile");
                    mRegProgress.setCanceledOnTouchOutside(false); //Not allowing user to hide dialog by touching outside
                    mRegProgress.show();
                    //Following function is responsible for user registration
                    registerUser(name, email, password, groupID, studentType, university);
                } else {
                    //If user provided any blank fields, showing friendly message
                    Toast.makeText(RegisterActivity.this, "Empty field(s) detected. Please fill all of fields.", Toast.LENGTH_LONG).show();
                }

            }
        });
    }
    //Registering user
    private void registerUser(final String name, String email, String password, final String groupID, final String studentType, final String university) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {

                    //Getting user id
                    FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                    final String uid = current_user.getUid();

                    //Getting reference to FirebaseDatabase and referencing it to users directory
                    mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);



                    //------------------------------Checking if the group has more than 3 members-------------------
                    FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupID).child("Members").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            final String group_status;
                            long  member_count = dataSnapshot.getChildrenCount();
                            if(member_count >= 3){
                                group_status = "Not Member";
                            } else {
                                group_status = "Member";
                            }

                            //To save complex data, it is better to use HashMap
                            HashMap<String, String> userMap = new HashMap<>();
                            userMap.put("name",name);
                            userMap.put("university",university);
                            userMap.put("group_status", group_status);
                            userMap.put("groupid", groupID);
                            userMap.put("type",studentType);
                            userMap.put("image","default");
                            userMap.put("thumb_image","default");
                            userMap.put("status", "Hi there, I am using My University App");

                            //Saving mapped data to database
                            mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    //If the user is successfully created,then we need to proceed
                                    if(task.isSuccessful()) {

                                        if (!group_status.equalsIgnoreCase("Member")) {

                                            //To save complex data, it is better to use HashMap
                                            HashMap<String, Object> requestMap = new HashMap<>();
                                            requestMap.put("accept_count", 0);
                                            requestMap.put("date", ServerValue.TIMESTAMP);
                                            requestMap.put("decline_count", 0);
                                            requestMap.put("uid", uid);

                                            FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupID).child("Requests").push().setValue(requestMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                }
                                            });
                                        }

                                        //Before moving to another Activity, dismiss the progress dialog
                                        mRegProgress.dismiss();

                                        Intent main_intent = new Intent(RegisterActivity.this, MainActivity.class);

                                        //Since the RegisterActivity has parent StartActivity, when new activity is created we need to create whole new task
                                        //so that we cant go back to StartActivity from the MainActivity
                                        main_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                                        startActivity(main_intent);

                                        //User should not be able to come back to RegisterActivity, thus we need to finish this activity
                                        finish();
                                    } else {
                                        //In case of errors, just hide the progress dialog
                                        mRegProgress.hide();
                                        Toast.makeText(RegisterActivity.this, "Could not write user data to the cloud. Please try again.", Toast.LENGTH_LONG).show();

                                    }

                                }
                            });







                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });









                } else {
                    //In case of errors, just hide the progress dialog
                    mRegProgress.hide();

                    Toast.makeText(RegisterActivity.this, "There were problems when creating your account. Please try again", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

}
