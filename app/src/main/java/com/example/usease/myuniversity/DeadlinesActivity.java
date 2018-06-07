package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.LinearLayout;
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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DeadlinesActivity extends AppCompatActivity {

    private DatabaseReference mUserDatabase, mDeadlinesDatabase;
    private FirebaseUser mCurrentUser;
    private Toolbar mToolbar;
    private RecyclerView mDeadlinesList;
    private LinearLayoutManager mLinearLayout;
    private Handler mHandler;
    private Spinner mSpinner;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deadlines);


        mDeadlinesList = (RecyclerView) findViewById(R.id.deadlines_list);
        mDeadlinesList.setHasFixedSize(true);
        mLinearLayout = new LinearLayoutManager(this);
        mDeadlinesList.setLayoutManager(mLinearLayout);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading Deadlines...");
        mProgressDialog.setMessage("Please wait until we load your deadlines.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mHandler = new Handler();
        mSpinner = (Spinner) findViewById(R.id.deadlines_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.Months, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.layout_for_spinner);
        // Apply the adapter to the spinner
        mSpinner.setAdapter(adapter);
        //Making the spinner to select the current month
        int month = Calendar.getInstance().get(Calendar.MONTH);
        mSpinner.setSelection(month);

        //Referencing toolbar and setting it
        mToolbar = (Toolbar) findViewById(R.id.deadlines_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.deadlines);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());


        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mProgressDialog.show();
                loadDeadlines();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    private void loadDeadlines() {

        mUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String university = dataSnapshot.child("university").getValue().toString();
                String groupid = dataSnapshot.child("groupid").getValue().toString();

                int selected_month = mSpinner.getSelectedItemPosition();
                String month = "";
                switch (selected_month){
                    case 0:
                        month = "January";
                        break;
                    case 1:
                        month = "February";
                        break;
                    case 2:
                        month = "March";
                        break;
                    case 3:
                        month = "April";
                        break;
                    case 4:
                        month = "May";
                        break;
                    case 5:
                        month = "June";
                        break;
                    case 6:
                        month = "July";
                        break;
                    case 7:
                        month = "August";
                        break;
                    case 8:
                        month = "September";
                        break;
                    case 9:
                        month = "October";
                        break;
                    case 10:
                        month = "November";
                        break;
                    case 11:
                        month = "December";
                        break;
                }
                mDeadlinesDatabase = FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupid).child("Deadlines").child(month);
                mDeadlinesDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {

                        final FirebaseRecyclerAdapter<Deadline, DeadlinesViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Deadline, DeadlinesViewHolder>(Deadline.class, R.layout.deadlines_single_layout, DeadlinesViewHolder.class, mDeadlinesDatabase) {
                            @Override
                            protected void populateViewHolder(final DeadlinesViewHolder viewHolder, final Deadline model, int position) {


                                final String deadline_id = getRef(position).getKey();

                                viewHolder.setTitle(model.getTitle());
                                viewHolder.setDeadlineText(model.getDate());
                                viewHolder.setDeadlineDate(model.getDate());
                                viewHolder.setDeadlineColor(model.getColor());

                                mDeadlinesDatabase.child(deadline_id).child("notification").addListenerForSingleValueEvent(new ValueEventListener() {
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



                                mHandler.postDelayed(viewHolder.runnable, 1000);


                                viewHolder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                String type = dataSnapshot.child("type").getValue().toString();

                                                if(type.equalsIgnoreCase("Student")) {

                                                    Toast.makeText(DeadlinesActivity.this, "Only CRs are able to delete the deadlines.", Toast.LENGTH_SHORT).show();

                                                } else {
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(DeadlinesActivity.this);

                                                    builder.setTitle("Deleting deadline")
                                                            .setMessage("Are you sure you want to delete this deadline?")
                                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    // continue with delete

                                                                    mDeadlinesDatabase.child(deadline_id).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            Toast.makeText(DeadlinesActivity.this, "Deleted the deadline: " + model.getTitle(), Toast.LENGTH_SHORT).show();
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
                                            Toast.makeText(DeadlinesActivity.this, "Notifications off for: " + model.getTitle(), Toast.LENGTH_SHORT).show();

                                            mDeadlinesDatabase.child(deadline_id).child("notification").child(mCurrentUser.getUid()).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    viewHolder.mNotificationButton.setEnabled(true);
                                                }
                                            });


                                        } else {
                                            viewHolder.mNotificationButton.setBackgroundResource(R.drawable.notification_icon_on);
                                            viewHolder.mNotificationButton.setTag("On");
                                            Toast.makeText(DeadlinesActivity.this, "Notifications on for: " + model.getTitle(), Toast.LENGTH_SHORT).show();
                                            mDeadlinesDatabase.child(deadline_id).child("notification").child(mCurrentUser.getUid()).setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
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
                                    mDeadlinesList.scrollToPosition(0);
                                }
                            }
                        });


                        mDeadlinesList.setAdapter(firebaseRecyclerAdapter);
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


    public static class DeadlinesViewHolder extends RecyclerView.ViewHolder {
        View mView;
        ImageButton mDeleteButton;
        ImageButton mNotificationButton;
        CountDownTimer mCountdown;
        Runnable runnable;
        Handler handler = new Handler();


        public DeadlinesViewHolder(final View itemView) {
            super(itemView);
            mView = itemView;
            mNotificationButton = (ImageButton) mView.findViewById(R.id.deadlines_single_notification_btn);
            mDeleteButton = (ImageButton) mView.findViewById(R.id.deadlines_single_edit_btn);

        }

        public void setTitle(String title) {
            TextView titleView = (TextView) mView.findViewById(R.id.deadlines_single_title);
            titleView.setText(title);
        }



        public void setDeadlineText(long deadline) {
            TextView dateView  = (TextView) mView.findViewById(R.id.deadlines_single_date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(deadline);
            final Date date = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
            String converted_date =  sdf.format(date);
            dateView.setText(converted_date);
        }

        public void setDeadlineDate(final long date){

            final TextView daysView = (TextView) mView.findViewById(R.id.deadlines_single_days);
            final TextView hoursView = (TextView) mView.findViewById(R.id.deadlines_single_hours);
            final TextView minutesView = (TextView) mView.findViewById(R.id.deadlines_single_minutes);
            final TextView secondsView = (TextView) mView.findViewById(R.id.deadlines_single_seconds);


            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            final long remainingTime = date - timestamp.getTime();

            if (remainingTime < 0 ){
                daysView.setText("0");
                hoursView.setText("0");
                secondsView.setText("0");
                minutesView.setText("0");

            } else {
                daysView.setText(""+ TimeUnit.DAYS.convert(remainingTime, TimeUnit.MILLISECONDS));
                hoursView.setText(""+ (TimeUnit.HOURS.convert(remainingTime, TimeUnit.MILLISECONDS)) % 24);
                minutesView.setText(""+ TimeUnit.MINUTES.convert(remainingTime, TimeUnit.MILLISECONDS)% 60);
                secondsView.setText(""+ TimeUnit.SECONDS.convert(remainingTime, TimeUnit.MILLISECONDS)%(60));
            }

            runnable = new Runnable() {
                @Override
                public void run() {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    final long remainingTime = date - timestamp.getTime();

                    if(remainingTime > 0) {

                        if(((TimeUnit.SECONDS.convert(remainingTime, TimeUnit.MILLISECONDS)%60)-1) == -1 ) {
                            secondsView.setText("59");

                            if(((TimeUnit.MINUTES.convert(remainingTime, TimeUnit.MILLISECONDS)%60)-1) == -1){
                                minutesView.setText("59");
                                if(((TimeUnit.HOURS.convert(remainingTime, TimeUnit.MILLISECONDS)%60)-1) == -1) {
                                    hoursView.setText("23");

                                    if(TimeUnit.DAYS.convert(remainingTime, TimeUnit.MILLISECONDS)-1 < 0) {
                                        daysView.setText("0");
                                    } else {
                                        daysView.setText(""+ (TimeUnit.DAYS.convert(remainingTime, TimeUnit.MILLISECONDS)-1));
                                    }
                                }else {
                                    hoursView.setText(""+ ((TimeUnit.HOURS.convert(remainingTime, TimeUnit.MILLISECONDS)%24)-1));
                                }
                            } else {
                                minutesView.setText(""+ ((TimeUnit.MINUTES.convert(remainingTime, TimeUnit.MILLISECONDS)%60)-1));
                            }
                        } else if (TimeUnit.DAYS.convert(remainingTime, TimeUnit.MILLISECONDS) == 0 && TimeUnit.HOURS.convert(remainingTime, TimeUnit.MILLISECONDS) == 0 && TimeUnit.MINUTES.convert(remainingTime, TimeUnit.MILLISECONDS) == 0){
                            secondsView.setText("0");
                        } else {
                            secondsView.setText(""+ ((TimeUnit.SECONDS.convert(remainingTime, TimeUnit.MILLISECONDS)%60)-1));
                        }

                        handler.postDelayed(this, 1000 );
                    } else {
                        daysView.setText("0");
                        hoursView.setText("0");
                        secondsView.setText("0");
                        minutesView.setText("0");
                    }

                }
            };

        }

        public void setDeadlineColor(int color) {

            View color_view =  mView.findViewById(R.id.deadlines_single_color);
            if(color ==0) {
                color_view.setBackgroundColor(Color.GREEN);
            } else {
                color_view.setBackgroundColor(color);
            }

        }
    }

    @Override
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
            startActivity(new Intent(DeadlinesActivity.this, AddDeadlineActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }


}
