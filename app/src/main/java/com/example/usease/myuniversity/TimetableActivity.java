package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
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

import java.sql.Time;
import java.util.Calendar;

public class TimetableActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mUserDatabase, mTimetableDatabase;

    private RecyclerView mTimetableList;
    private LinearLayoutManager mLinearLayout;
    private ProgressDialog mProgressDialog;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);



        mTimetableList = (RecyclerView) findViewById(R.id.timetable_list);
        mTimetableList.setHasFixedSize(true);
        mLinearLayout = new LinearLayoutManager(this);
        mTimetableList.setLayoutManager(mLinearLayout);

        //Referencing toolbar and setting it
        mToolbar = (Toolbar) findViewById(R.id.timetable_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.timetable);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading Timetable...");
        mProgressDialog.setMessage("Please wait until we load your modules.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());


        mSpinner = (Spinner) findViewById(R.id.timetable_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.Weekdays, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.layout_for_spinner);
        // Apply the adapter to the spinner
        mSpinner.setAdapter(adapter);
        //Making the spinner to select the current month
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        //Since there is difference between array indexing and returned day of the week, we need to subtract 2 from the day        day = day-2;

        day = day -2;
        mSpinner.setSelection(day);


        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mProgressDialog.show();
                loadTimetable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }

    private void loadTimetable() {

        mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String university = dataSnapshot.child("university").getValue().toString();
                String groupid = dataSnapshot.child("groupid").getValue().toString();

                int selected_month = mSpinner.getSelectedItemPosition();
                String day = "";
                switch (selected_month){
                    case 0:
                        day = "Monday";
                        break;
                    case 1:
                        day = "Tuesday";
                        break;
                    case 2:
                        day = "Wednesday";
                        break;
                    case 3:
                        day = "Thursday";
                        break;
                    case 4:
                        day = "Friday";
                        break;
                    case 5:
                        day = "Saturday";
                        break;
                    case 6:
                        day = "Sunday";
                        break;

                }
                mTimetableDatabase = FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupid).child("Timetable").child(day);
                mTimetableDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final FirebaseRecyclerAdapter<Timetable, TimetableViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Timetable, TimetableViewHolder>(Timetable.class, R.layout.timetable_single_layout, TimetableViewHolder.class, mTimetableDatabase) {

                            @Override
                            protected void populateViewHolder(final TimetableViewHolder viewHolder, final Timetable model, int position) {

                                final String lesson_id = getRef(position).getKey();

                                viewHolder.setTime(model.getTime());
                                viewHolder.setModuleColor(model.getColor());
                                viewHolder.setName(model.getName());
                                viewHolder.setRoom(model.getRoom());
                                viewHolder.setTeacher(model.getTeacher());
                                viewHolder.setType(model.getType());

                                mTimetableDatabase.child(lesson_id).child("notification").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild(mCurrentUser.getUid())) {

                                            viewHolder.mNotificationButton.setTag("On");
                                            viewHolder.mNotificationButton.setBackgroundResource(R.drawable.notification_icon_on);


                                        } else {
                                            viewHolder.mNotificationButton.setTag("Off");
                                            viewHolder.mNotificationButton.setBackgroundResource(R.drawable.notification_icon_off);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                                viewHolder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                String type = dataSnapshot.child("type").getValue().toString();

                                                if(type.equalsIgnoreCase("Student")) {

                                                    Toast.makeText(TimetableActivity.this, "Only CRs are able to delete the lessons.", Toast.LENGTH_SHORT).show();

                                                } else {

                                                    AlertDialog.Builder builder = new AlertDialog.Builder(TimetableActivity.this);

                                                    builder.setTitle("Deleting lesson")
                                                            .setMessage("Are you sure you want to delete this lesson?")
                                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    // continue with delete

                                                                    mTimetableDatabase.child(lesson_id).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            Toast.makeText(TimetableActivity.this, "Deleted the lesson: " + model.getName(), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });

                                                                }
                                                            })
                                                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    // do nothing
                                                                }
                                                            })
                                                            .setIcon(R.drawable.delete_icon)
                                                            .show();

                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });



                                    }
                                });


                                viewHolder.mNotificationButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        viewHolder.mNotificationButton.setEnabled(false);

                                        if(viewHolder.mNotificationButton.getTag().equals("On"))    {
                                            viewHolder.mNotificationButton.setBackgroundResource(R.drawable.notification_icon_off);
                                            viewHolder.mNotificationButton.setTag("Off");
                                            Toast.makeText(TimetableActivity.this, "Notifications off for: " + model.getName(), Toast.LENGTH_SHORT).show();

                                            mTimetableDatabase.child(lesson_id).child("notification").child(mCurrentUser.getUid()).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    viewHolder.mNotificationButton.setEnabled(true);
                                                }
                                            });


                                        } else {
                                            viewHolder.mNotificationButton.setBackgroundResource(R.drawable.notification_icon_on);
                                            viewHolder.mNotificationButton.setTag("On");
                                            Toast.makeText(TimetableActivity.this, "Notifications on for: " + model.getName(), Toast.LENGTH_SHORT).show();
                                            mTimetableDatabase.child(lesson_id).child("notification").child(mCurrentUser.getUid()).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    viewHolder.mNotificationButton.setEnabled(true);
                                                }
                                            });

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
                                int lastVisiblePosition = mLinearLayout.findLastVisibleItemPosition();
                                // If the recycler view is initially being loaded or the
                                // user is at the bottom of the list, scroll to the bottom
                                // of the list to show the newly added message.
                                if (lastVisiblePosition == -1 ||  (positionStart >= (groupmate_count - 1) && lastVisiblePosition == (positionStart - 1))) {
                                    mTimetableList.scrollToPosition(0);
                                }
                            }
                        });


                        mTimetableList.setAdapter(firebaseRecyclerAdapter);
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



        mProgressDialog.dismiss();
    }


    public static class TimetableViewHolder extends RecyclerView.ViewHolder {

        View mView;
        ImageButton mDeleteButton;
        ImageButton mNotificationButton;

        public TimetableViewHolder(View itemView) {

            super(itemView);
            mView = itemView;
            mNotificationButton = (ImageButton) mView.findViewById(R.id.timetable_single_notification_btn);
            mDeleteButton = (ImageButton) mView.findViewById(R.id.timetable_single_edit_btn);

        }

        public void setName(String name) {
            TextView nameView = (TextView) mView.findViewById(R.id.timetable_module_name);
            nameView.setText(name);
        }

        public void setTime(String time) {
            TextView timeView = (TextView) mView.findViewById(R.id.timetable_time);
            timeView.setText(time);


        }

        public void setRoom(String room) {
            TextView roomView = (TextView) mView.findViewById(R.id.timetable_room);
            roomView.setText(room);
        }

        public void setTeacher(String teacher) {
            TextView teacherView = (TextView) mView.findViewById(R.id.timetable_teacher);
            teacherView.setText(teacher);
        }

        public void setType(String type) {
            TextView typeView = (TextView) mView.findViewById(R.id.timetable_type);
            typeView.setText(type);
        }


        public void setModuleColor(int color) {

            View color_view =  mView.findViewById(R.id.timetable_single_color);
            if(color ==0) {
                color_view.setBackgroundColor(Color.GREEN);
            } else {
                color_view.setBackgroundColor(color);
            }

        }

    }

    public boolean onCreateOptionsMenu(final Menu menu) {

        FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String type =  dataSnapshot.child("type").getValue().toString();

                if(!type.equalsIgnoreCase("Student")) {
                    getMenuInflater().inflate(R.menu.announcements_menu, menu);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.add_announcement_icon) {
            startActivity(new Intent(TimetableActivity.this, AddTimetableActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
