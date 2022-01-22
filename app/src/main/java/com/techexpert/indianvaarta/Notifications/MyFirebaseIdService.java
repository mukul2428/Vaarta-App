package com.techexpert.indianvaarta.Notifications;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseIdService extends FirebaseMessagingService
{

    @Override
    public void onNewToken(String s) {
        Log.e("NEW_TOKEN", s);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if(firebaseUser!=null)
        {
            updateToken(s);
        }
    }

    private void updateToken(String refreshToken)
    {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(refreshToken);
        if(firebaseUser!=null)
        {
            reference.child(firebaseUser.getUid()).setValue(token);
        }
    }
}
