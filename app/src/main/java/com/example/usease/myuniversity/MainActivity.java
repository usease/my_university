package com.example.usease.myuniversity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.kunzisoft.switchdatetime.time.widget.CircleView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {

    private LinearLayout mTimetableBtn, mAnnouncementsBtn, mDeadlinesBtn, mChatsBtn;
    private Toolbar mToolbar;

    private FirebaseAuth mAuth;
    private DatabaseReference mCurrentUserDatabase;
    private FirebaseUser mCurrentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Referencing toolbar and setting it
        mToolbar = (Toolbar) findViewById(R.id.main_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("My University");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Initializing FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        //Reference to buttons
        mTimetableBtn = (LinearLayout) findViewById(R.id.timtable_btn);
        mAnnouncementsBtn = (LinearLayout) findViewById(R.id.annnouncements_btn);
        mDeadlinesBtn = (LinearLayout) findViewById(R.id.deadlines_btn);
        mChatsBtn = (LinearLayout) findViewById(R.id.chat_btn);




        //Getting reference to current user database
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mAuth = FirebaseAuth.getInstance();

        if (mCurrentUser != null) {
            //Delete old requests
            handleRequestStatus();
            checkUserMemberStatus();
        }



        if(mAuth.getCurrentUser() != null) {



        } else {
            resendToStart();
        }

        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {


                final TextView userNameView = (TextView) findViewById(R.id.nav_user_name);
                final TextView groupIDView = (TextView) findViewById(R.id.nav_groupid);
                final TextView groupStatusView = (TextView) findViewById(R.id.nav_group_status);
                final CircleImageView userImageView = (CircleImageView) findViewById(R.id.nav_user_thumb_image);

                mCurrentUserDatabase.child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String name = dataSnapshot.child("name").getValue().toString();
                        String groupID = dataSnapshot.child("groupid").getValue().toString();
                        String group_status = dataSnapshot.child("group_status").getValue().toString();
                        final String thumb_image_url = dataSnapshot.child("thumb_image").getValue().toString();

                        userNameView.setText(name);
                        groupStatusView.setText( group_status);
                        groupIDView.setText(groupID);

                        if(getResources().getConfiguration().locale == Locale.US){
                            groupStatusView.setText( group_status);
                            groupIDView.setText(groupID);
                        } else {

                            if (group_status.equalsIgnoreCase("Member")) {
                                groupStatusView.setText("Guruh A\'zosi");
                            } else {
                                groupStatusView.setText("A\'zo Emas");
                            }
                        }

                        //Initially, we search image locally
                        Picasso.with(MainActivity.this).load(thumb_image_url).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(userImageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                //If Picasso successfully finds the local cache of image, do nothing
                            }
                            @Override
                            public void onError() {
                                //else, that means we dont have the local image. Thus, there is a need to download
                                Picasso.with(MainActivity.this).load(thumb_image_url).placeholder(R.drawable.default_avatar).into(userImageView);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    private void checkUserMemberStatus() {
        if(mCurrentUser != null) {

            //Disabling sections if user is not member or doing vice verse
            mCurrentUserDatabase.child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String group_status = dataSnapshot.child("group_status").getValue().toString();
                    String university = dataSnapshot.child("university").getValue().toString();
                    String group_id = dataSnapshot.child("groupid").getValue().toString();

                    if (!group_status.equalsIgnoreCase("Member")) {

                        mTimetableBtn.setAlpha(0.8f);
                        mDeadlinesBtn.setAlpha(0.8f);
                        mAnnouncementsBtn.setAlpha(0.8f);
                        mChatsBtn.setAlpha(0.8f);

                        mAnnouncementsBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "Only group members can access this section!", Toast.LENGTH_SHORT).show();
                            }
                        });

                        mDeadlinesBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "Only group members can access this section!", Toast.LENGTH_SHORT).show();
                            }
                        });

                        mTimetableBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "Only group members can access this section!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        mChatsBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(MainActivity.this, "Only group members can access this section!", Toast.LENGTH_SHORT).show();
                            }
                        });


                        FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Requests").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {

                                    String uid = childSnapshot.child("uid").getValue().toString();
                                    if (uid.equals(mCurrentUser.getUid())) {

                                        //Getting accept and decline counts from the database
                                        String decline_count = childSnapshot.child("decline_count").getValue().toString();
                                        String accept_count = childSnapshot.child("accept_count").getValue().toString();
                                        long request_date = childSnapshot.child("date").getValue(Long.class);
                                        long current_date = System.currentTimeMillis();
                                        //Converting them into INT to perform operations
                                        int decline_count_number = Integer.parseInt(decline_count);
                                        int accept_count_number = Integer.parseInt(accept_count);

                                        long secs = (current_date - request_date) / 1000;
                                        long hours = secs / 3600;
                                        hours = 24-hours;

                                        double percentage = 0.00;
                                        if (accept_count_number != 0 || decline_count_number !=0) {
                                            //Performing calculations
                                            percentage = (accept_count_number * 100 / (accept_count_number + decline_count_number));
                                        }


                                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                                        alertDialog.setTitle("Group Join Status");
                                        alertDialog.setMessage("You have not been allocated to your group yet. Please wait until your groupmates finish voting. You cant not access most of the application sections currently. \n \nYou have:  " + hours +" hours left. \nAccept Count: " + accept_count + " \nDecline Count: "+ decline_count+ "\nAccept Percentage: " + percentage + " %");
                                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                });
                                        alertDialog.show();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    } else {

                        mTimetableBtn.setAlpha(1f);
                        mDeadlinesBtn.setAlpha(1f);
                        mAnnouncementsBtn.setAlpha(1f);
                        mChatsBtn.setAlpha(1f);

                        mChatsBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent chat_intent = new Intent(MainActivity.this, ChatsActivity.class);
                                startActivity(chat_intent);
                            }
                        });

                        mTimetableBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent timetable_intent = new Intent(MainActivity.this, TimetableActivity.class);
                                startActivity(timetable_intent);
                            }
                        });

                        mDeadlinesBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent deadlines_intent = new Intent(MainActivity.this, DeadlinesActivity.class);
                                startActivity(deadlines_intent);
                            }
                        });
                        mAnnouncementsBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent announcement_intent = new Intent(MainActivity.this, AnnouncementsActivity.class);
                                startActivity(announcement_intent);
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        //Check if the user signed in (non-null)
        //If the user is not signed in, then redirect to StartActivity
        if(mCurrentUser == null) {
            //Resend the user to StartActivity if the user is not authorized

            resendToStart();
        } else {
            //If the user is authenticated
            //We set the user online
            mCurrentUserDatabase.child(mCurrentUser.getUid()).child("online").setValue("Online");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mCurrentUser != null) {
            //When activity is stopped or minimized, that means user is not online anymore
            mCurrentUserDatabase.child(mCurrentUser.getUid()).child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

    //Function responsible for redirecting user to Start Activity
    private void resendToStart() {
        Intent start_intent = new Intent (MainActivity.this, StartActivity.class);
        startActivity(start_intent);
        //Finish the current activity so that user can not come back by pressing the back button
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //Inflating our menu
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        //if log out menu item is selected
        if(item.getItemId() == R.id.main_logout_btn) {
            //Signing user out
            FirebaseAuth.getInstance().signOut();
            //After signing out, we need to update UI. We need to send the user to StartActivity again
            resendToStart();
        }
        if(item.getItemId() == R.id.main_account_settings_btn) {
            //When AccountSettings selected, go to SettingsActivity
            Intent settings_intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settings_intent);
        }
        return true;
    }



    @Override
    public void onBackPressed() {


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();
        mCurrentUserDatabase.child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String group_status = dataSnapshot.child("group_status").getValue().toString();

                if (!group_status.equalsIgnoreCase("Member")) {

                    if(id == R.id.nav_timetable || id == R.id.nav_announcements || id == R.id.nav_deadlines || id == R.id.nav_chats) {
                        Toast.makeText(MainActivity.this, "Only group members can access this section! ", Toast.LENGTH_SHORT).show();
                    }


                } else {

                    if (id == R.id.nav_timetable) {
                        Intent intent = new Intent(MainActivity.this, TimetableActivity.class);
                        startActivity(intent);
                    } else if (id == R.id.nav_deadlines) {
                        Intent intent = new Intent(MainActivity.this, DeadlinesActivity.class);
                        startActivity(intent);

                    } else if (id == R.id.nav_announcements) {

                        Intent intent = new Intent(MainActivity.this, AnnouncementsActivity.class);
                        startActivity(intent);

                    } else if (id == R.id.nav_chats) {

                        Intent intent = new Intent(MainActivity.this, ChatsActivity.class);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (id == R.id.nav_account_settings) {

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_logout) {
            resendToStart();
        }
        else if (id == R.id.nav_quit) {
            this.finishAffinity();
        } else if (id == R.id.nav_manual) {
            Intent intent = new Intent(MainActivity.this, UsersManualActivity.class);
            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleRequestStatus() {

        mCurrentUserDatabase.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String university = dataSnapshot.child("university").getValue().toString();
                final String group_id = dataSnapshot.child("groupid").getValue().toString();
                FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Requests").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (final DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                            String current_user_id = childSnapshot.child("uid").getValue().toString();

                            if (current_user_id.equals(mAuth.getCurrentUser().getUid())) {

                                long request_date = childSnapshot.child("date").getValue(Long.class);
                                long current_date = System.currentTimeMillis();
                                //long day_difference = TimeUnit.DAYS.convert(current_date - request_date, TimeUnit.MILLISECONDS);
                                long secs = (current_date - request_date) / 1000;
                                long hours = secs / 3600;
                                //hours = 24-hours;
                                if (hours > 23){
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
                                        } else {
                                            FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Requests").child(childSnapshot.getKey()).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                }
                                            });
                                        }
                                    }

                                } else {
//                                    FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(group_id).child("Requests").child(childSnapshot.getKey()).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
//                                        @Override
//                                        public void onComplete(@NonNull Task<Void> task) {
//
//                                        }
//                                    });
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

    @Override
    protected void onRestart() {
        if (mCurrentUser != null) {
            //Delete old requests
            handleRequestStatus();
            checkUserMemberStatus();
        }
        super.onRestart();
    }
}
