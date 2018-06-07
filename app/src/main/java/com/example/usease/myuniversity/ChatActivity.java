package com.example.usease.myuniversity;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private String uid;
    private DatabaseReference mUserDatabase;
    private DatabaseReference mCurrentUserDatabase;
    private TextView mTitleView, mLastSeenView;
    private CircleImageView mProfileImage;
    private ImageButton mChatAddBtn, mChatSendBtn;
    private EditText mChatMessageView;
    private FirebaseUser mCurrentUser;
    private String mCurrentUserID;
    private RecyclerView mMessagesRecyclerView;
    private List<Messages> mMessagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;

    private static final int NUMBER_OF_MESSAGES_TO_LOAD = 10;
    private int mCurrentPage = 1;

    private int mItemPostition = 0;
    private String mLastMessageKey;
    private String mPreviousMessageKey;

    private static final int GALLERY_PICK = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Initializing and setting Toolbar
        mToolbar = (Toolbar) findViewById(R.id.chat_app_bar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar  = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        //We need layout inflator service since there is a need to inflate our custom action bar
        LayoutInflater inflator =  (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Inflating our custom layout
        View action_bar_view = inflator.inflate(R.layout.chat_custom_bar, null);
        //Finally setting it to our ActionBar
        actionBar.setCustomView(action_bar_view);

        //Getting current user, current user ID and reference to current user database
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserID = mCurrentUser.getUid();
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUserID);

        mChatAddBtn = (ImageButton) findViewById(R.id.chat_add_btn);
        mChatSendBtn = (ImageButton) findViewById(R.id.chat_send_btn);
        mChatMessageView = (EditText) findViewById(R.id.chat_message_view);
        mMessagesRecyclerView = (RecyclerView) findViewById(R.id.messages_list);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);
        mLinearLayout = new LinearLayoutManager(this);

        mAdapter = new MessageAdapter(mMessagesList);
        mMessagesRecyclerView.setHasFixedSize(true);
        mMessagesRecyclerView.setLayoutManager(mLinearLayout);
        mMessagesRecyclerView.setAdapter(mAdapter);

        loadMessages();

        //Getting uid
        uid = getIntent().getStringExtra("uid");
        //Getting reference to user database
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

        //-------------------------Loading User Data-----------------------------------------------------
        //Getting user data and setting it to views
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Setting user name as the title of the action bar
                String name = dataSnapshot.child("name").getValue(String.class);
                String online = dataSnapshot.child("online").getValue().toString();
                final String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                //Referencing view here, because if done anywhere else, views might not be accessible
                mTitleView = (TextView) findViewById(R.id.chat_custom_name);
                mLastSeenView = (TextView) findViewById(R.id.chat_custom_last_seen);
                mProfileImage = (CircleImageView) findViewById(R.id.chat_custom_thumb);

                mTitleView.setText(name);

                //Handling user last seen status
                if (online.equalsIgnoreCase("Online")) {
                    //if user is online, show him online
                    mLastSeenView.setText("Online");
                } else {
                    //Creating an instance of GetTimeAgo class. This class helps to calculate the last visit of the user
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastSeenTime = Long.parseLong(online); //Converting time from string to long
                    String lastSeenReady = getTimeAgo.getTimeAgo(lastSeenTime, getApplicationContext(), getResources().getConfiguration().locale); //Generating last seen
                    mLastSeenView.setText(lastSeenReady);
                }

                //Initially, we search image locally
                Picasso.with(getApplicationContext()).load(thumb_image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(mProfileImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        //If Picasso successfully finds the local cache of image, do nothing
                    }
                    @Override
                    public void onError() {
                        //else, that means we dont have the local image. Thus, there is a need to download
                        Picasso.with(getApplicationContext()).load(thumb_image).placeholder(R.drawable.default_avatar).into(mProfileImage);
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //-----------------------------------------------------------------------------------------------

        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send messages when user clicks send messages button
                sendMessages();
            }
        });

        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery_intent =  new Intent();
                gallery_intent.setType("image/*");
                gallery_intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(gallery_intent, "SELECT IMAGE TO SEND"), GALLERY_PICK);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                mItemPostition = 0;
                loadMessagesMore();

            }
        });
    }
    //Function is responsible for addtiional messages when user swipes
    private void loadMessagesMore() {

        uid = getIntent().getStringExtra("uid");
        //Getting reference to the messages database which holds messages between our current user and the selected user
        final DatabaseReference CurrentUserMessagesDatabase = mCurrentUserDatabase.child("messages").child(uid);
        final Query messageQuery =  CurrentUserMessagesDatabase.orderByKey().endAt(mLastMessageKey).limitToLast(NUMBER_OF_MESSAGES_TO_LOAD);
        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                //Getting each message as message object
                Messages message = dataSnapshot.getValue(Messages.class);

                CurrentUserMessagesDatabase.child(mLastMessageKey).child("seen").setValue(true);

                String messagKey = dataSnapshot.getKey();

                //For not duplicating messages, we check if current message key is not the same as previous message key
                if(!mPreviousMessageKey.equals(messagKey)) {
                    //If they are different, adding the message to messages list
                    mMessagesList.add(mItemPostition++, message);
                } else{
                    //If they are the same, setting previous message key to last message key
                    mPreviousMessageKey = mLastMessageKey;
                }

                //If all of the messages are loaded (this happens when item position index in adapter is 1)
                if(mItemPostition == 1) {
                    //we return the key of that last message
                    mLastMessageKey = dataSnapshot.getKey();
                }

                //Notifying adapter about changes
                mAdapter.notifyDataSetChanged();
                if(mItemPostition == 1) {
                    //If there has been 10 messages loaded, only after this scroll
                    mLinearLayout.scrollToPositionWithOffset(NUMBER_OF_MESSAGES_TO_LOAD-1,0 );
                } else {
                    //If there has not been loaded more than 10 messages, that means scrolling 10 messages is not meaningful
                    mLinearLayout.scrollToPositionWithOffset(mItemPostition,0 );
                }
                //mLinearLayout.scrollToPositionWithOffset(NUMBER_OF_MESSAGES_TO_LOAD-1,0 );
                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Function is responsible for loading messages
    private void loadMessages() {

        uid = getIntent().getStringExtra("uid");
        //Getting reference to the messages database which holds messages between our current user and the selected user
        DatabaseReference CurrentUserMessagesDatabase = mCurrentUserDatabase.child("messages").child(uid);
        Query messageQuery =  CurrentUserMessagesDatabase.limitToLast(mCurrentPage * NUMBER_OF_MESSAGES_TO_LOAD);


        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //Getting each message as message object
                Messages message = dataSnapshot.getValue(Messages.class);


                //Adding it to messages list
                mMessagesList.add(message);
                mItemPostition++;

                //If all of the messages are loaded (this happens when item position index in adapter is 1)
                if(mItemPostition == 1) {
                    //we return the key of that last message
                    mLastMessageKey = dataSnapshot.getKey();
                    mPreviousMessageKey = mLastMessageKey;
                }
                mCurrentUserDatabase.child("messages").child(uid).child(dataSnapshot.getKey()).child("seen").setValue(true);

                //Notifying adapter about changes
                mAdapter.notifyDataSetChanged();

                //Scrolling to last position when loading messages
                mMessagesRecyclerView.scrollToPosition(mMessagesList.size() - 1);

                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Function is responsible for sending messsages
    private void sendMessages() {
        //Getting user message
        String message = mChatMessageView.getText().toString();
        //Sending message only if message is non-empty
        if(!TextUtils.isEmpty(message.trim())) {

            //Clearing edit text
            mChatMessageView.getText().clear();

            //We need to create unique ID for each message. We can achieve it by using push()
            DatabaseReference user_message_push = mCurrentUserDatabase.child("messages").child(uid).push();
            String message_push_id = user_message_push.getKey();

            //Creating map to hold message data
            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("type", "text");
            messageMap.put("seen", false);
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserID);

            //Creating map to hold message data
            Map currentUserMessageMap = new HashMap();
            currentUserMessageMap.put("message", message);
            currentUserMessageMap.put("type", "text");
            currentUserMessageMap.put("seen", true);  //Changing only this variable since the user sees the message he sends
            currentUserMessageMap.put("time", ServerValue.TIMESTAMP);
            currentUserMessageMap.put("from", mCurrentUserID);

            String current_user_ref = "messages/" + uid;
            String user_ref = "messages/" + mCurrentUserID;

            //Specific message map for user
            Map messageUserMap = new HashMap();
            messageUserMap.put(user_ref + "/" + message_push_id, messageMap);

            //Specific message map for current user
            Map messageCurrentUserMap = new HashMap();
            messageCurrentUserMap.put(current_user_ref+ "/" + message_push_id, currentUserMessageMap);

            //Saving message to current user database
            mCurrentUserDatabase.updateChildren(messageCurrentUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                }
            });
            //Saving message to user database
            mUserDatabase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            //Getting selected image URI
            Uri image_uri = data.getData();

            final String current_user_ref = "messages/" + uid;
            final String user_ref = "messages/" + mCurrentUserID;

            //We need to create unique ID for each messasge. We can achieve it by using push()
            DatabaseReference user_message_push = mCurrentUserDatabase.child("messages").child(uid).push();
            final String message_push_id = user_message_push.getKey();

            StorageReference messageImageStorageRef = FirebaseStorage.getInstance().getReference().child("message_images").child(message_push_id + ".jpg");
            messageImageStorageRef.putFile(image_uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    String download_url = task.getResult().getDownloadUrl().toString();

                    //Creating map to hold message data
                    Map messageMap = new HashMap();
                    messageMap.put("message", download_url);
                    messageMap.put("type", "image");
                    messageMap.put("seen", false);
                    messageMap.put("time", ServerValue.TIMESTAMP);
                    messageMap.put("from", mCurrentUserID);


                    //Creating map to hold message data
                    Map currentUserMessageMap = new HashMap();
                    currentUserMessageMap.put("message", download_url);
                    currentUserMessageMap.put("type", "image");
                    currentUserMessageMap.put("seen", true); //User has already seen his photo
                    currentUserMessageMap.put("time", ServerValue.TIMESTAMP);
                    currentUserMessageMap.put("from", mCurrentUserID);

                    //Specific message map for user
                    Map messageUserMap = new HashMap();
                    messageUserMap.put(user_ref + "/" + message_push_id, messageMap);

                    //Specific message map for current user
                    Map messageCurrentUserMap = new HashMap();
                    messageCurrentUserMap.put(current_user_ref+ "/" + message_push_id, currentUserMessageMap);

                    mChatMessageView.setText("");

                    //Saving message to current user database
                    mCurrentUserDatabase.updateChildren(messageCurrentUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        }
                    });
                    //Saving message to user database
                    mUserDatabase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        }
                    });

                }
            });

        }
    }
}
