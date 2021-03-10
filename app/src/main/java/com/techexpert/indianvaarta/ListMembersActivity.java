package com.techexpert.indianvaarta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class ListMembersActivity extends AppCompatActivity {

    private RecyclerView myMemberList;
    private DatabaseReference AllMembersRef, UsersRef;

    private Toolbar ReqToolbar;

    private String GroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_members);

        GroupId = getIntent().getStringExtra("group_id");

        Log.e("myTag1", GroupId);

        AllMembersRef = FirebaseDatabase.getInstance().getReference().child("Group Members").child(GroupId);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        ReqToolbar = findViewById(R.id.group_all_members);
        setSupportActionBar(ReqToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Members of Group");

        myMemberList = findViewById(R.id.all_members_list);
        myMemberList.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public void onStart()
    {
        super.onStart();

        //getting all info from firebase
        FirebaseRecyclerOptions<contacts> options =
                new FirebaseRecyclerOptions.Builder<contacts>()
                        .setQuery(AllMembersRef, contacts.class)
                        .build();

        //putting info in list using adapter
        FirebaseRecyclerAdapter<contacts, RequestViewHolder> adapter
                = new FirebaseRecyclerAdapter<contacts,RequestViewHolder>(options)
        {
            @Override
            //setting the data to the recyclerview list
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull contacts model)
            {

                final String ListUserID = getRef(position).getKey();
                Log.e("myTag", ListUserID);
                UsersRef.child(ListUserID).addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            if(dataSnapshot.child("userState").hasChild("state"))
                            {
                                String state = dataSnapshot.child("userState").child("state").getValue().toString();

                                //checking state if online or offline
                                if(state.equals("online"))
                                {
                                    holder.OnlineIcon.setVisibility(View.VISIBLE);
                                }
                                else if(state.equals("offline"))
                                {
                                    holder.OnlineIcon.setVisibility(View.INVISIBLE);
                                }
                            }
                            //for those who have not updated their user profile and had just had just made the id
                            else
                            {
                                holder.OnlineIcon.setVisibility(View.INVISIBLE);
                            }


                            if(dataSnapshot.hasChild("image"))
                            {
                                String UserImage = dataSnapshot.child("image").getValue().toString();
                                String profileStatus = dataSnapshot.child("status").getValue().toString();
                                String profileName = dataSnapshot.child("name").getValue().toString();

                                //display retrieved info
                                holder.userName.setText(profileName);
                                holder.userStatus.setText(profileStatus);
                                Picasso.get().load(UserImage).placeholder(R.drawable.profile_image).into(holder.profileImage);
                            }
                            else
                            {
                                String profileStatus = dataSnapshot.child("status").getValue().toString();
                                String profileName = dataSnapshot.child("name").getValue().toString();

                                //display retrieved info
                                holder.userName.setText(profileName);
                                holder.userStatus.setText(profileStatus);
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
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_display_layout2,parent,false);
                return new RequestViewHolder(view);
            }
        };

        myMemberList.setAdapter(adapter);
        adapter.startListening();

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



    //this class will contain all the items that we have to place on the list view
    public static class RequestViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName, userStatus;
        CircleImageView profileImage;
        ImageView OnlineIcon;

        //constructor
        public RequestViewHolder(@NonNull View itemView)
        {
            super(itemView);

            OnlineIcon = itemView.findViewById(R.id.user_online_status);
            userName = itemView.findViewById(R.id.user_profile_name2);
            userStatus = itemView.findViewById(R.id.user_status2);
            profileImage = itemView.findViewById(R.id.users_profile_image2);
        }
    }
}