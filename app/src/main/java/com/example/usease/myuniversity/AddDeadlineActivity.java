package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.kunzisoft.switchdatetime.SwitchDateTimeDialogFragment;
import com.kunzisoft.switchdatetime.time.widget.CircleView;
import com.madrapps.pikolo.HSLColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class AddDeadlineActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private Button mDatePickBtn;
    private TextView mSelectedDateText;
    private DatabaseReference mCurrentUserDatabase;
    private FirebaseUser mCurrentUser;
    private TextInputLayout mTitle;
    private long mDeadlineDate = 0;
    private ProgressDialog mProgressDialog;
    private int mSelectedColor = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_deadline);

        mDatePickBtn = (Button) findViewById(R.id.add_deadline_pick_date_btn);
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());
        mTitle = (TextInputLayout) findViewById(R.id.add_deadline_title);
        mSelectedDateText = (TextView) findViewById(R.id.add_deadline_selected_date);
        mProgressDialog = new ProgressDialog(this   );

        mToolbar = (Toolbar) findViewById(R.id.add_deadline_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.add_deadline);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        final HSLColorPicker colorPicker = (HSLColorPicker) findViewById(R.id.add_deadline_color_picker);
        colorPicker.setScrollBarSize(5);
        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                //Do whatever you want with the color
                CircleImageView selected_color_view = (CircleImageView) findViewById(R.id.add_deadlines_selected_color_circle);
                ((GradientDrawable)selected_color_view.getBackground()).setColor(color);
                mSelectedColor = color;
            }
        });

        mDatePickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateTimePicker();
            }
        });


    }

    private void showDateTimePicker() {

        Locale locale = getResources().getConfiguration().locale;
        if (locale == Locale.US) {
            // Initializing date time picker
            SwitchDateTimeDialogFragment dateTimeDialogFragment = SwitchDateTimeDialogFragment.newInstance(
                    "Choose deadline",
                    "OK",
                    "Cancel"
            );
            getDeadlineDateFromUser(dateTimeDialogFragment);


        } else {
            // Initializing date time picker
            SwitchDateTimeDialogFragment dateTimeDialogFragment = SwitchDateTimeDialogFragment.newInstance(
                    "Sanani Tanlang",
                    "OK",
                    "Bekor Qilish"
            );

            getDeadlineDateFromUser(dateTimeDialogFragment);

        }
    }

    public void getDeadlineDateFromUser(SwitchDateTimeDialogFragment dateTimeDialogFragment) {

        // Assign values
        dateTimeDialogFragment.startAtCalendarView();
        dateTimeDialogFragment.set24HoursMode(true);
        dateTimeDialogFragment.setDefaultDateTime(Calendar.getInstance().getTime());

        //Setting the minimum date the user can pick
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.HOUR, -1);
        dt = c.getTime();
        dateTimeDialogFragment.setMinimumDateTime(dt);

        dateTimeDialogFragment.setOnButtonClickListener(new SwitchDateTimeDialogFragment.OnButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Date date) {

                mSelectedDateText.setText(date.toString());
                mDeadlineDate = date.getTime();


            }

            @Override
            public void onNegativeButtonClick(Date date) {

            }
        });
        // Show
        dateTimeDialogFragment.show(getSupportFragmentManager(), "dialog_time");


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.add_announcement_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.save_announcement_btn) {
            mCurrentUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    String university = dataSnapshot.child("university").getValue().toString();
                    String groupid = dataSnapshot.child("groupid").getValue().toString();

                    String title = mTitle.getEditText().getText().toString().trim();

                    //Checking if user did not provide any blank fields or dates
                    if (!TextUtils.isEmpty(title) && mDeadlineDate != 0) {
                        //If all of the fields are filled correctly
                        //Show progress dialog
                        mProgressDialog.setMessage("Creating Deadline...");
                        mProgressDialog.setCanceledOnTouchOutside(false); //Not allowing user to hide dialog by touching outside
                        mProgressDialog.show();
                        //Following function is responsible for user registration

                        createDeadline(title, mDeadlineDate, mSelectedColor, university, groupid);

                    } else {
                        //If user provided any blank fields, showing friendly message
                        Toast.makeText(AddDeadlineActivity.this, R.string.empty_fields_detected_make_sure_you_filled_title_and_date_fields, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    private void createDeadline(String title, long mDeadlineDate, int mSelectedColor, String university, String groupid) {


        //To save complex data, it is better to use HashMap
        final Map deadlineMap = new HashMap<>();
        deadlineMap.put("title",title);
        deadlineMap.put("date",mDeadlineDate);
        deadlineMap.put("color", mSelectedColor);

        //Getting the month of the deadline
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mDeadlineDate);
        int month_int = cal.get(Calendar.MONTH);
        String month = "";
        switch (month_int){
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

        final DatabaseReference deadlines_database =  FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupid).child("Deadlines").child(month).push();
        deadlines_database.setValue(deadlineMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()) {
                    mProgressDialog.dismiss();
                    Intent announcement_intent = new Intent(AddDeadlineActivity.this, DeadlinesActivity.class);
                    startActivity(announcement_intent);
                } else {
                    //In case of errors, just hide the progress dialog
                    mProgressDialog.hide();
                    Toast.makeText(AddDeadlineActivity.this, R.string.could_not_create_your_deadline, Toast.LENGTH_LONG).show();

                }
            }
        });

    }


}
