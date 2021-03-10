package com.techexpert.indianvaarta.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.techexpert.indianvaarta.ChatActivity;
import com.techexpert.indianvaarta.LoginActivity;
import com.techexpert.indianvaarta.Notifications.Token;
import com.techexpert.indianvaarta.R;
import com.techexpert.indianvaarta.contacts;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment
{
    private RecyclerView chatList;

    private DatabaseReference ChatsRef, UsersRef;
    private FirebaseAuth mAuth;

    private TextView textView;

    private String currentUserID ="";


    public ChatFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View private_chats_view;

        private_chats_view = inflater.inflate(R.layout.fragment_chat, container, false);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser()!=null)
        {
            currentUserID = mAuth.getCurrentUser().getUid();
        }
        else
        {
            Intent loginIntent=new Intent(getContext(), LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(loginIntent);
        }

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatsRef = FirebaseDatabase.getInstance().getReference()
                .child("Contacts").child(currentUserID);

        textView = private_chats_view.findViewById(R.id.main_text);

        chatList = private_chats_view.findViewById(R.id.chats_list);
        chatList.setLayoutManager(new LinearLayoutManager(getContext()));

       // updateToken(FirebaseInstanceId.getInstance().getToken());

        return private_chats_view;
    }

//    private void updateToken(String token)
//    {
//        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
//        Token token1 = new Token(token);
//        reference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token1);
//    }

    @Override
    public void onStart()
    {
        super.onStart();

        FirebaseRecyclerOptions<contacts> options =
                new FirebaseRecyclerOptions.Builder<contacts>()
                .setQuery(ChatsRef, contacts.class)
                .build();

        FirebaseRecyclerAdapter<contacts, ChatsViewHolder> adapter
                =new FirebaseRecyclerAdapter<contacts, ChatsViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsViewHolder holder, int position, @NonNull contacts model)
            {
                //by this line we will get user id of each key line by line present in a contact node of current user
                final String usersIds = getRef(position).getKey();
                final String[] retImage = {"default_image"};

                textView.setVisibility(View.GONE);
                chatList.setVisibility(View.VISIBLE);

                //getting the info of each user
                UsersRef.child(usersIds).addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            if(dataSnapshot.hasChild("image"))
                            {
                                retImage[0] = dataSnapshot.child("image").getValue().toString();

                                Picasso.get().load(retImage[0])
                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                        .into(holder.profile_image, new Callback() {
                                            @Override
                                            public void onSuccess() {
                                            }
                                            @Override
                                            public void onError(Exception e)
                                            {
                                                Picasso.get().load(retImage[0])
                                                        .into(holder.profile_image);
                                            }
                                        });
                            }

                            final String retName = dataSnapshot.child("name").getValue().toString();
                            final String retStatus = dataSnapshot.child("status").getValue().toString();

                            holder.user_name.setText(retName);

                            if(dataSnapshot.child("userState").hasChild("state"))
                            {
                                String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                String date = dataSnapshot.child("userState").child("date").getValue().toString();
                                String time = dataSnapshot.child("userState").child("time").getValue().toString();

                                //checking state if online or offline
                                if(state.equals("online"))
                                {
                                    holder.user_status.setTextColor(Color.WHITE);
                                    holder.user_status.setText("Online");
                                }
                                else if(state.equals("offline"))
                                {
                                    holder.user_status.setTextColor(Color.WHITE);
                                    holder.user_status.setText("Last Seen: "+ date +" "+ time);
                                }
                                else if(state.equals("typing..."))
                                {
                                    holder.user_status.setText("typing...");
                                    holder.user_status.setTextColor(Color.GREEN);
                                }
                            }
                            //for those who have not updated their user profile and had just had just made the id
                            else
                            {
                                holder.user_status.setText("Offline");
                            }

                            holder.itemView.setOnClickListener(v ->
                            {
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);

                                chatIntent.putExtra("visit_user_id",usersIds);
                                chatIntent.putExtra("visit_user_name",retName);
                                chatIntent.putExtra("user_image", retImage[0]);

                                startActivity(chatIntent);
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
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View view = LayoutInflater.from(getContext()).inflate(R.layout.user_display_layout2, parent,false);
                return new ChatsViewHolder(view);
            }
        };

        chatList.setAdapter(adapter);
        adapter.startListening();
    }


    public static class ChatsViewHolder extends RecyclerView.ViewHolder
    {

        CircleImageView profile_image;
        TextView user_status, user_name;

        public ChatsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            profile_image = itemView.findViewById(R.id.users_profile_image2);
            user_name = itemView.findViewById(R.id.user_profile_name2);
            user_status = itemView.findViewById(R.id.user_status2);
        }
    }
}
