package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import org.w3c.dom.Text;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class AddAnnouncementActivity extends AppCompatActivity {


    private Toolbar mToolbar;
    private RadioButton mRadioInformation, mRadioWarning, mRadioNews, mRadioUniversity, mRadioGroupOnly;
    private TextInputLayout mTitle, mDesc;
    private ImageButton mImage;
    private ProgressDialog mProgressDialog;
    private static final int GALLERY_REQUEST = 1;
    private Uri mImageUri = null;
    private DatabaseReference mCurrentUserDatabase;
    private FirebaseUser mCurrentUser;
    private StorageReference mAnnouncementImageStorage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_announcement);

        mRadioInformation = (RadioButton) findViewById(R.id.add_announcement_radio_type_information);
        mRadioNews = (RadioButton) findViewById(R.id.add_announcement_radio_type_news);
        mRadioWarning = (RadioButton) findViewById(R.id.add_announcement_radio_type_warning);
        mRadioUniversity = (RadioButton) findViewById(R.id.add_announcement_radio_coverage_university);
        mRadioGroupOnly = (RadioButton) findViewById(R.id.add_announcement_radio_coverage_group);
        mImage = (ImageButton) findViewById(R.id.add_announcement_image);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());
        mTitle = (TextInputLayout) findViewById(R.id.add_announcement_title);
        mDesc = (TextInputLayout) findViewById(R.id.add_announcement_desc);


        mRadioInformation.setChecked(true);
        mRadioGroupOnly.setChecked(true);

        mProgressDialog = new ProgressDialog(this);
        mAnnouncementImageStorage = FirebaseStorage.getInstance().getReference().child("announcement_images");

        mToolbar = (Toolbar) findViewById(R.id.add_announcement_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.add_announcement);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery_intent = new Intent(Intent.ACTION_GET_CONTENT);
                gallery_intent.setType("image/*");
                startActivityForResult(gallery_intent, GALLERY_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            mImageUri = data.getData();
            mImage.setImageURI(mImageUri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.add_announcement_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.save_announcement_btn) {
           mCurrentUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
               @Override
               public void onDataChange(DataSnapshot dataSnapshot) {

                   String university = dataSnapshot.child("university").getValue().toString();
                   String groupid = dataSnapshot.child("groupid").getValue().toString();

                   String title = mTitle.getEditText().getText().toString().trim();
                   String desc = mDesc.getEditText().getText().toString().trim();

                   //Getting data from Radio Buttons
                   String coverage;
                   if(mRadioGroupOnly.isChecked()) {
                       coverage = groupid;
                   } else if (mRadioUniversity.isChecked()){
                       coverage = "University";
                   } else {
                       coverage = groupid;
                   }
                   String type;
                   if(mRadioInformation.isChecked()) {
                       type = "Information";
                   } else if (mRadioNews.isChecked()) {
                       type = "News";
                   } else if (mRadioWarning.isChecked()) {
                       type = "Warning";
                   } else {
                       type = "Information";
                   }

                   //Checking if user did not provide any blank fields
                   if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(desc)) {
                       //If all of the fields are filled correctly
                       //Show progress dialog
                       mProgressDialog.setMessage("Creating Announcement...");
                       mProgressDialog.setCanceledOnTouchOutside(false); //Not allowing user to hide dialog by touching outside
                       mProgressDialog.show();
                       //Following function is responsible for user registration
                       createAnnouncement(title, desc, university, type, coverage);
                   } else {
                       //If user provided any blank fields, showing friendly message
                       Toast.makeText(AddAnnouncementActivity.this, R.string.empty_fields_detected, Toast.LENGTH_LONG).show();
                   }
               }

               @Override
               public void onCancelled(DatabaseError databaseError) {

               }
           });
        }
        return super.onOptionsItemSelected(item);
    }

    private void createAnnouncement(String title, String desc, String university, String type, String coverage) {
        String uid = mCurrentUser.getUid();
        //To save complex data, it is better to use HashMap
        final Map announcementMap = new HashMap<>();
        announcementMap.put("title",title);
        announcementMap.put("desc",desc);
        announcementMap.put("coverage", coverage);
        announcementMap.put("type",type);
        announcementMap.put("date", ServerValue.TIMESTAMP);
        announcementMap.put("uid", uid);
        announcementMap.put("likes", 0);

        final DatabaseReference announcements_ref =  FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Announcements").push();
        final String announcement_id = announcements_ref.getKey();

        if (mImageUri != null) {
            mAnnouncementImageStorage.child(announcement_id).putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri uri = taskSnapshot.getDownloadUrl();
                    String downloadUrl = uri.toString();
                    announcementMap.put("image", downloadUrl);

                    announcements_ref.setValue(announcementMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()) {
                                mProgressDialog.dismiss();
                                Intent announcement_intent = new Intent(AddAnnouncementActivity.this, AnnouncementsActivity.class);
                                startActivity(announcement_intent);
                            } else {
                                //In case of errors, just hide the progress dialog
                                mProgressDialog.hide();
                                Toast.makeText(AddAnnouncementActivity.this, R.string.could_not_create_your_announcement, Toast.LENGTH_LONG).show();

                            }
                        }
                    });
                }
            });
        } else {

            announcementMap.put("image", "default");

            announcements_ref.setValue(announcementMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {
                        mProgressDialog.dismiss();
                        Intent announcement_intent = new Intent(AddAnnouncementActivity.this, AnnouncementsActivity.class);
                        startActivity(announcement_intent);
                    } else {
                        //In case of errors, just hide the progress dialog
                        mProgressDialog.hide();
                        Toast.makeText(AddAnnouncementActivity.this, R.string.could_not_create_your_announcement, Toast.LENGTH_LONG).show();

                    }
                }
            });
        }
    }
}
