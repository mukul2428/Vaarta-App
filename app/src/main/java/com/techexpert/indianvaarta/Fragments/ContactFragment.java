package com.techexpert.indianvaarta.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.squareup.picasso.Picasso;
import com.techexpert.indianvaarta.R;
import com.techexpert.indianvaarta.contacts;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ContactFragment extends Fragment
{
    private RecyclerView myContactsList;

    private DatabaseReference ContactsRef, UsersRef;

    public ContactFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        // Inflate the layout for this fragment
        View contactsView = inflater.inflate(R.layout.fragment_contact, container, false);

        myContactsList = contactsView.findViewById(R.id.contacts_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(getContext()));

        //we will get current user id by firebase auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String currentUserID = mAuth.getCurrentUser().getUid();

        //getting reference of contacts saved in
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return contactsView;
    }

    //retrieving data from firebase and putting into recycler view
    @Override
    public void onStart()
    {
        super.onStart();

        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<contacts>()
                .setQuery(ContactsRef,contacts.class)
                .build();


        FirebaseRecyclerAdapter<contacts,ContactsViewHolder> adapter
                = new FirebaseRecyclerAdapter<contacts, ContactsViewHolder>(options)
        {

            //setting all the elements in the recyclerview list
            @Override
            protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, int position, @NonNull contacts model)
            {

                //retrieving all the user id of users which are saved in current user contacts
                String UsersIds = getRef(position).getKey();

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
                //object of ContactsViewHolder class
                ContactsViewHolder viewHolder = new ContactsViewHolder(view);
                return viewHolder;
            }
        };

        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }



    public static class ContactsViewHolder extends  RecyclerView.ViewHolder
    {

        TextView userName, userStatus;
        CircleImageView profileImage;
        ImageView OnlineIcon;

        public ContactsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name2);
            userStatus = itemView.findViewById(R.id.user_status2);
            profileImage = itemView.findViewById(R.id.users_profile_image2);
            OnlineIcon = itemView.findViewById(R.id.user_online_status);
        }
    }
}
