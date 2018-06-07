package com.example.usease.myuniversity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout mEmail, mPassword;
    private Button mLoginBtn;
    private Toolbar mToolbar;
    private ProgressDialog mLoginProgress;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Referencing views
        mEmail = (TextInputLayout) findViewById(R.id.login_email);
        mPassword  = (TextInputLayout) findViewById(R.id.login_password);
        mLoginBtn = (Button) findViewById(R.id.login_login_btn);
        mToolbar = (Toolbar) findViewById(R.id.login_app_bar);

        //Initializing Progress Dialog and FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        mLoginProgress = new ProgressDialog(this);

        //Setting up Toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Log in");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //Enabling go back button in the Toolbar

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Getting user input
                String email  = mEmail.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();
                //Checking for blank fields
                if(!TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)) {
                    //Showing progress dialog
                    mLoginProgress.setTitle("Loggin in...");
                    mLoginProgress.setMessage("Please wait while we check your credentials.");
                    mLoginProgress.setCanceledOnTouchOutside(false); //Not allowing user to hide dialog by touching outside
                    mLoginProgress.show();
                    //Allowing to log in only if all of the fields are filled
                    login(email, password);
                } else {
                    //Warning about the empty fields
                    Toast.makeText(LoginActivity.this, "There were some empy fields detected. Please fill them to proceed.", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    //Function is responsible for loggin in the user
    private void login(String email, String password) {

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    //If log in is successful

                    //Dismiss the progress dialog
                    mLoginProgress.dismiss();

                    //Go to MainActivity
                    Intent main_intent = new Intent(LoginActivity.this, MainActivity.class);

                    //Since the LoginActivity has parent StartActivity, when new acitivty is created we need to create whole new task
                    //so that we cant go back to StartActivity from the MainActivity
                    main_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(main_intent);
                    //User should not be able to come back to LoginActivity, thus we need to finish this activity
                    finish();

                } else {
                    //In case task fails, hiding the dialog and showing friendly message
                    mLoginProgress.hide();
                    Toast.makeText(LoginActivity.this, "There were problems while signing in. Please check your input and try again.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
