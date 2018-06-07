package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import id.zelory.compressor.Compressor;


public class SettingsActivity extends AppCompatActivity {

    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;

    private ImageView mImage;
    private TextView mName;
    private TextView mStatus;
    private TextView mUniversity;
    private TextView mGroup;
    private TextView mType;
    private TextView mOnline;
    private TextView mGroupStatus;
    private ProgressDialog mSettingsProgress;
    private Button mImagebtn;
    private Button mEditBtn;
    private StorageReference mUserImageStorage;
    private FirebaseUser current_user;
    private String uid;

    private static final int IMAGE_PICK_CODE = 1; //Code to start Gallery Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Referencing the fields
        mImage = (ImageView) findViewById(R.id.settings_profile_image);
        mName = (TextView) findViewById(R.id.settings_name);
        mStatus = (TextView) findViewById(R.id.settings_status);
        mUniversity  = (TextView) findViewById(R.id.settings_university);
        mGroup = (TextView) findViewById(R.id.settings_groupid);
        mType = (TextView) findViewById(R.id.settings_student_type);
        mOnline = (TextView) findViewById(R.id.settings_last_seen);
        mEditBtn = (Button) findViewById(R.id.settings_edit_btn);
        mImagebtn = (Button) findViewById(R.id.settings_image_btn);
        mGroupStatus = (TextView) findViewById(R.id.settings_group_status);

        //Referencing to storage
        mUserImageStorage = FirebaseStorage.getInstance().getReference();

        //Getting current user and then its ID
        current_user = FirebaseAuth.getInstance().getCurrentUser();
        uid = current_user.getUid();
        //Getting reference for the current user database
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        //Storing data locally
        mUserDatabase.keepSynced(true);

        //Initializing and setting progress dialog
        mSettingsProgress = new ProgressDialog(this);
        mSettingsProgress.setTitle("Refreshing...");
        mSettingsProgress.setMessage("Refreshing user data. Please wait.");
        mSettingsProgress.setCanceledOnTouchOutside(false);
        mSettingsProgress.show();

        loadUserData(); //Loading user data

        mImagebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //When user clicks change image button, creating new intent
                Intent gallery_intent = new Intent();
                //Setting the type to image so that we can pick only images
                gallery_intent.setType("image/*");
                gallery_intent.setAction(Intent.ACTION_GET_CONTENT);
                //Initializing Default File Chooser of the phone
                startActivityForResult(Intent.createChooser(gallery_intent, "SELECT IMAGE"), IMAGE_PICK_CODE);
            }
        });

        mEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Sending user to EditProfileActivity
                Intent edit_intent = new Intent (SettingsActivity.this, EditProfileActivity.class);
                startActivity(edit_intent);
            }
        });
    }

    //Function responsible for loading user data from the cloud database
    private void loadUserData() {

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)  {

                //Getting values from the cloud database
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String university = dataSnapshot.child("university").getValue().toString();
                String groupid = dataSnapshot.child("groupid").getValue().toString();
                String type = dataSnapshot.child("type").getValue().toString();
                String online = dataSnapshot.child("online").getValue().toString();
                String groupStatus = dataSnapshot.child("group_status").getValue().toString();

                //Handling User Image
                final String profile_image_url = dataSnapshot.child("image").getValue().toString();
                if(!profile_image_url.equals("default")) {
                    //if user does have image and then load it. Otherwise do not attempt to load!
                    //Also, Picasso tries to load image from the local cache if network is not available. If it cant find image locally then it should
                    //try to download it
                    Picasso.with(SettingsActivity.this).load(profile_image_url).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(mImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            //If Picasso successfully finds the local cache of image, do nothing
                        }
                        @Override
                        public void onError() {
                            //else, that means we dont have the local image. Thus, there is a need to download
                            Picasso.with(SettingsActivity.this).load(profile_image_url).placeholder(R.drawable.default_avatar).into(mImage);
                        }
                    });
                }

                //Now, setting acquired data to fields
                mName.setText(name);
                mStatus.setText(status);
                mUniversity.setText(university);
                mGroup.setText(groupid);


                if(getResources().getConfiguration().locale == Locale.US){
                    mGroupStatus.setText(groupStatus);
                    mType.setText(type);
                } else {

                    if (groupStatus.equalsIgnoreCase("Member")) {
                        mGroupStatus.setText("Guruh A\'zosi");
                    } else {
                        mGroupStatus.setText("A\'zo Emas");
                    }

                    if (type.equalsIgnoreCase("Student")) {
                        mType.setText("Student");
                    } else {
                        mType.setText("Guruh Sardori");
                    }
                }

                //Handling user last seen status
                if (online.equalsIgnoreCase("Online")) {
                    //if user is online, show him online
                   mOnline.setText(online);
                } else {

                    //Creating an instance of GetTimeAgo class. This class helps to calculate the last visit of the user
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastSeenTime = Long.parseLong(online); //Converting time from string to long
                    String lastSeenReady = getTimeAgo.getTimeAgo(lastSeenTime, getApplicationContext(), getResources().getConfiguration().locale); //Generating last seen
                   mOnline.setText(lastSeenReady);
                }

                //Dismissing the dialog after successful load
                mSettingsProgress.dismiss();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Hiding the dialog first
                mSettingsProgress.hide();
                //Showing error message when there is error
                Toast.makeText(SettingsActivity.this, "Could not retrieve user data. " + databaseError, Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
                //Getting reference to the image which had been selected and making it source file uri
                final Uri sourceUri = data.getData();
                //The cropped image will be saved in the cache for a while. Thus, creating temporary file to hold cropped image
                File tempCropped = new File (getCacheDir(), uid);
                //This file's URI is used to upload the cropped image. Thus, getting URI of the file.
                Uri destinationUri = Uri.fromFile(tempCropped);
                //Calling UCROP library to crop the image. NOTE: WE PROVIDE THE SOURCE AND DESTINATION URIs.
                UCrop.of(sourceUri, destinationUri)
                        .withAspectRatio(4, 3)  //Certain formatting to disable users from uploading too large or small files
                        .withMaxResultSize(800, 600)
                        .start(this);
            }

            if (requestCode == UCrop.REQUEST_CROP) {

                //If image is successfully cropped, getting the resulting URI of the cropped image
                Uri imgUri = UCrop.getOutput(data);
                //Setting up Progress Dialog accordingly
                mSettingsProgress.setTitle("Uploading image...");
                mSettingsProgress.setMessage("Please wait until your image is uploaded.");
                mSettingsProgress.setCanceledOnTouchOutside(false); //Not enabling user to quit dailog by touching outside of dialog
                mSettingsProgress.show();
                //Uploading the image
                uploadImage(imgUri);
                //Uploading thumbnail
                uploadThumb(imgUri);

            }
    }

    //Function is responsible for uploading thumbnail of the image.
    private void uploadThumb(Uri imgUri) {

        //Creating file from copped image URI. This file will later be needed when as parameter for the compressor lib.
        File thumb_file_path = new File(imgUri.getPath());
        //Due to Java IOException, wrapping the method inside the try catch statements
        try {
            //Creating bitmap from the user image
            Bitmap thumb_bitmap = new Compressor(this)
                    .setMaxWidth(200)  //Setting max width and height for the thumb
                    .setMaxHeight(200)
                    .setQuality(50) //Setting the quality to only 50%
                    .compressToBitmap(thumb_file_path);

            //Following steps are done to save image as bitmap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte [] thumb_byte = baos.toByteArray();

            //Getting reference to the thumb image storage directory
            StorageReference thumb_reference =  mUserImageStorage.child("profile_images").child("thumbs").child(uid);

            //Uploading bitmap
            UploadTask uploadTask = thumb_reference.putBytes(thumb_byte);
            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {
                    String thumb_download_url = thumb_task.getResult().getDownloadUrl().toString();
                    if(thumb_task.isSuccessful()) {
                        //Saving the bitmap
                        mUserDatabase.child("thumb_image").setValue(thumb_download_url);

                    } else {
                        //Showing error message
                        Toast.makeText(SettingsActivity.this, "Error in uploading thumb nail", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Function is responsible for uploading user image
    private void uploadImage(Uri imgUri) {

        //Getting reference for specific directory of the Storage
        StorageReference userSpecificPath =  mUserImageStorage.child("profile_images").child(uid);
        userSpecificPath.putFile(imgUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if( task.isSuccessful()) {
                    //Having successfully uploaded, getting image download URL and saving it into User Database
                    String download_url = task.getResult().getDownloadUrl().toString();
                    mUserDatabase.child("image").setValue(download_url).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                            } else {
                                //Showing errors in case of errors
                                Toast.makeText(SettingsActivity.this, "Could not update user profile with new image URL.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    //Ending the Progress Dialog
                    mSettingsProgress.dismiss();
                } else {
                    //Hiding dialog if error occurs
                    mSettingsProgress.hide();
                    //Showing errors in case of errors
                    Toast.makeText(SettingsActivity.this, "Error occurred when image was being uploaded.", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        //Check if the user signed in (non-null)
        if(current_user != null) {
            //If the user is authenticated and
            //when the activity is started, we set the user online
            mUserDatabase.child("online").setValue("Online");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Check if the user signed in (non-null)
        if(current_user != null) {
            //When activity is stopped or minimized, that means user is not online anymore
            mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

}
