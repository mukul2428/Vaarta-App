package com.techexpert.indianvaarta.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.techexpert.indianvaarta.Activities.GroupChatActivity;
import com.techexpert.indianvaarta.GroupContacts;
import com.techexpert.indianvaarta.R;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class GroupFragment extends Fragment
{

    View GroupFragmentView;
    RecyclerView GroupChatList;

    TextView textView;

    FirebaseAuth mAuth;

    private DatabaseReference GroupRef;

    public GroupFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        GroupFragmentView=inflater.inflate(R.layout.fragment_group, container, false);

        mAuth = FirebaseAuth.getInstance();
        GroupRef= FirebaseDatabase.getInstance().getReference().child("Group").child(mAuth.getCurrentUser().getUid());

        textView = GroupFragmentView.findViewById(R.id.main_text3);

        GroupChatList = GroupFragmentView.findViewById(R.id.group_list);
        GroupChatList.setLayoutManager(new LinearLayoutManager(getContext()));

        return GroupFragmentView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        FirebaseRecyclerOptions<GroupContacts> options =
                new FirebaseRecyclerOptions.Builder<GroupContacts>()
                        .setQuery(GroupRef, GroupContacts.class)
                        .build();

        FirebaseRecyclerAdapter<GroupContacts, GroupChatViewHolder> adapter
                =new FirebaseRecyclerAdapter<GroupContacts, GroupChatViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull final GroupChatViewHolder holder, int position, @NonNull GroupContacts model)
            {
                final String usersIds = getRef(position).getKey();
                final String[] retImage = {"default_image"};

                //getting the info of each group
                GroupRef.child(usersIds).addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            if (dataSnapshot.hasChild("image"))
                            {
                                retImage[0] = dataSnapshot.child("image").getValue().toString();

                                Picasso.get().load(retImage[0])
                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                        .into(holder.group_image, new Callback() {
                                            @Override
                                            public void onSuccess() {
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                Picasso.get().load(retImage[0])
                                                        .into(holder.group_image);
                                            }
                                        });
                            }

                            if (dataSnapshot.hasChild("name") || dataSnapshot.hasChild("status"))
                            {
                                final String retName = dataSnapshot.child("name").getValue().toString();
                                final String retStatus = dataSnapshot.child("status").getValue().toString();
                                holder.group_name.setText(retName);
                                holder.group_status.setText(retStatus);

                                holder.itemView.setOnClickListener(v ->
                                {
                                    Intent chatIntent = new Intent(getContext(), GroupChatActivity.class);

                                    chatIntent.putExtra("visit_group_id",usersIds);
                                    chatIntent.putExtra("visit_group_name",retName);
                                    chatIntent.putExtra("group_image", retImage[0]);
                                    chatIntent.putExtra("group_desc", retStatus);

                                    startActivity(chatIntent);
                                });
                            }
                            textView.setVisibility(View.GONE);
                            GroupChatList.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public GroupChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View view = LayoutInflater.from(getContext()).inflate(R.layout.user_display_layout2, parent,false);
                return new GroupChatViewHolder(view);
            }
        };

        GroupChatList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class GroupChatViewHolder extends RecyclerView.ViewHolder
    {

        CircleImageView group_image;
        TextView group_status, group_name;

        public GroupChatViewHolder(@NonNull View itemView)
        {
            super(itemView);

            group_image = itemView.findViewById(R.id.users_profile_image2);
            group_name = itemView.findViewById(R.id.user_profile_name2);
            group_status = itemView.findViewById(R.id.user_status2);
        }
    }

}
