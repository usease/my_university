package com.example.usease.myuniversity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class AnnouncementsActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mAnnouncementList;
    private LinearLayoutManager mLinearLayout;
    private DatabaseReference userDB, announcementDatabase;
    private FirebaseUser mCurrentUser;
    private String mCurrentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserID = mCurrentUser.getUid();

        mToolbar = (Toolbar) findViewById(R.id.announcement_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.announcements);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAnnouncementList = (RecyclerView) findViewById(R.id.announcements_announcement_list);
        mAnnouncementList.setHasFixedSize(true);
        mLinearLayout = new LinearLayoutManager(this);
        mLinearLayout.setReverseLayout(true);
        mLinearLayout.setStackFromEnd(true);
        mLinearLayout.setAutoMeasureEnabled(true);
        mAnnouncementList.setLayoutManager(mLinearLayout);

        userDB = FirebaseDatabase.getInstance().getReference().child("Users");
        userDB.keepSynced(true);

        userDB.child(mCurrentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String university = dataSnapshot.child("university").getValue().toString();
                announcementDatabase = FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Announcements");
                announcementDatabase.keepSynced(true);
                loadAnnouncements();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.announcements_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.add_announcement_icon) {
            startActivity(new Intent(AnnouncementsActivity.this, AddAnnouncementActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAnnouncements() {

        announcementDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                final FirebaseRecyclerAdapter<Announcement, AnnouncementViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Announcement, AnnouncementViewHolder>
                        (
                                Announcement.class,
                                R.layout.announcement_single_layout,
                                AnnouncementViewHolder.class,
                                announcementDatabase
                        ) {
                    @Override
                    protected void populateViewHolder(final AnnouncementViewHolder viewHolder, Announcement model, int position) {

                        final String announcement_key = getRef(position).getKey();
                        viewHolder.setTitle(model.getTitle());
                        viewHolder.setDate(model.getDate());
                        viewHolder.setLikes(model.getLikes());
                        viewHolder.setUserNameandImage(model.getUid(), getApplicationContext());
                        viewHolder.setImage(model.getImage(), getApplicationContext());
                        viewHolder.setType(model.getType(), getApplicationContext());
                        viewHolder.setCoverage(model.getCoverage(), getApplicationContext());


                        //=======================================================================================================
                        //Loading Like Status
                        announcementDatabase.child(announcement_key).child("like_uids").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                    viewHolder.mLikeButton.setImageResource(R.drawable.heart_icon_red);
                                    viewHolder.mLikeButton.setTag("liked");
                                } else {
                                    viewHolder.mLikeButton.setImageResource(R.drawable.heart_icon_gray);
                                    viewHolder.mLikeButton.setTag("unliked");
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        //=======================================================================================================
                        viewHolder.mLikeButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                if(!viewHolder.mLikeButton.getTag().equals("liked")){
                                    viewHolder.mLikeButton.setEnabled(false);

                                    announcementDatabase.child(announcement_key).child("like_uids").child(mCurrentUserID).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            viewHolder.mLikeButton.setImageResource(R.drawable.heart_icon_gray);
                                            viewHolder.mLikeButton.setTag("unliked");

                                            announcementDatabase.child(announcement_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    int likes = dataSnapshot.child("likes").getValue(Integer.class);
                                                    likes++;
                                                    announcementDatabase.child(announcement_key).child("likes").setValue(likes).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            //After everything is finished successfully, then enable the button again
                                                            viewHolder.mLikeButton.setEnabled(true);
                                                        }
                                                    });
                                                }
                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });

                                        }
                                    });

                                } else {
                                    viewHolder.mLikeButton.setEnabled(false);
                                    announcementDatabase.child(announcement_key).child("like_uids").child(mCurrentUserID).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            viewHolder.mLikeButton.setImageResource(R.drawable.heart_icon_red);
                                            viewHolder.mLikeButton.setTag("liked");

                                            announcementDatabase.child(announcement_key).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    int likes = dataSnapshot.child("likes").getValue(Integer.class);
                                                    likes--;
                                                    announcementDatabase.child(announcement_key).child("likes").setValue(likes).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            //After everything is finished successfully, then enable the button again
                                                            viewHolder.mLikeButton.setEnabled(true);
                                                        }
                                                    });

                                                }
                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                        //=============================================================================================================================================================

                    }
                };

                firebaseRecyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        super.onItemRangeInserted(positionStart, itemCount);

                        int groupmate_count = firebaseRecyclerAdapter.getItemCount();
                        int lastVisiblePosition = mLinearLayout.findLastVisibleItemPosition();
                        // If the recycler view is initially being loaded or the
                        // user is at the bottom of the list, scroll to the bottom
                        // of the list to show the newly added message.
                        if (lastVisiblePosition == -1 ||  (positionStart >= (groupmate_count - 1) && lastVisiblePosition == (positionStart - 1))) {
                            mAnnouncementList.scrollToPosition(positionStart);
                        }
                    }
                });


                mAnnouncementList.setAdapter(firebaseRecyclerAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        View mView;
        ImageButton mLikeButton;
        DatabaseReference userDB;

        public AnnouncementViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            mLikeButton = (ImageButton) itemView.findViewById(R.id.announcement_single_like_btn);
            userDB  = FirebaseDatabase.getInstance().getReference().child("Users");
            userDB.keepSynced(true);
        }       
        
        public void setTitle(String title){
            TextView titleView = (TextView) mView.findViewById(R.id.announcement_single_title);
            titleView.setText(title);
        }

        public void setLikes(int likes) {
            TextView likesView = (TextView) mView.findViewById(R.id.announcement_single_like_count);


                likesView.setText(" " + likes);

        }

        public void setDate(long announcement_date){
            TextView dateView  = (TextView) mView.findViewById(R.id.announcement_single_date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(announcement_date);
            Date date = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
            String converted_date =  sdf.format(date);
            dateView.setText(converted_date);
        }

        public void setUserNameandImage(String uid, final Context ctx) {


            userDB.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String name = dataSnapshot.child("name").getValue().toString();
                    final String userImage = dataSnapshot.child("thumb_image").getValue().toString();

                    TextView nameView = (TextView) mView.findViewById(R.id.announcement_single_name);
                    final CircleImageView userCircleImage = (CircleImageView) mView.findViewById(R.id.announcement_single_user_image);

                    nameView.setText(name);

                    Picasso.with(ctx).load(userImage).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(userCircleImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            //If Picasso successfully finds the local cache of image, do nothing
                        }
                        @Override
                        public void onError() {
                            //else, that means we dont have the local image. Thus, there is a need to download
                            Picasso.with(ctx).load(userImage).placeholder(R.drawable.default_avatar).into(userCircleImage);
                        }
                    });

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void setImage(final String image, final Context ctx) {

            final ImageView userImageView = (ImageView) mView.findViewById(R.id.announcement_single_image);
            //Initially, we search image locally
            Picasso.with(ctx).load(image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_announcement).into(userImageView, new Callback() {
                @Override
                public void onSuccess() {
                    //If Picasso successfully finds the local cache of image, do nothing
                }
                @Override
                public void onError() {
                    //else, that means we dont have the local image. Thus, there is a need to download
                    Picasso.with(ctx).load(image).placeholder(R.drawable.default_announcement).into(userImageView);
                }
            });
        }

        public void setType(String type, Context ctx) {
            ImageView imageView = (ImageView) mView.findViewById(R.id.announcement_single_warning_image);
            TextView typeView = (TextView) mView.findViewById(R.id.announcement_single_type);

            if(type.equalsIgnoreCase("Information")) {
                imageView.setImageResource(R.drawable.warning_icon_information);
                typeView.setTextColor(ctx.getResources().getColor(R.color.colorGreen));
                typeView.setText(R.string.information);
            } else if (type.equalsIgnoreCase("News")) {
                imageView.setImageResource(R.drawable.warning_icon_accent);
                typeView.setTextColor(ctx.getResources().getColor(R.color.colorAccent));
                typeView.setText(R.string.news);
            }
            else {
                imageView.setImageResource(R.drawable.warning_icon_warning);
                typeView.setTextColor(ctx.getResources().getColor(R.color.colorRed));
                typeView.setText(R.string.warning);
            }
        }


        public void setCoverage(String coverage, Context ctx) {

            ImageView imageView = (ImageView) mView.findViewById(R.id.announcement_single_coverage_icon);
            TextView coverageView = (TextView) mView.findViewById(R.id.announcement_single_coverage_text);

            if(coverage.equalsIgnoreCase("University")) {
                imageView.setImageResource(R.drawable.university_icon);
                coverageView.setTextColor(ctx.getResources().getColor(R.color.colorAccent));
                coverageView.setText(R.string.select_university);
            }
             else {
                imageView.setImageResource(R.drawable.group_icon);
                coverageView.setTextColor(ctx.getResources().getColor(R.color.colorGreen));
                coverageView.setText(R.string.my_group_only);

            }
        }


    }
}
