package com.techexpert.indianvaarta;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    Button CreateAccountButton;
    EditText UserEmail,UserPassword;
    TextView AlreadyHaveAccount;

    FirebaseAuth mAuth;

    DatabaseReference RootReference;

    ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth=FirebaseAuth.getInstance();

        InitializeFields();

        RootReference= FirebaseDatabase.getInstance().getReference();

        AlreadyHaveAccount.setOnClickListener(v -> SendUserToLoginActivity());

        CreateAccountButton.setOnClickListener(v -> CreateNewAccount());

    }

    private void InitializeFields() {

        CreateAccountButton=findViewById(R.id.register_button);
        UserEmail=findViewById(R.id.register_email);
        UserPassword=findViewById(R.id.register_password);
        AlreadyHaveAccount=findViewById(R.id.already_have_account_link);

        loadingBar=new ProgressDialog(this);

    }

    private void SendUserToMainActivity()
    {
        Intent MainIntent=new Intent(RegisterActivity.this,MainActivity.class);
        MainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(MainIntent);
        finish();
    }

    private void SendUserToLoginActivity()
    {
        Intent LoginIntent=new Intent(RegisterActivity.this,LoginActivity.class);
        startActivity(LoginIntent);
    }

    private void CreateNewAccount()
    {

        String email=UserEmail.getText().toString();
        String password=UserPassword.getText().toString();

        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(password))
        {
            Toast.makeText(this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        }
        else
        {

            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please wait, while we are creating new account for you...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(task -> {

                        if(task.isSuccessful())
                        {

                            FirebaseMessaging.getInstance ().getToken ()
                                    .addOnCompleteListener ( task1 -> {
                                        if (!task1.isSuccessful ())
                                        {
                                            //Could not get FirebaseMessagingToken
                                            String message= task1.getException().toString();
                                            Toast.makeText(RegisterActivity.this, "Error :"+ message, Toast.LENGTH_LONG).show();
                                            loadingBar.dismiss();
                                        }
                                        if (null != task1.getResult ())
                                        {
                                            //Got FirebaseMessagingToken
                                            String token = task1.getResult();

                                            String currentUserID = mAuth.getCurrentUser().getUid();

                                            RootReference.child("Users").child(currentUserID).setValue("");

                                            RootReference.child("Users").child(currentUserID).child("device_token")
                                                    .setValue(token);

                                            SendUserToMainActivity();
                                            Toast.makeText(RegisterActivity.this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
                                            loadingBar.dismiss();
                                        }
                                    } );
                        }
                        else
                        {
                            String message=task.getException().toString();
                            Toast.makeText(RegisterActivity.this, "Error :"+ message, Toast.LENGTH_LONG).show();
                            loadingBar.dismiss();
                        }
                    });
        }
    }
}