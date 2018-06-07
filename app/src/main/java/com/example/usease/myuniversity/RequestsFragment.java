package com.example.usease.myuniversity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private String mCurrentUID;
    private View mMainView;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mCurrentUserDatabase;
    private DatabaseReference mRequestsDatabase;
    private RecyclerView mRequestList;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView =  inflater.inflate(R.layout.fragment_requests, container, false);
        //Referencing RecyclerView
        mRequestList = (RecyclerView) mMainView.findViewById(R.id.requests_list);
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
        mRequestList.setHasFixedSize(true);
        //Setting layout manager for the RecyclerView
        final LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getContext());
        mRequestList.setLayoutManager(mLinearLayoutManager);

        readData(new MyCallback() {
            @Override
            public void onCallback(final DatabaseReference mRequestsDatabaseReference) {
                //When fragment is created, we create our adapter
                final FirebaseRecyclerAdapter<Groupmates, RequestsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Groupmates, RequestsViewHolder>(
                        Groupmates.class,
                        R.layout.request_users_single_layout,
                        RequestsViewHolder.class,
                        mRequestsDatabaseReference
                ) {
                    @Override
                    protected void populateViewHolder(final RequestsViewHolder requestsViewHolder, Groupmates model, int position) {
                        //We also need to know the ID of the each request. To achieve this, getRef() method helps us.
                        //Initially, we get request ID
                        final String requestID = getRef(position).getKey();
                        mRequestsDatabaseReference.child(requestID).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                //We also need to know the ID of the each user who made request. To achieve this, getRef() method helps us
                                final String uid  = dataSnapshot.child("uid").getValue().toString();
                                //Also we need date of the request
                                final String request_date  = dataSnapshot.child("date").getValue().toString();

                                mUsersDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot != null) {
                                            //Getting values from the cloud database
                                            String name = dataSnapshot.child("name").getValue().toString();
                                            String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();
                                            String type = dataSnapshot.child("type").getValue().toString();
                                            //In each iteration which sets data to each row of RecyclerView, we set the name, student type, request date and thumbnail
                                            requestsViewHolder.setName(name);
                                            requestsViewHolder.setStudentType(type);
                                            requestsViewHolder.setThumbImage(thumb_image, getContext());
                                            requestsViewHolder.setRequestDate(request_date);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });

                                //Setting onClickListener for each item in the RecyclerView
                                requestsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        //When an item is clicked from the list, that user's profile should be opened
                                        Intent not_member_intent = new Intent(getContext(), NotMemberProfileActivity.class);
                                        not_member_intent.putExtra("uid", uid);
                                        startActivity(not_member_intent);
                                    }
                                });
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {

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
                            mRequestList.scrollToPosition(positionStart);
                        }
                    }
                });

                //Finally, setting the ready adapter to the RecyclerView
                mRequestList.setAdapter(firebaseRecyclerAdapter);
            }
        });

        return mMainView;
    }

    //Inner class
    //Viewholder class for the adapter
    public static class RequestsViewHolder extends  RecyclerView.ViewHolder {
        //Creating a new view so that we can set values or onClickListeners for each RecyclerView row
        View mView;

        public RequestsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }
        //Function is responsible for setting name of the user
        public void setName(String name ) {
            TextView userName = (TextView) mView.findViewById(R.id.request_user_single_name);
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
        //Function is responsible for student type
        public void setStudentType(String type) {
            TextView userType = (TextView) mView.findViewById(R.id.requests_single_student_type);
            if(type.equalsIgnoreCase("Course Representative")) {
                userType.setText("Course Rep.");
            } else {
                userType.setText(type);
            }
        }

        public void setRequestDate (String requestDate) {

            TextView request_date_view  = (TextView) mView.findViewById(R.id.request_single_user_date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.parseLong(requestDate));
            Date date = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MMMM/yyyy HH:mm");
            String converted_date =  sdf.format(date);
            request_date_view.setText(converted_date);
        }
    }

    //Creating Callback since there might need to get reference to mRequestsDatabase again and again
    public void readData(final MyCallback myCallback) {
        mCurrentUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String university = dataSnapshot.child("university").getValue().toString();
                String groupid = dataSnapshot.child("groupid").getValue().toString();
                //Having acquired university and group ID, now we can get reference to group members list
                mRequestsDatabase = FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupid).child("Requests");
                myCallback.onCallback(mRequestsDatabase);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}
