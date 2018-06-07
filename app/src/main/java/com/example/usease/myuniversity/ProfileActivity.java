package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView mStatus, mGroupStatus, mLastSeen, mStudentType, mProfileName;
    private Button mSendMessageBtn;
    private DatabaseReference mUserDatabase;
    private ImageView mImage;
    private ProgressDialog mProfileProgress;
    private String uid;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mCurrentUserDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //Initializing FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        //Getting reference to current user database
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        //Getting references to fields
        mProfileName = (TextView) findViewById(R.id.profile_name);
        mStatus = (TextView) findViewById(R.id.profile_status);
        mGroupStatus = (TextView) findViewById(R.id.profile_group_status);
        mLastSeen = (TextView) findViewById(R.id.profile_last_seen);
        mStudentType = (TextView) findViewById(R.id.profile_type);
        mImage = (ImageView) findViewById(R.id.profile_profile_image);

        //Getting ID of the user which has been selected and sent via intent
        uid = getIntent().getStringExtra("uid");

        //Getting reference for the profile of user of which profile is being viewed
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
        //Storing data locally
        mUserDatabase.keepSynced(true);

        //Initializing and setting progress dialog
        mProfileProgress = new ProgressDialog(this);
        mProfileProgress.setTitle("Refreshing...");
        mProfileProgress.setMessage("Refreshing user data. Please wait.");
        mProfileProgress.setCanceledOnTouchOutside(false);
        mProfileProgress.show();

        //Loading user data
        loadProfileData();

        mSendMessageBtn = (Button) findViewById(R.id.profile_send_message_btn);
        mSendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //When send message button is clicked, directing current user to chat room
                Intent chat_intent =  new Intent(ProfileActivity.this, ChatActivity.class);
                chat_intent.putExtra("uid", uid);
                startActivity(chat_intent);
            }
        });
    }

    //Function is responsible for loading user data
    private void loadProfileData() {
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Getting values from the cloud database
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String type = dataSnapshot.child("type").getValue().toString();
                String online = dataSnapshot.child("online").getValue().toString();
                String groupStatus = dataSnapshot.child("group_status").getValue().toString();

                //Setting acquired texts to fields
                mProfileName.setText(name);
                mStatus.setText(status);

                if(getResources().getConfiguration().locale == Locale.US){
                    mGroupStatus.setText(groupStatus);
                    mStudentType.setText(type);
                } else {

                    if (groupStatus.equalsIgnoreCase("Member")) {
                        mGroupStatus.setText("Guruh A\'zosi");
                    } else {
                        mGroupStatus.setText("A\'zo Emas");
                    }

                    if (type.equalsIgnoreCase("Student")) {
                        mStudentType.setText("Student");
                    } else {
                        mStudentType.setText("Guruh Sardori");
                    }
                }

                //Handling User Image
                final String profile_image_url = dataSnapshot.child("image").getValue().toString();
                if(!profile_image_url.equals("default")) {
                    //If user does have image and then load it. Otherwise do not attempt to load!
                    //Also, Picasso tries to load image from the local cache if network is not available. If it cant find image locally then it should
                    //try to download it
                    Picasso.with(ProfileActivity.this).load(profile_image_url).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(mImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            //If Picasso successfully finds the local cache of image, do nothing
                        }
                        @Override
                        public void onError() {
                            //else, that means we do not have the local image. Thus, there is a need to download
                            Picasso.with(ProfileActivity.this).load(profile_image_url).placeholder(R.drawable.default_avatar).into(mImage);
                        }
                    });
                }

                //Handling user last seen status
                if (online.equalsIgnoreCase("Online")) {
                    //if user is online, show him online
                    mLastSeen.setText(online);
                } else {
                    //Creating an instance of GetTimeAgo class. This class helps to calculate the last visit of the user
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastSeenTime = Long.parseLong(online); //Converting time from string to long
                    String lastSeenReady = getTimeAgo.getTimeAgo(lastSeenTime, getApplicationContext(), getResources().getConfiguration().locale); //Generating last seen
                    mLastSeen.setText(lastSeenReady);
                }

                //Dismissing the dialog after successful load
                mProfileProgress.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Hiding the dialog first
                mProfileProgress.hide();
                //Showing error message when there is error
                Toast.makeText(ProfileActivity.this, "Could not retrieve user data. " + databaseError, Toast.LENGTH_LONG).show();

            }
        });
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
