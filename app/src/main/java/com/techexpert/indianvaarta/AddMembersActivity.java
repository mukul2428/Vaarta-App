package com.techexpert.indianvaarta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddMembersActivity extends AppCompatActivity {

    Toolbar mToolbar;
    RecyclerView myContactsList;
    DatabaseReference ContactsRef, UsersRef;
    TextView textView;
    String group_id;
    FloatingActionButton floatingActionButton;

    ArrayList<String> arr;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_members);

        mToolbar = findViewById(R.id.group_members);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Add Members");

        textView = findViewById(R.id.main_text2);
        myContactsList = findViewById(R.id.contacts_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(this));

        floatingActionButton = findViewById(R.id.floating_button);

        //we will get current user id by firebase auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String currentUserID = mAuth.getCurrentUser().getUid();
        group_id = getIntent().getStringExtra("group_id");

        //getting reference of contacts saved in
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        arr = new ArrayList<>();
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

        FirebaseRecyclerOptions<contacts> options =
                new FirebaseRecyclerOptions.Builder<contacts>()
                        .setQuery(ContactsRef,contacts.class)
                        .build();


        FirebaseRecyclerAdapter<contacts, ContactsViewHolder> adapter
                = new FirebaseRecyclerAdapter<contacts, ContactsViewHolder>(options)
        {

            //setting all the elements in the recyclerview list
            @Override
            protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, int position, @NonNull contacts model)
            {

                //retrieving all the user id of users which are saved in current user contacts
                String UsersIds = getRef(position).getKey();

                Log.e("UsersIds", UsersIds);

                myContactsList.setVisibility(View.VISIBLE);
                textView.setVisibility(View.INVISIBLE);

                //to get all the info of user
                UsersRef.child(UsersIds).addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {

                            if(dataSnapshot.child("userState").hasChild("state"))
                            {
                                String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                String date = dataSnapshot.child("userState").child("date").getValue().toString();
                                String time = dataSnapshot.child("userState").child("time").getValue().toString();

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

                            String uid = dataSnapshot.child("uid").getValue().toString();

                            FirebaseDatabase.getInstance().getReference().child("Users").child(uid).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if(snapshot.hasChild("Group"))
                                        {
                                            if(snapshot.child("Group").hasChild(group_id))
                                            {
                                                holder.select_item.setVisibility(View.VISIBLE);
                                                holder.select_item.setText("Member");
                                                holder.select_item.setTextColor(Color.GREEN);
                                                holder.relativeLayout.setBackgroundResource(R.color.colorBackground);
                                                //Toast.makeText(getContext(), "Already a member", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });


                            holder.itemView.setOnClickListener(view ->
                            {

                                if(!holder.select_item.getText().equals("Selected") && !holder.select_item.getText().equals("Member"))
                                {
                                    holder.select_item.setVisibility(View.VISIBLE);
                                    holder.select_item.setText("Selected");
                                    arr.add(uid);
                                    floatingActionButton.setVisibility(View.VISIBLE);
                                    if(holder.select_item.getText().equals("Member"))
                                        holder.relativeLayout.setBackgroundResource(R.color.colorBackground);
                                    else
                                        holder.relativeLayout.setBackgroundResource(R.color.colorButton);
                                }
                                else if(holder.select_item.getText().equals("Selected"))
                                {
                                    arr.remove(uid);
                                    holder.select_item.setVisibility(View.GONE);
                                    holder.select_item.setText("");
                                    holder.relativeLayout.setBackgroundResource(R.color.colorBackground);
                                }

                                if(arr.isEmpty() || arr.contains(group_id))
                                    floatingActionButton.setVisibility(View.INVISIBLE);

                                floatingActionButton.setOnClickListener(view1 -> AddMembersToGroup(arr));
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            //linking with the xml file
            public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View view =  LayoutInflater.from(parent.getContext()).inflate(R.layout.user_display_layout2, parent,false);
                return new ContactsViewHolder(view);
            }
        };

        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ContactsViewHolder extends  RecyclerView.ViewHolder
    {

        TextView userName, userStatus, select_item;
        CircleImageView profileImage;
        ImageView OnlineIcon;
        RelativeLayout relativeLayout;

        public ContactsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name2);
            userStatus = itemView.findViewById(R.id.user_status2);
            profileImage = itemView.findViewById(R.id.users_profile_image2);
            OnlineIcon = itemView.findViewById(R.id.user_online_status);
            select_item = itemView.findViewById(R.id.elem_select);
            relativeLayout = itemView.findViewById(R.id.rl);
        }
    }

    private void AddMembersToGroup(ArrayList<String> arr)
    {
        DatabaseReference GroupRef= FirebaseDatabase.getInstance().getReference().child("Group");
        DatabaseReference GroupMemberRef= FirebaseDatabase.getInstance().getReference().child("Group Members");
        DatabaseReference UsersRef= FirebaseDatabase.getInstance().getReference().child("Users");

        String group_id = getIntent().getStringExtra("group_id");
        String group_name = getIntent().getStringExtra("group_name");
        String group_desc = getIntent().getStringExtra("group_desc");
        String group_pic = getIntent().getStringExtra("group_pic");
        for(String s: arr)
        {

            UsersRef.child(s).child("Group").child(group_id).setValue("Member").addOnCompleteListener(task -> {

            });
            GroupRef.child(s).child(group_id).child("name").setValue(group_name).addOnCompleteListener(task -> {

            });
            GroupRef.child(s).child(group_id).child("status").setValue(group_desc).addOnCompleteListener(task -> Toast.makeText(this, "Members Added", Toast.LENGTH_SHORT).show());

            GroupMemberRef.child(group_id).child(s).child("Group").setValue("Member").addOnCompleteListener(task -> {

            });
        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}