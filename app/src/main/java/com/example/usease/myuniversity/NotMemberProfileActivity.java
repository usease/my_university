package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
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
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotMemberProfileActivity extends AppCompatActivity {

    private TextView mStatus, mGroupStatus, mLastSeen, mStudentType, mProfileName, mRequestText, mAcceptPercentage, mTotalAccepts;
    private Button mAcceptRequestBtn, mDeclineRequestBtn;
    private DatabaseReference mUserDatabase;
    private ImageView mImage;
    private ProgressDialog mNotMemberProgress;
    private String uid;
    private DatabaseReference mRequestsDatabase;
    private DatabaseReference mCurrentUserRequestDatabase;
    private String mCurrentUID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_not_member_profile);

        //Getting references to fields
        mProfileName = (TextView) findViewById(R.id.notmember_name);
        mStatus = (TextView) findViewById(R.id.notmember_status);
        mGroupStatus = (TextView) findViewById(R.id.notmember_group_status);
        mLastSeen = (TextView) findViewById(R.id.notmember_last_seen);
        mStudentType = (TextView) findViewById(R.id.notmember_type);
        mImage = (ImageView) findViewById(R.id.notmember_profile_image);
        mRequestText = (TextView) findViewById(R.id.notmember_request_text);
        mAcceptPercentage = (TextView) findViewById(R.id.notmember_accept_percentage);
        mTotalAccepts = (TextView) findViewById(R.id.notmember_total_accepts);
        mAcceptRequestBtn = (Button) findViewById(R.id.notmember_accept_request_btn);
        mDeclineRequestBtn = (Button) findViewById(R.id.notmember_decline_request_btn);

        //Getting uid from the intent
        uid = getIntent().getStringExtra("uid");

        //Getting reference for the profile of user of which profile is being viewed
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

        //Getting id of current user
        final FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUID = current_user.getUid();

        //Initializing and setting progress dialog
        mNotMemberProgress = new ProgressDialog(this);
        mNotMemberProgress.setTitle("Refreshing...");
        mNotMemberProgress.setMessage("Refreshing user data. Please wait.");
        mNotMemberProgress.setCanceledOnTouchOutside(false);
        mNotMemberProgress.show();

        //Loading user data
        loadProfileData();



        //Checking if the current user already accepted request of the user. Thus, enabling or disabling accept/decline buttons accordingly.
        checkIfCurrentUserAcceptedOrDeclinedRequest();


        //When the Accept Request Button is clicked
        mAcceptRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Getting reference to the database where user's request data is stored
                mCurrentUserRequestDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //Getting accept and decline counts from the database
                                String decline_count = dataSnapshot.child("decline_count").getValue().toString();
                                String accept_count = dataSnapshot.child("accept_count").getValue().toString();
                                //Converting them into INT to perform operations
                                int decline_count_number = Integer.parseInt(decline_count);
                                int accept_count_number = Integer.parseInt(accept_count);
                                //This is tricky. If, after load finishes, both buttons are enabled, this means that that current user has not voted yet.
                                //It means that current users did not accept or decline neither. Thus, when the user clicks Accept button, we increase
                                //accept count by 1.
                                if(mAcceptRequestBtn.isEnabled() && mDeclineRequestBtn.isEnabled()) {
                                    accept_count_number++;

                                } else {
                                    //If buttons are enabled, that means user has already accepted the request. Thus, when the user again clicks Accept
                                    //this means that user is changing from decline->accept. Thus, decline count should be less now. Similarly, accept count increases.
                                    decline_count_number--;
                                    accept_count_number++;
                                }
                                //Having handled the accept and decline counts, we save them to cloud
                                mCurrentUserRequestDatabase.child("decline_count").setValue(decline_count_number);
                                mCurrentUserRequestDatabase.child("accept_count").setValue(accept_count_number);

                                //Checking if the Decline button is disabled
                                boolean isDeclinedDisabled = mDeclineRequestBtn.isEnabled();
                                //Changing the layout of Request Section of the layout
                                handleConditionsWhenUserAlreadyAcceptedRequest();
                                //Saving that current user has accepted user's request
                                mCurrentUserRequestDatabase.child("accepted_uids").push().child("uid").setValue(mCurrentUID);
                                //If Decline Button is disabled that further means that current user has already declined the request
                                //Therefore, before accepting, we need to delete the Decline data of current user
                                if(!isDeclinedDisabled) {
                                    //Getting reference to list which shows which users declined the request
                                    mCurrentUserRequestDatabase.child("declined_uids").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            //Looking through each child
                                            for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                                                //Getting current child's user ID
                                                String child_uid = childSnapshot.child("uid").getValue().toString();
                                                //If user's ID is the same as current users ID
                                                if(child_uid.equals(mCurrentUID)) {
                                                    //We delete this node. Because current user cannot be in the DECLINED LIST anymore since he accepted the request
                                                    mCurrentUserRequestDatabase.child("declined_uids").child(childSnapshot.getKey().toString()).setValue(null);
                                                }
                                            }
                                        }
                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                //In case of errors, showing error message
                                Toast.makeText(NotMemberProfileActivity.this, "Could not save your Accept. " + databaseError.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                checkRequestStatus();
            }
        });
        //When the Decline Request Button is clicked
        mDeclineRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Getting reference to the database where user's request data is stored
                mCurrentUserRequestDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Getting accept and decline counts from the database
                        String decline_count = dataSnapshot.child("decline_count").getValue().toString();
                        String accept_count = dataSnapshot.child("accept_count").getValue().toString();
                        //Converting them into INT to perform operations
                        int decline_count_number = Integer.parseInt(decline_count);
                        int accept_count_number = Integer.parseInt(accept_count);
                        //This is tricky. If, after load finishes, both buttons are enabled, this means that that current user has not voted yet.
                        //It means that current users did not accept or decline neither. Thus, when the user clicks Decline button, we increase
                        //decline count by 1.
                        if(mAcceptRequestBtn.isEnabled() && mDeclineRequestBtn.isEnabled()) {
                            decline_count_number++;
                        } else {
                            //If buttons are enabled, that means user has already declined the request. Thus, when the user again clicks Decline
                            //this means that user is changing from accept->decline. Thus, accept count should be less now. Similarly, decline count increases.
                            decline_count_number++;
                            accept_count_number--;
                        }
                        //Having handled the accept and decline counts, we save them to cloud
                        mCurrentUserRequestDatabase.child("decline_count").setValue(decline_count_number);
                        mCurrentUserRequestDatabase.child("accept_count").setValue(accept_count_number);

                        //Checking if the Accept button is disabled
                        boolean isAcceptedDisabled = mAcceptRequestBtn.isEnabled();
                        //Changing the layout of Request Section of the layout
                        handleConditionsWhenUserAlreadyDeclinedRequest();

                        //Saving that current user has declined user's request
                        mCurrentUserRequestDatabase.child("declined_uids").push().child("uid").setValue(mCurrentUID);
                        //If Accept Button is disabled that further means that current user has already accepted the request
                        //Therefore, before declining, we need to delete the Accept data of current user
                        if(!isAcceptedDisabled) {
                            //Getting reference to list which shows which users accepted the request
                            mCurrentUserRequestDatabase.child("accepted_uids").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    //Looking through each child
                                    for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                                        //Getting current child's user ID
                                        String child_uid = childSnapshot.child("uid").getValue().toString();
                                        //If user's ID is the same as current users ID
                                        if(child_uid.equals(mCurrentUID)) {
                                            //We delete this node. Because current user cannot be in the ACCEPTED LIST anymore since he declined the request
                                            mCurrentUserRequestDatabase.child("accepted_uids").child(childSnapshot.getKey().toString()).setValue(null);
                                        }
                                    }
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //In case of errors, showing error message
                        Toast.makeText(NotMemberProfileActivity.this, "Could not save your Decline. " + databaseError.toString(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }
    //Function is responsible for checking if the user has already accepted or declined the request
    private void checkIfCurrentUserAcceptedOrDeclinedRequest() {
        //Getting reference to current user database
        DatabaseReference mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUID);
        mCurrentUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Acquiring user university and group data
                String university = dataSnapshot.child("university").getValue().toString();
                String group = dataSnapshot.child("groupid").getValue().toString();

                //With that data, trying to gain reference to the Group Requests section of the database
                final DatabaseReference RequestsDatabase = FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group).child("Requests");

                //Having gained the reference
                RequestsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Looping through each REQUEST in the group
                        for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                            //Getting ID of each request owner
                            String child_uid = childSnapshot.child("uid").getValue().toString();
                            //If child's ID is the same as our user ID
                            if(child_uid.equals(uid)) {
                                //That's means that we successfully found the request made by our user. Now, there is 2 scenarios:

                                //1. Getting reference to this specific request in the database and further finding list which shows user who accepted the request
                                DatabaseReference CurrentUserAcceptStatus = RequestsDatabase.child(childSnapshot.getKey().toString()).child("accepted_uids");
                                CurrentUserAcceptStatus.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        //Going through each user who accepted the request
                                        for (DataSnapshot childSnapShot: dataSnapshot.getChildren()) {
                                            //Getting user ID of child
                                            String child_uid = childSnapShot.child("uid").getValue().toString();
                                            //If child's ID is the same as our current user ID, that means that our current user has already accepted the request.
                                            //Because we could find our user in the list which shows accepted users
                                            if(child_uid.equals(mCurrentUID)) {
                                                //Therefore, we update the UI accordingly.
                                                handleConditionsWhenUserAlreadyAcceptedRequest();
                                            }
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });

                                //2. Getting reference to this specific request in the database and further finding list which shows user who declined the request
                                DatabaseReference CurrentUserDeclineStatus = RequestsDatabase.child(childSnapshot.getKey().toString()).child("declined_uids");
                                CurrentUserDeclineStatus.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        //Going through each user who declined the request
                                        for (DataSnapshot childSnapShot: dataSnapshot.getChildren()) {
                                            //Getting user ID of child
                                            String child_uid = childSnapShot.child("uid").getValue().toString();
                                            //If child's ID is the same as our current user ID, that means that our current user has already declined the request.
                                            //Because we could find our user in the list which shows declined users
                                            if(child_uid.equals(mCurrentUID)) {
                                                //Therefore, we update the UI accordingly.
                                                handleConditionsWhenUserAlreadyDeclinedRequest();
                                            }
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    //Function is responsible to update UI when the user has already ACCEPTED the request
    private void handleConditionsWhenUserAlreadyAcceptedRequest() {
        //Disabling accept button
        mAcceptRequestBtn.setEnabled(false);
        mAcceptRequestBtn.setAlpha(.5f);
        mRequestText.setText(R.string.already_accepted);
        //Enabling decline button
        mDeclineRequestBtn.setAlpha(1f);
        mDeclineRequestBtn.setEnabled(true);
    }

    //Function is responsible to update UI when the user has already DECLINED the request
    private void handleConditionsWhenUserAlreadyDeclinedRequest() {
        //Disabling decline button
        mDeclineRequestBtn.setEnabled(false);
        mDeclineRequestBtn.setAlpha(.5f);
        mRequestText.setText(R.string.already_declined);
        //Enabling accept button
        mAcceptRequestBtn.setEnabled(true);
        mAcceptRequestBtn.setAlpha(1f);
    }

    //Function is responsible for loading user data and in the meantime calls loadRequestData() as well
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

                //We also try to load Request data
                loadRequestData();

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
                    //if user does have image and then load it. Otherwise do not attempt to load!
                    //Also, Picasso tries to load image from the local cache if network is not available. If it cant find image locally then it should
                    //try to download it
                    Picasso.with(NotMemberProfileActivity.this).load(profile_image_url).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(mImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            //If Picasso successfully finds the local cache of image, do nothing
                        }

                        @Override
                        public void onError() {
                            //else, that means we do not have the local image. Thus, there is a need to download
                            Picasso.with(NotMemberProfileActivity.this).load(profile_image_url).placeholder(R.drawable.default_avatar).into(mImage);
                        }
                    });
                }


                //Handling user last seen status
                if (online.equalsIgnoreCase("Online")) {
                   mLastSeen.setText(online);
                } else {
                    //Creating an instance of GetTimeAgo class. This class helps to calculate the last visit of the user
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastSeenTime = Long.parseLong(online); //Converting time from string to long
                    String lastSeenReady = getTimeAgo.getTimeAgo(lastSeenTime, getApplicationContext(), getResources().getConfiguration().locale); //Generating last seen
                    mLastSeen.setText(lastSeenReady);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Hiding the dialog first
                mNotMemberProgress.hide();
                //Showing error message when there is error
                Toast.makeText(NotMemberProfileActivity.this, "Could not retrieve user data. " + databaseError, Toast.LENGTH_LONG).show();
            }
        });
    }

    //Function is responsible for loading request data
    private void loadRequestData() {

        DatabaseReference mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUID);
        mCurrentUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String university = dataSnapshot.child("university").getValue().toString();
                String group = dataSnapshot.child("groupid").getValue().toString();

                //Getting reference to the requests section in the cloud
                mRequestsDatabase = FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group).child("Requests");
                mRequestsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (final DataSnapshot childSnapshot: dataSnapshot.getChildren()) {

                            //We need to make sure that we are working with correct request. Thus, checking request's UID with our UID.
                            String request_uid = childSnapshot.child("uid").getValue().toString();
                            if(request_uid.equals(uid)) {
                                mCurrentUserRequestDatabase = mRequestsDatabase.child(childSnapshot.getKey().toString());

//                                //Storing data locally
//                                mCurrentUserRequestDatabase.keepSynced(true);

                                mCurrentUserRequestDatabase.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        //Presenting accept count and perecentage of the request
                                        //Initially getting values from the database
                                        String accept_count = dataSnapshot.child("accept_count").getValue().toString();
                                        String decline_count = dataSnapshot.child("decline_count").getValue().toString();
                                        //Converting them into INT
                                        int accept_count_number = Integer.parseInt(accept_count);
                                        int decline_count_number = Integer.parseInt(decline_count);

                                        if (accept_count_number != 0 || decline_count_number !=0){
                                            //Performing calculations
                                            double percentage =  (accept_count_number*100/(accept_count_number + decline_count_number));
                                            //Setting the results to TextViews
                                            mAcceptPercentage.setText(percentage  + "%");
                                            mTotalAccepts.setText(accept_count + "/" +  (decline_count_number + accept_count_number));

                                        }

                                        //Dismissing the dialog after successful load
                                        mNotMemberProgress.dismiss();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                        //Hiding the dialog first
                                        mNotMemberProgress.hide();
                                        //Showing error message when there is error
                                        Toast.makeText(NotMemberProfileActivity.this, "Could not retrieve request data. " + databaseError, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Function responsible for correctly allocating user base on accept percentage
    private void checkRequestStatus(){


                mCurrentUserRequestDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        long request_date = dataSnapshot.child("date").getValue(Long.class);

                        long current_date = System.currentTimeMillis();

                        long day_difference = TimeUnit.DAYS.convert(current_date - request_date, TimeUnit.MILLISECONDS);
                        if (day_difference >= 1 ){

                            //Getting accept and decline counts from the database
                            String decline_count = dataSnapshot.child("decline_count").getValue().toString();
                            String accept_count = dataSnapshot.child("accept_count").getValue().toString();
                            //Converting them into INT to perform operations
                            int decline_count_number = Integer.parseInt(decline_count);
                            int accept_count_number = Integer.parseInt(accept_count);

                            if (accept_count_number != 0 || decline_count_number !=0){
                                //Performing calculations
                                double percentage =  (accept_count_number*100/(accept_count_number + decline_count_number));

                                if (percentage >= 60.0) {

                                    FirebaseDatabase.getInstance().getReference().child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {

                                            String university = dataSnapshot.child("university").getValue().toString();
                                            String group_id = dataSnapshot.child("groupid").getValue().toString();


                                            //Joining the users to Members List

                                            FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Members").child(uid).child("date").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("group_status").setValue("Member").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {

                                                        }
                                                    });
                                                }
                                            });

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });




    }

}
