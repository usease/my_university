package com.example.usease.myuniversity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupmatesFragment extends Fragment {

    private RecyclerView mGroupmatesList;
    private DatabaseReference mMembersDatabase;
    private FirebaseAuth mAuth;
    private String mCurrentUID;
    private View mMainView;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mCurrentUserDatabase;

    public GroupmatesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment and save it as a view
        mMainView =  inflater.inflate(R.layout.fragment_groupmates, container, false);
        //Referencing RecyclerView
        mGroupmatesList = (RecyclerView) mMainView.findViewById(R.id.groupmates_list);
        //Referencing FirebaseAuth and getting current user ID
        mAuth = FirebaseAuth.getInstance();
        mCurrentUID = mAuth.getCurrentUser().getUid();
        //There is also need for database reference from which we can get group members list
        //At first, we get user university and group ID
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUID);
        //Storing data locally
        mCurrentUserDatabase.keepSynced(true);

        deleteOldRequests();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        //Storing data locally
        mUsersDatabase.keepSynced(true);
        //For performance improvement, setting fixed size for the RecyclerView
        mGroupmatesList.setHasFixedSize(true);
        //Setting layout manager for the RecyclerView
        final LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getContext());
        mGroupmatesList.setLayoutManager(mLinearLayoutManager);

        //Calling Callback function to get ref to mMembersDatabase
        readData(new MyCallback() {
            @Override
            public void onCallback(DatabaseReference databaseReference) {

                //When fragment is created, we create our adapter
                final FirebaseRecyclerAdapter<Groupmates, GroupmatesViewHolder> firebaseRecyclerAdapter =
                        new FirebaseRecyclerAdapter <Groupmates, GroupmatesViewHolder> (
                                Groupmates.class,
                                R.layout.users_single_layout,
                                GroupmatesViewHolder.class,
                                databaseReference
                        ) {

                            @Override
                            protected  void populateViewHolder(final GroupmatesViewHolder groupmatesViewHolder, final Groupmates groupmates, final int position) {

                                //We also need to know the ID of the each groupmate. To achieve this, getRef() method helps us
                                final String uid = getRef(position).getKey();


                                mUsersDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if(dataSnapshot != null) {
                                            //Getting values from the cloud database
                                            String name = dataSnapshot.child("name").getValue().toString();
                                            String status = dataSnapshot.child("status").getValue().toString();
                                            String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();
                                            String online = dataSnapshot.child("online").getValue().toString();

                                            //In each iteration which sets data to each row of RecyclerView, we set the name, status, online status and thumbnail
                                            groupmatesViewHolder.setName(name);
                                            groupmatesViewHolder.setStatus(status);
                                            groupmatesViewHolder.setUserOnline(online);
                                            //Since we use Picasso to load image and it requires context, we also need to pass the context as a parameter
                                            groupmatesViewHolder.setThumbImage(thumb_image, getContext());
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                                //Setting onClickListener for each item in the RecyclerView
                                groupmatesViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        //If the clicked child is the profile of the user itself
                                        if(uid.equals(mCurrentUID)) {
                                            Intent edit_profile_intent = new Intent(getContext(), SettingsActivity.class );
                                            startActivity(edit_profile_intent);
                                        } else {
                                            //In other cases, when an item is clicked from the list, that user's profile should be opened
                                            Intent profile_intent = new Intent(getContext(), ProfileActivity.class);
                                            profile_intent.putExtra("uid", uid);
                                            startActivity(profile_intent);
                                        }

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
                            mGroupmatesList.scrollToPosition(positionStart);
                        }
                    }
                });

                //Finally, setting the ready adapter to the RecyclerView
                mGroupmatesList.setAdapter(firebaseRecyclerAdapter);
            }
        });
        //Finally, returning ready view
        return mMainView;
    }

    private void deleteOldRequests() {
        mCurrentUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String university = dataSnapshot.child("university").getValue().toString();
                final String group_id = dataSnapshot.child("groupid").getValue().toString();
                FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Requests").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                            for (final DataSnapshot childSnapshot: dataSnapshot.getChildren()) {

                                long request_date = childSnapshot.child("date").getValue(Long.class);
                                long current_date = System.currentTimeMillis();
                                long day_difference = TimeUnit.DAYS.convert(current_date - request_date, TimeUnit.MILLISECONDS);
                                if (day_difference >= 1 ){

                                    //Getting accept and decline counts from the database
                                    String decline_count = childSnapshot.child("decline_count").getValue().toString();
                                    String accept_count = childSnapshot.child("accept_count").getValue().toString();
                                    final String uid = childSnapshot.child("uid").getValue().toString();
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

                                                    final String university = dataSnapshot.child("university").getValue().toString();
                                                    final String group_id = dataSnapshot.child("groupid").getValue().toString();


                                                    //Joining the users to Members List

                                                    FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Members").child(uid).child("date").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {

                                                            FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("group_status").setValue("Member").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    //Deleting the request after allocating
                                                                    FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Requests").child(childSnapshot.getKey()).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                        }
                                                                    });
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

    //Inner class
    //Viewholder class for the adapter
    public static class GroupmatesViewHolder extends RecyclerView.ViewHolder {

        //Creating a new view so that we can set values or onClickListeners for each RecyclerView row
        View mView;

        public GroupmatesViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }
        //Function is responsible for setting name of the user
        public void setName(String name ) {
            TextView userName = (TextView) mView.findViewById(R.id.user_single_name);
            userName.setText(name);
        }

        //Function is responsible for setting status of the user
        public void setStatus(String status) {
            TextView userStatus = (TextView) mView.findViewById(R.id.request_single_user_date);
            userStatus.setText(status);
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

        //Function is responsible for setting online icon of the user
        public void setUserOnline(String online) {
            ImageView online_icon = (ImageView) mView.findViewById(R.id.user_single_online_icon);
            //If user is online
            if(online.equalsIgnoreCase("Online")) {
                //Showing little online icon
                online_icon.setVisibility(View.VISIBLE);
            } else {
                //Otherwise, hiding the icon
                online_icon.setVisibility(View.INVISIBLE);
            }
        }
    }

    //Creating Callback since there might need to get reference to mMembersDatabase again and again
    public void readData(final MyCallback myCallback) {
        mCurrentUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String university = dataSnapshot.child("university").getValue().toString();
                String groupid = dataSnapshot.child("groupid").getValue().toString();
                //Having acquired university and group ID, now we can get reference to group members list
                mMembersDatabase = FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupid).child("Members");
                myCallback.onCallback(mMembersDatabase);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}
