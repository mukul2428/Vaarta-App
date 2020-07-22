package com.techexpert.indianvaarta;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendActivity extends AppCompatActivity {


    Toolbar mToolbar;
    RecyclerView FindFriendRecyclerView;

    DatabaseReference UserRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friend);

        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");

        FindFriendRecyclerView = findViewById(R.id.find_friend_recycler_list);
        FindFriendRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mToolbar = findViewById(R.id.find_friend_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");

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
    protected void onStart()
    {
        super.onStart();


        FirebaseRecyclerOptions<contacts> options =
                new FirebaseRecyclerOptions.Builder<contacts>()
                .setQuery(UserRef, contacts.class)
                .build();

        FirebaseRecyclerAdapter<contacts, FindFriendsViewHolder> adapter=
                new FirebaseRecyclerAdapter<contacts, FindFriendsViewHolder>(options)
                {
                    @Override
                    protected void onBindViewHolder(@NonNull final FindFriendsViewHolder holder, final int position, @NonNull final contacts model)
                    {

                        if(!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(model.getUid()))
                        {
                            //setting the data to the recyclerview list

                            holder.userName.setText(model.getName());
                            holder.userStatus.setText(model.getStatus());

                            Picasso.get().load(model.getImage())
                                    .networkPolicy(NetworkPolicy.OFFLINE)
                                    .placeholder(R.drawable.profile_image)
                                    .into(holder.profileImage, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                        }
                                        @Override
                                        public void onError(Exception e)
                                        {
                                            Picasso.get().load(model.getImage())
                                                    .placeholder(R.drawable.profile_image)
                                                    .into(holder.profileImage);
                                        }
                                    });


                            //if you click on any list item then you will be go on other activity
                            holder.itemView.setOnClickListener(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                {
                                    String visitUserId = getRef(position).getKey();

                                    //passing userId to other activity of the person which is clicked by us
                                    Intent profileIntent = new Intent(FindFriendActivity.this,ProfileActivity.class);
                                    profileIntent.putExtra("visitUserId",visitUserId);
                                    startActivity(profileIntent);

                                }
                            });
                        }
                        else
                        {
                            holder.userName.setVisibility(View.GONE);
                            holder.userStatus.setVisibility(View.GONE);
                            holder.profileImage.setVisibility(View.GONE);
                            holder.linearLayout.setVisibility(View.GONE);
                        }

                    }

                    @NonNull
                    @Override
                    public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
                    {
                        //setting connection with the xml file
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_display_layout2, parent,false);
                        FindFriendsViewHolder viewHolder = new FindFriendsViewHolder(view);
                        return  viewHolder;
                    }
                };

        FindFriendRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }


    //this class will contain all the items that we have to place on the list view
    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder
    {

        TextView userName, userStatus;
        CircleImageView profileImage;
        LinearLayout linearLayout;

        //constructor
        public FindFriendsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name2);
            userStatus= itemView.findViewById(R.id.user_status2);
            profileImage = itemView.findViewById(R.id.users_profile_image2);
            linearLayout = itemView.findViewById(R.id.linear_layout);

        }
    }
}
