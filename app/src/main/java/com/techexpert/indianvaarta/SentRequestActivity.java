package com.techexpert.indianvaarta;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class SentRequestActivity extends AppCompatActivity
{

    private RecyclerView myRequestList;
    private DatabaseReference ChatRequestRef, UsersRef, ContactsRef;
    private FirebaseAuth mAuth;

    private String currentUserID;

    private TextView textView;

    private Toolbar SentReqToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sent_request);

        textView = findViewById(R.id.main_text4);

        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ContactsRef =FirebaseDatabase.getInstance().getReference().child("Contacts");


        SentReqToolbar = findViewById(R.id.sent_req_toolbar);
        setSupportActionBar(SentReqToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Sent Request");


        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        myRequestList = findViewById(R.id.sent_request_list);
        myRequestList.setLayoutManager(new LinearLayoutManager(this));

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

    @Override
    public void onStart()
    {
        super.onStart();

        //getting all info from firebase
        FirebaseRecyclerOptions<contacts> options =
                new FirebaseRecyclerOptions.Builder<contacts>()
                        .setQuery(ChatRequestRef.child(currentUserID), contacts.class)
                        .build();

        //putting info in list using adapter
        FirebaseRecyclerAdapter<contacts, RequestViewHolder> adapter
                = new FirebaseRecyclerAdapter<contacts,RequestViewHolder>(options)
        {
            @Override
            //setting the data to the recyclerview list
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull contacts model)
            {

                //holder is an object of class that we have created
                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.VISIBLE);

                //getKey will get all the keys from chat request node of current user
                final String ListUserID = getRef(position).getKey();


                DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();

                getTypeRef.addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            String type = dataSnapshot.getValue().toString();

                            if(type.equals("sent"))
                            {

                                textView.setVisibility(View.GONE);
                                myRequestList.setVisibility(View.VISIBLE);

                                Button request_sent_btn = holder.itemView.findViewById(R.id.request_accept_btn);
                                request_sent_btn.setText("Cancel");

                                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.INVISIBLE);

                                holder.itemView.findViewById(R.id.request_accept_btn)
                                        .setOnClickListener(v -> ChatRequestRef.child(currentUserID).child(ListUserID)
                                                .removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>()
                                                {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task)
                                                    {
                                                        if(task.isSuccessful())
                                                        {
                                                            CancelSentRequest(ListUserID);
                                                        }
                                                    }
                                                }));


                                UsersRef.child(ListUserID).addValueEventListener(new ValueEventListener()
                                {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                    {
                                        if(dataSnapshot.hasChild("image"))
                                        {
                                            final String requestProfileImage = dataSnapshot.child("image").getValue().toString();
                                            Picasso.get().load(requestProfileImage).into(holder.profileImage);
                                        }

                                        final String requestUserName = dataSnapshot.child("name").getValue().toString();
                                        final String requestUserStatus = dataSnapshot.child("status").getValue().toString();

                                        holder.userName.setText(requestUserName);
                                        holder.userStatus.setText("You have sent a request");

                                        holder.itemView.setOnClickListener(new View.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(View v)
                                            {
                                                CharSequence options[] = new CharSequence[]
                                                        {
                                                                "Cancel Chat Request"
                                                        };
                                                AlertDialog.Builder builder = new AlertDialog.Builder(SentRequestActivity.this);
                                                builder.setTitle(requestUserName + " Sent Request");

                                                builder.setItems(options, new DialogInterface.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which)
                                                    {

                                                        if(which == 0)
                                                        {

                                                            //this will remove the request for sender
                                                            ChatRequestRef.child(currentUserID).child(ListUserID)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>()
                                                                    {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                        {
                                                                            if(task.isSuccessful())
                                                                            {
                                                                                CancelSentRequest(ListUserID);
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });

                                                builder.show();
                                            }
                                        });

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                            else
                            {

                                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.GONE);
                                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.GONE);
                                holder.itemView.findViewById(R.id.users_profile_image).setVisibility(View.GONE);
                                holder.itemView.findViewById(R.id.user_profile_name).setVisibility(View.GONE);
                                holder.itemView.findViewById(R.id.user_status).setVisibility(View.GONE);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            //setting connection with the xml file
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_display_layout,parent,false);
                return new RequestViewHolder(view);
            }
        };

        myRequestList.setAdapter(adapter);
        adapter.startListening();

    }


    //this class will contain all the items that we have to place on the list view
    public static class RequestViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName, userStatus;
        CircleImageView profileImage;
        Button AcceptBtn, CancelBtn;

        //constructor
        public RequestViewHolder(@NonNull View itemView)
        {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            AcceptBtn = itemView.findViewById(R.id.request_accept_btn);
            CancelBtn = itemView.findViewById(R.id.request_cancel_btn);
        }
    }

    private void CancelSentRequest(final String ListUserID)
    {
        //sent request will remove on receiver side
        ChatRequestRef.child(ListUserID).child(currentUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            Toast.makeText(SentRequestActivity.this, "You have cancelled the chat request...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}
