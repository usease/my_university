package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.madrapps.pikolo.HSLColorPicker;
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddTimetableActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private DatabaseReference mCurrentUserDatabase;
    private FirebaseUser mCurrentUser;
    private ProgressDialog mProgressDialog;
    private int mSelectedColor = 0;
    private TextInputLayout mName, mTeacher, mType, mRoom;
    private Spinner mDay, mStartTime, mEndTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_timetable);

        mName = (TextInputLayout) findViewById(R.id.add_timetable_name);
        mTeacher = (TextInputLayout) findViewById(R.id.add_timtable_teacher);
        mRoom = (TextInputLayout) findViewById(R.id.add_timtable_room);
        mType = (TextInputLayout) findViewById(R.id.add_timtable_type);

        mDay = (Spinner) findViewById(R.id.add_timetable_spinner);
        mStartTime = (Spinner) findViewById(R.id.add_timetable_start_time);
        mEndTime = (Spinner) findViewById(R.id.add_timetable_end_time);


        //Referencing toolbar and setting it
        mToolbar = (Toolbar) findViewById(R.id.add_timetable_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.add_new_module);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mProgressDialog = new ProgressDialog(this   );
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mCurrentUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.Weekdays, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.layout_for_spinner);
        // Apply the adapter to the spinner
        mDay.setAdapter(adapter);
        //Making the spinner to select the current month
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        //Since there is difference between array indexing and returned day of the week, we need to subtract 2 from the day

        day = day -2;
        mDay.setSelection(day);



        Double [] double_numbers = {6.00, 6.30, 7.00, 7.30, 8.00, 8.30, 9.00,9.30, 10.00, 10.30,11.00,11.30,12.00,12.30,13.00,13.30,14.00,14.30,15.00,15.30,16.00,16.30,17.00,17.30,18.00,18.30,19.00,19.30,20.00};


        ArrayAdapter <Double> dataAdapter = new ArrayAdapter<Double>( this,android.R.layout.simple_spinner_item,double_numbers);

        dataAdapter.setDropDownViewResource(R.layout.layout_for_spinner);
        //Apply the adapter to the spinner
        mStartTime.setAdapter(dataAdapter);
        mEndTime.setAdapter(dataAdapter);





        final HSLColorPicker colorPicker = (HSLColorPicker) findViewById(R.id.add_deadline_color_picker);
        colorPicker.setScrollBarSize(5);
        colorPicker.setColorSelectionListener(new SimpleColorSelectionListener() {
            @Override
            public void onColorSelected(int color) {
                //Do whatever you want with the color
                CircleImageView selected_color_view = (CircleImageView) findViewById(R.id.add_timetable_selected_color_circle);
                ((GradientDrawable)selected_color_view.getBackground()).setColor(color);
                mSelectedColor = color;
            }
        });




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

                    String name = mName.getEditText().getText().toString().trim();
                    String teacher = mTeacher.getEditText().getText().toString().trim();;
                    String room = mRoom.getEditText().getText().toString().trim();
                    String type = mType.getEditText().getText().toString().trim();


                    if(TextUtils.isEmpty(teacher)) {
                        teacher = "N/A";
                    }
                    if(TextUtils.isEmpty(room)) {
                        room = "N/A";
                    }
                    if(TextUtils.isEmpty(type)) {
                        type = "N/A";
                    }

                    String day = mDay.getSelectedItem().toString();
                    String start_time = mStartTime.getSelectedItem().toString();
                    String end_time = mEndTime.getSelectedItem().toString();
                    String time = start_time +  " - " + end_time;


                    //Checking if user did not provide any blank fields or dates
                    if (!TextUtils.isEmpty(name) && start_time != null && end_time != null) {
                        //If all of the fields are filled correctly
                        //Show progress dialog

                        mProgressDialog.setMessage("Creating Module...");
                        mProgressDialog.setCanceledOnTouchOutside(false); //Not allowing user to hide dialog by touching outside
                        mProgressDialog.show();
                        //Following function is responsible for user registration

                        createTimetable(day, time, name, room, teacher, mSelectedColor, university, groupid, type);

                    } else {
                        //If user provided any blank fields, showing friendly message
                        Toast.makeText(AddTimetableActivity.this, R.string.make_sure_you_filled_all_fields_with_astersik_sign, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    private void createTimetable(String day, String time, String name, String room, String teacher, int mSelectedColor, String university, String groupid, String type) {
        //To save complex data, it is better to use HashMap
        final Map timetableMap = new HashMap<>();
        timetableMap.put("name",name);
        timetableMap.put("time",time);
        timetableMap.put("room",room);
        timetableMap.put("teacher",teacher);
        timetableMap.put("color", mSelectedColor);
        timetableMap.put("type", type);



        final DatabaseReference timetable_database =  FirebaseDatabase.getInstance().getReference().child("Universities").child(university).child("Groups").child(groupid).child("Timetable").child(day).push();
        timetable_database.setValue(timetableMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()) {
                    mProgressDialog.dismiss();
                    Intent timetable_intent = new Intent(AddTimetableActivity.this, TimetableActivity.class);
                    startActivity(timetable_intent);
                } else {
                    //In case of errors, just hide the progress dialog
                    mProgressDialog.hide();
                    Toast.makeText(AddTimetableActivity.this, R.string.could_not_create_your_module, Toast.LENGTH_LONG).show();

                }
            }
        });



    }

}
