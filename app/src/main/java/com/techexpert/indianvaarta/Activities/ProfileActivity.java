package com.techexpert.indianvaarta.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techexpert.indianvaarta.R;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    String receiverUserId,current_state,senderUserID;

    CircleImageView userProfileImage;
    TextView userProfileName, userProfileStatus;
    Button SendMessageRequestButton, DeclineRequestButton;

    //these references are to create node for database
    DatabaseReference UserRef,ChatRequestRef, ContactsRef, NotificationRef;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");


        //taking the user id from findFriend activity whose profile is clicked
        receiverUserId = getIntent().getExtras().get("visitUserId").toString();

        senderUserID = mAuth.getCurrentUser().getUid();

        userProfileImage = findViewById(R.id.visit_profile_image);
        userProfileName = findViewById(R.id.visit_user_name);
        userProfileStatus = findViewById(R.id.visit_profile_status);
        SendMessageRequestButton = findViewById(R.id.send_message_request_button);
        DeclineRequestButton =findViewById(R.id.decline_message_request_button);

        current_state = "new";

        RetrieveUserInfo();

    }


    @Override
    protected void onResume() {
        super.onResume();

        MainActivity.UpdateUserStatus("online");

    }

    @Override
    protected void onPause() {
        super.onPause();

        MainActivity.UpdateUserStatus("offline");
    }

    private void RetrieveUserInfo()
    {
        //getting all the info of user which is clicked
        UserRef.child(receiverUserId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                //check if user has added his profile image
                if(dataSnapshot.exists() && (dataSnapshot.hasChild("image")))
                {
                    String userImage = dataSnapshot.child("image").getValue().toString();
                    String userName = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();

                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequest();
                }
                else
                {
                    String userName = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();;

                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    //creating chat request note in database
    private void ManageChatRequest()
    {
        ChatRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.hasChild(receiverUserId))
                        {
                            //creating node inside chat request node
                            String request_type = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();

                            if(request_type.equals("sent"))
                            {
                                current_state ="request_sent";
                                SendMessageRequestButton.setText("Cancel Chat Request");
                            }
                            //request_type.equals("received") only at receiver side
                            //so we are writing this code for receiver side
                            else if(request_type.equals("received"))
                            {
                                current_state = "request_received";

                                SendMessageRequestButton.setText("Accept Chat Request");

                                DeclineRequestButton.setVisibility(View.VISIBLE);
                                DeclineRequestButton.setEnabled(true);

                                DeclineRequestButton.setOnClickListener(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View v)
                                    {
                                        CancelChatRequest();
                                    }
                                });

                            }
                        }

                        //when there is no child node inside sender node then this
                        //means that both of them has become friends
                        else
                        {
                            ContactsRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener()
                                    {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                        {
                                            //creating node inside sender node
                                            if(dataSnapshot.hasChild(receiverUserId))
                                            {
                                                current_state = "friends";
                                                SendMessageRequestButton.setText("Remove this Contact");
                                                //sender can also remove receiver from contacts
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        if(!senderUserID.equals(receiverUserId))
        {
            SendMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                   SendMessageRequestButton.setEnabled(false);
                   //current_state = "new" means that we have not sent chat request
                    // to that particular user and it is new user to send request
                    if(current_state.equals("new"))
                    {
                        SendChatRequest();
                    }
                    //we have sent request to user and now we want to cancel it
                    if(current_state.equals("request_sent"))
                    {
                        CancelChatRequest();
                    }
                    //writing code for the receiver end
                    if(current_state.equals("request_received"))
                    {
                        AcceptChatRequest();
                    }
                    if(current_state.equals("friends"))
                    {
                        RemoveSpecificContact();
                    }

                }
            });
        }
    }


    private void RemoveSpecificContact()
    {
        //removing contacts at sender side
        ContactsRef.child(senderUserID).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if(task.isSuccessful())
                        {
                            //removing contacts at receiver side
                            ContactsRef.child(receiverUserId).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if(task.isSuccessful())
                                            {
                                                SendMessageRequestButton.setEnabled(true);
                                                current_state = "new";
                                                SendMessageRequestButton.setText("Send Message");

                                                DeclineRequestButton.setVisibility(View.INVISIBLE);
                                                DeclineRequestButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }


    //if receiver will click on accept request
    //then it will added to both sender and receiver contacts list
    private void AcceptChatRequest()
    {
        //for sender end
        ContactsRef.child(senderUserID).child(receiverUserId)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if(task.isSuccessful())
                        {
                            //for receiver end
                            ContactsRef.child(receiverUserId).child(senderUserID)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {

                                            //when contacts will be saved on both sender and receiver end
                                            //then we have remove the sent and received request from database
                                            if(task.isSuccessful())
                                            {
                                                //for sender end
                                                ChatRequestRef.child(senderUserID).child(receiverUserId)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>()
                                                        {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if(task.isSuccessful())
                                                                {
                                                                    //for receiver end
                                                                    ChatRequestRef.child(receiverUserId).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>()
                                                                            {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                {
                                                                                    //code for receiver end who can remove sender from contacts

                                                                                    SendMessageRequestButton.setEnabled(true);
                                                                                    current_state ="friends";
                                                                                    SendMessageRequestButton.setText("Remove this Contact");

                                                                                    DeclineRequestButton.setVisibility(View.INVISIBLE);
                                                                                    DeclineRequestButton.setEnabled(false);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void CancelChatRequest()
    {
        //removing sent request at sender side
        ChatRequestRef.child(senderUserID).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if(task.isSuccessful())
                        {
                            //removing sent request at receiver side
                            ChatRequestRef.child(receiverUserId).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>()
                                    {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if(task.isSuccessful())
                                            {
                                                SendMessageRequestButton.setEnabled(true);
                                                current_state = "new";
                                                SendMessageRequestButton.setText("Send Message");

                                                DeclineRequestButton.setVisibility(View.INVISIBLE);
                                                DeclineRequestButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void SendChatRequest()
    {
        //creating receiver node inside sender node
        //and setting request_type of sender node to sent
        ChatRequestRef.child(senderUserID).child(receiverUserId)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        //setting request_type of receiver node to received
                        ChatRequestRef.child(receiverUserId).child(senderUserID)
                                .child("request_type").setValue("received")
                                .addOnCompleteListener(new OnCompleteListener<Void>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if(task.isSuccessful())
                                        {

                                            HashMap<String, String> chatNotificationMap = new HashMap<>();
                                            chatNotificationMap.put("from", senderUserID);
                                            chatNotificationMap.put("type", "request");

                                            //we will give unique key to each notification
                                            NotificationRef.child(receiverUserId).push()
                                                    .setValue(chatNotificationMap).addOnCompleteListener(new OnCompleteListener<Void>()
                                            {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task)
                                                {
                                                    if(task.isSuccessful())
                                                    {
                                                        SendMessageRequestButton.setEnabled(true);
                                                        current_state = "request_sent";
                                                        SendMessageRequestButton.setText("Cancel Chat Request");
                                                    }
                                                }
                                            });

                                        }
                                    }
                                });
                    }
                });
    }
}
