package com.techexpert.indianvaarta.Notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.techexpert.indianvaarta.ChatActivity;
import com.techexpert.indianvaarta.MainActivity;
import com.techexpert.indianvaarta.R;
import com.techexpert.indianvaarta.ReceiverID;

public class FirebaseMessaging extends FirebaseMessagingService
{
    private static final String TAG = "FcmListenerService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.e(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage == null)
        {
            return;
        }

        String sent = remoteMessage.getData().get("sent");
        String user = remoteMessage.getData().get("user");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser()!=null)
        {

            FirebaseUser firebaseUser = mAuth.getCurrentUser();

            SharedPreferences sp = getSharedPreferences("SP_USER",MODE_PRIVATE);
            String savedCurrentUser = sp.getString("Current_USERID","offline");

            if(firebaseUser!=null && sent.equals(mAuth.getCurrentUser().getUid()))
            {
              //  Toast.makeText(this, savedCurrentUser, Toast.LENGTH_SHORT).show();

               if(ChatActivity.s().equals("offline"))
               {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        sendOAndAboveNotification(remoteMessage);
                    }
                    else
                    {
                        sendNormalNotification(remoteMessage);
                    }
                }
            }
        }
    }

    private void sendNormalNotification(RemoteMessage remoteMessage)
    {
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        String user = remoteMessage.getData().get("user");
        String name = remoteMessage.getData().get("name");
        String pic = remoteMessage.getData().get("pic");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(user.replaceAll("[\\D]",""));

        Intent intent = new Intent(this, ChatActivity.class);

        Bundle bundle = new Bundle();
        bundle.putString("visit_user_id",user);
        bundle.putString("visit_user_name",name);
        bundle.putString("user_image",pic);

        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,j,intent,PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.vaarta)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        int i=0;
        if(j>0)
        {
            i=j;
        }

        notificationManager.notify(i,builder.build());
    }

    private void sendOAndAboveNotification(RemoteMessage remoteMessage)
    {
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        String user = remoteMessage.getData().get("user");
        String name = remoteMessage.getData().get("name");
        String pic = remoteMessage.getData().get("pic");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(user.replaceAll("[\\D]",""));

        Intent intent = new Intent(this, ChatActivity.class);

        Bundle bundle = new Bundle();
        bundle.putString("visit_user_id",user);
        bundle.putString("visit_user_name",name);
        bundle.putString("user_image",pic);

        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,j,intent,PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationOreo notificationOreo = new NotificationOreo(this);
        Notification.Builder builder = notificationOreo.getNotifications(title, body, pendingIntent, defaultSound, icon);

        int i=0;
        if(j>0)
        {
            i=j;
        }

        notificationOreo.getManager().notify(i,builder.build());
    }
}
