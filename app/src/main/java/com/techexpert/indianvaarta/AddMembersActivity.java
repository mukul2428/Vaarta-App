package com.techexpert.indianvaarta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;

import com.techexpert.indianvaarta.Fragments.ContactFragment;

public class AddMembersActivity extends AppCompatActivity {

    Toolbar mToolbar;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_members);

        mToolbar = findViewById(R.id.group_members);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Add Members");

        Fragment fragment = new ContactFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.members_fragment,fragment).commit();

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

    public String GroupId()
    {
        if(getIntent().getStringExtra("group_id") != null)
            return getIntent().getStringExtra("group_id");
        return "id";
    }
    public String GroupName()
    {
        return getIntent().getStringExtra("group_name");
    }
    public String GroupDesc()
    {
        return getIntent().getStringExtra("group_desc");
    }
    public String GroupImage()
    {
        return getIntent().getStringExtra("group_pic");
    }
}