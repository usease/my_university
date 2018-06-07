package com.example.usease.myuniversity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    private RecyclerView mConversationList;
    private FirebaseAuth mAuth;
    private String mCurrentUID;
    private View mMainView;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mCurrentUserDatabase;
    final LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getContext());;


    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment and save it as a view
        mMainView =  inflater.inflate(R.layout.fragment_chats, container, false);
        //Referencing RecyclerView
        mConversationList = (RecyclerView) mMainView.findViewById(R.id.conversation_list);
        //Referencing FirebaseAuth and getting current user ID
        mAuth = FirebaseAuth.getInstance();
        mCurrentUID = mAuth.getCurrentUser().getUid();
        //There is also need for database reference from which we can get group members list
        //At first, we get user university and group ID
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUID);
        //Storing data locally
        mCurrentUserDatabase.keepSynced(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        //Storing data locally
        mUsersDatabase.keepSynced(true);

        //For performance improvement, setting fixed size for the RecyclerView
        mConversationList.setHasFixedSize(true);
        //Setting layout manager for the RecyclerView
        mConversationList.setLayoutManager(mLinearLayoutManager);

        //Finally, returning ready view
        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query conversationQuery = mCurrentUserDatabase.child("messages").orderByChild("time");

        final FirebaseRecyclerAdapter<Conversation, ConversationViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Conversation, ConversationViewHolder>(
                Conversation.class,
                R.layout.users_single_layout,
                ConversationViewHolder.class,
                conversationQuery
        ) {
            @Override
            protected void populateViewHolder(final ConversationViewHolder viewHolder, final Conversation conversation, int position) {

                //We also need to know the ID of the each user ID. To achieve this, getRef() method helps us
                final String uid = getRef(position).getKey();
                //Getting the last message from the user
                Query lastMessageQuery = mCurrentUserDatabase.child("messages").child(uid).limitToLast(1);

                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if(dataSnapshot !=null) {
                            String message = dataSnapshot.child("message").getValue().toString();
                            String from = dataSnapshot.child("from").getValue().toString();
                            String type = dataSnapshot.child("type").getValue().toString();
                            boolean seen = dataSnapshot.child("seen").getValue(Boolean.class);

                            viewHolder.setMessage(message, seen, from, mCurrentUID, type);
                            mUsersDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String name = dataSnapshot.child("name").getValue().toString();
                                    String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();
                                    viewHolder.setName(name);
                                    viewHolder.setThumbImage(thumb_image, getContext());
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

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


                //Setting onClickListener for each item in the RecyclerView
                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                           //Directing user to ChatActivity
                            Intent profile_intent = new Intent(getContext(), ChatActivity.class);
                            profile_intent.putExtra("uid", uid);
                            startActivity(profile_intent);
                    }
                });


            }



        };

        firebaseRecyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int groupmate_count = firebaseRecyclerAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||  (positionStart >= (groupmate_count - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mConversationList.scrollToPosition(positionStart);
                }
            }
        });
        //Finally, setting the ready adapter to the RecyclerView
        mConversationList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {

        //Creating a new view so that we can set values or onClickListeners for each RecyclerView row
        View mView;
        public ConversationViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }


        public void setMessage(String message, boolean seen, String from, String mCurrenUID, String type) {
            TextView userStatusView = (TextView) mView.findViewById(R.id.request_single_user_date);


            if(from.equals(mCurrenUID)) {
                    //if the message is coming from us that means there no need for styling
            } else {
                if(!seen) {
                    userStatusView.setTextColor(Color.parseColor("#ff5521"));
                    userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
                } else {
                    userStatusView.setTextColor(Color.GRAY);
                    userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
                }
            }

            //If the message is photo
            if(type.equals("image")) {
                userStatusView.setText("Photo");
            } else {
                //if it is just a text
                userStatusView.setText(message);
            }



        }

        //Function is responsible for setting name of the user
        public void setName(String name ) {
            TextView userName = (TextView) mView.findViewById(R.id.user_single_name);
            userName.setText(name);
        }

        //Function is responsible for setting thumb image of the user
        public void setThumbImage(final String thumbImage, final Context ctx) {
            final CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.request_user_single_image);
            //Initially, we search image locally
            Picasso.with(ctx).load(thumbImage).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(userImageView, new Callback() {
                @Override
                public void onSuccess() {
                    //If Picasso successfully finds the local cache of image, do nothing
                }
                @Override
                public void onError() {
                    //else, that means we dont have the local image. Thus, there is a need to download
                    Picasso.with(ctx).load(thumbImage).placeholder(R.drawable.default_avatar).into(userImageView);
                }
            });
        }
    }
}
