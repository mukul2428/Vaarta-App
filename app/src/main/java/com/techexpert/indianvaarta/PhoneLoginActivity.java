package com.techexpert.indianvaarta;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity
{
    private  Button sendVerificationCode, VerifyButton;
    private EditText InputPhoneNumber, InputVerificationCode;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private FirebaseAuth mAuth;

    private String mVerificationId;

    private ProgressDialog LoadingBar;

    private DatabaseReference UsersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        sendVerificationCode = findViewById(R.id.send_ver_code_button);
        VerifyButton = findViewById(R.id.verify_button);
        InputPhoneNumber = findViewById(R.id.phone_number_input);
        InputVerificationCode = findViewById(R.id.verification_code_input);

        LoadingBar = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        sendVerificationCode.setOnClickListener(v -> {

            String phoneNumber = "+91"+InputPhoneNumber.getText().toString();

            if(TextUtils.isEmpty(phoneNumber))
            {
                Toast.makeText(PhoneLoginActivity.this, "Please enter phone number", Toast.LENGTH_SHORT).show();
            }
            else
            {
                LoadingBar.setTitle("Phone Verification");
                LoadingBar.setMessage("Please wait, while we are authenticating your number...");
                LoadingBar.setCanceledOnTouchOutside(false);
                LoadingBar.show();

                PhoneAuthOptions options =
                        PhoneAuthOptions.newBuilder(mAuth)
                                .setPhoneNumber(phoneNumber)       // Phone number to verify
                                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                .setActivity(this)                 // Activity (for callback binding)
                                .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
                                .build();
                PhoneAuthProvider.verifyPhoneNumber(options);
            }
        });

        VerifyButton.setOnClickListener(v -> {

            sendVerificationCode.setVisibility(View.INVISIBLE);
            InputPhoneNumber.setVisibility(View.INVISIBLE);

            String verificationCode = InputVerificationCode.getText().toString();
            if(TextUtils.isEmpty(verificationCode))
            {
                Toast.makeText(PhoneLoginActivity.this, "Please enter the code", Toast.LENGTH_SHORT).show();
            }
            else
            {
                LoadingBar.setTitle("Code Verification");
                LoadingBar.setMessage("Please wait, while we are verifying your code...");
                LoadingBar.setCanceledOnTouchOutside(false);
                LoadingBar.show();

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                signInWithPhoneAuthCredential(credential);
            }
        });


        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks()
        {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential)
            {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e)
            {
                Toast.makeText(PhoneLoginActivity.this, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                LoadingBar.dismiss();

                sendVerificationCode.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);
                VerifyButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                LoadingBar.dismiss();

                Toast.makeText(PhoneLoginActivity.this, "Verification Code sent successfully", Toast.LENGTH_SHORT).show();

                sendVerificationCode.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);
                VerifyButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);
            }
        };
    }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential)
    {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful())
                    {
                        final String currentUserId = mAuth.getCurrentUser().getUid();

                        FirebaseMessaging.getInstance ().getToken ()
                                .addOnCompleteListener ( task1 ->
                                {
                                    if (!task1.isSuccessful ())
                                    {
                                        //Could not get FirebaseMessagingToken
                                        String message = task1.getException().toString();
                                        Toast.makeText(PhoneLoginActivity.this, "Error: "+ message, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (null != task1.getResult ())
                                    {
                                        //Got FirebaseMessagingToken
                                        String firebaseMessagingToken = Objects.requireNonNull(task1.getResult());
                                        UsersRef.child(currentUserId).child("device_token")
                                                .setValue(firebaseMessagingToken);
                                        LoadingBar.dismiss();
                                        Toast.makeText(PhoneLoginActivity.this, "Logged in Successfully...", Toast.LENGTH_SHORT).show();
                                        sendUserToMainActivity();
                                    }
                                } );
                    }
                    else
                    {
                        // Sign in failed
                        String message = task.getException().toString();
                        Toast.makeText(PhoneLoginActivity.this, "Error: "+ message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendUserToMainActivity()
    {
        Intent mainIntent = new Intent(PhoneLoginActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

}
