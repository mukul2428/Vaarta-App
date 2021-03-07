package com.techexpert.indianvaarta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.security.acl.Group;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatActivity extends AppCompatActivity {

    Toolbar mToolbar;

    ImageButton SendMessageButton;
    EditText UserMessageInput;

    ImageButton SendGroupFilesBtn;

    String GroupName, GroupImage, GroupDescription;
    String checker = "", myUrl = "";
    Uri fileUri;
    StorageTask uploadTask;

    final List<Messages> messagesList = new ArrayList<>();
    MessageAdapter messageAdapter;
    RecyclerView userMessagesList;
    LinearLayoutManager linearLayoutManager;

    ProgressDialog loadingBar;

    TextView userName, userLastSeen;
    CircleImageView userImage;

    FirebaseAuth mAuth;

    DatabaseReference UserRef,GroupNameRef,GroupMessageKeyRef;

    String CurrentGroupID,currentUserID,currentUserName,currentDate,currentTime;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        CurrentGroupID = getIntent().getStringExtra("visit_group_id");

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        GroupNameRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(CurrentGroupID);

        InitialiseFields();

        GroupName = getIntent().getStringExtra("visit_group_name");
        GroupImage = getIntent().getStringExtra("group_image");
        GroupDescription = getIntent().getStringExtra("group_desc");

        userName.setText(GroupName);
        userLastSeen.setText(GroupDescription);
        Picasso.get().load(GroupImage)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.profile_image)
                .into(userImage, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(GroupImage)
                                .placeholder(R.drawable.profile_image)
                                .into(userImage);
                    }
                });

        GetUserInfo();

        SendMessageButton.setOnClickListener(v -> {

            SendMessageInfoToDatabase();

            UserMessageInput.setText("");
        });

        SendGroupFilesBtn.setOnClickListener(v -> {
            CharSequence[] options = new CharSequence[]
                    {
                            "Images",
                            "PDF File",
                            "MS Word File"
                    };

            AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
            builder.setTitle("Select File");
            builder.setItems(options, (dialogInterface, i) -> {
                if (i == 0) {
                    checker = "image";

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent.createChooser(intent, "Select Image"), 5);
                }
                if (i == 1) {
                    checker = "pdf";

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/pdf");
                    startActivityForResult(intent.createChooser(intent, "Select PDF File"), 5);

                }
                if (i == 2) {
                    checker = "docx";

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/msword");
                    startActivityForResult(intent.createChooser(intent, "Select Ms Word File"), 5);
                }
            });
            builder.show();
        });


        FirebaseDatabase.getInstance().getReference().
                child("Groups").child(CurrentGroupID).
                addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        Messages messages = snapshot.getValue(Messages.class);
                        messagesList.add(messages);

                        messageAdapter.notifyDataSetChanged();

                        userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 5 && resultCode == RESULT_OK && data!=null && data.getData()!=null)
        {

            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait, We are sending your file...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();


            //store the file that is selected by user
            fileUri = data.getData();

            //if user has not selected the image type
            if(!checker.equals("image"))
            {

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");

                //each message has unique key
                final String messagePushId = GroupNameRef.push().getKey();

                //it pointing to folder of images in database and we are giving unique key for each image
                final StorageReference Filepath = storageReference.child(messagePushId + "." + checker);

                //putting file in firebase from where user can retrieve it
                Filepath.putFile(fileUri).addOnCompleteListener(task ->
                {
                    if(task.isSuccessful())
                    {

                        FirebaseStorage storageReference1 = FirebaseStorage.getInstance();

                        StorageReference storageRef = storageReference1.getReference().child("Document Files");


                        storageRef.child(messagePushId + "."+ checker).getDownloadUrl()
                                .addOnCompleteListener(task1 -> {
                                    String PdfUrl = task1.getResult().toString();

                                    Map messageFile = new HashMap();
                                    messageFile.put("message",PdfUrl); //link to file present in Storage
                                    messageFile.put("name",fileUri.getLastPathSegment());
                                    messageFile.put("type", checker);
                                    messageFile.put("from",FirebaseAuth.getInstance().getCurrentUser().getUid());
                                    messageFile.put("to", "");
                                    messageFile.put("messageID",messagePushId);
                                    messageFile.put("time",currentTime);
                                    messageFile.put("date",currentDate);

                                    Map messageBodyDetails = new HashMap();
                                    messageBodyDetails.put(messagePushId, messageFile);

                                    FirebaseDatabase.getInstance().getReference().updateChildren(messageBodyDetails);

                                    //creating a sent folder of files which are send by user
                                    final File ImageFolder =new File(Environment.getExternalStorageDirectory()+ File.separator+"Vaarta"+ File.separator+"Vaarta Documents" + File.separator+"Sent");

                                    //downloading the files from firebase which you have sent
                                    //and storing to sent folder

                                    boolean success = true;
                                    if(!ImageFolder.exists())
                                    {
                                        success = ImageFolder.mkdirs();
                                    }
                                    if(success)
                                    {
                                        StorageReference storageImageRef = FirebaseStorage.getInstance().getReference("Document Files/");

                                        if(checker.equals("pdf"))
                                        {
                                            final File outputFile =new File(ImageFolder,messagePushId+".pdf");

                                            storageImageRef.child(messagePushId+".pdf").getFile(outputFile)
                                                    .addOnSuccessListener(taskSnapshot -> {

                                                        //Toast.makeText(holder.itemView.getContext(), "Image sent", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(GroupChatActivity.this, "Failed", Toast.LENGTH_SHORT).show());
                                        }
                                        else if(checker.equals("docx"))
                                        {
                                            final File outputFile =new File(ImageFolder,messagePushId+".docx");

                                            storageImageRef.child(messagePushId+".docx").getFile(outputFile)
                                                    .addOnSuccessListener(taskSnapshot -> {

                                                        //Toast.makeText(holder.itemView.getContext(), "Image sent", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(new OnFailureListener()
                                                    {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e)
                                                        {
                                                            Toast.makeText(GroupChatActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }
                                    else
                                    {
                                        Toast.makeText(GroupChatActivity.this, "Please grant permission for storage...", Toast.LENGTH_SHORT).show();
                                    }

                                    loadingBar.dismiss();

                                });

                    }
                }).addOnFailureListener(e -> {
                    loadingBar.dismiss();
                    Toast.makeText(GroupChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnProgressListener(taskSnapshot -> {
                    double p = (100.0*taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    loadingBar.setMessage((int) p + "%  Uploading....");
                });

            }


            else if(checker.equals("image"))
            {
                //it will create a folder in storage of firebase
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

                //each message has unique key
                final String messagePushId = GroupNameRef.push().getKey();

                //it pointing to folder of images in database and we are giving unique key for each image
                final StorageReference Filepath = storageReference.child(messagePushId + ".jpg");

                //putting file inside the storage
                //fileUri contains image
                uploadTask = Filepath.putFile(fileUri);

                uploadTask.continueWithTask((Continuation) task -> {
                    if(!task.isSuccessful())
                    {
                        throw task.getException();
                    }
                    return Filepath.getDownloadUrl();
                }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                    if(task.isSuccessful())
                    {
                        Uri downloadUrl = task.getResult();

                        //myUrl will contain link of the image
                        myUrl = downloadUrl.toString();


                        Map messageImageBody = new HashMap();
                        messageImageBody.put("message",myUrl);
                        messageImageBody.put("name",fileUri.getLastPathSegment());
                        messageImageBody.put("type", checker);
                        messageImageBody.put("from",FirebaseAuth.getInstance().getCurrentUser().getUid());
                        messageImageBody.put("to", "");
                        messageImageBody.put("messageID",messagePushId);
                        messageImageBody.put("time",currentTime);
                        messageImageBody.put("date",currentDate);

                        CurrentGroupID = getIntent().getExtras().get("visit_group_id").toString();

                        Map messageBodyDetails = new HashMap();
                        messageBodyDetails.put("Groups/" + CurrentGroupID + "/" + messagePushId, messageImageBody);

                        FirebaseDatabase.getInstance().getReference().updateChildren(messageBodyDetails).addOnCompleteListener((OnCompleteListener) task12 -> {
                            if(task12.isSuccessful())
                            {
                                loadingBar.dismiss();
                                Toast.makeText(GroupChatActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();

                                //creating a sent folder of files which are send by user
                                final File ImageFolder =new File(Environment.getExternalStorageDirectory()+ File.separator+"Vaarta"+ File.separator+"Vaarta Images" + File.separator+"Sent");

                                //downloading the files from firebase which you have sent
                                //and storing to sent folder

                                boolean success = true;
                                if(!ImageFolder.exists())
                                {
                                    success = ImageFolder.mkdirs();
                                }
                                if(success)
                                {
                                    final File outputFile =new File(ImageFolder,messagePushId+".jpg");

                                    StorageReference storageImageRef = FirebaseStorage.getInstance().getReference("Image Files/");

                                    storageImageRef.child(messagePushId+".jpg").getFile(outputFile)
                                            .addOnSuccessListener(taskSnapshot -> {

                                                //Toast.makeText(holder.itemView.getContext(), "Image sent", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(GroupChatActivity.this, "Failed", Toast.LENGTH_SHORT).show());
                                }
                                else
                                {
                                    Toast.makeText(GroupChatActivity.this, "Please grant permission for storage...", Toast.LENGTH_SHORT).show();
                                }

                            }
                            else
                            {
                                loadingBar.dismiss();
                                Toast.makeText(GroupChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
            }
            else
            {
                loadingBar.dismiss();
                Toast.makeText(this, "Please select any file...", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void InitialiseFields()
    {
        mToolbar=findViewById(R.id.group_chat_bar_layout);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        loadingBar = new ProgressDialog(this);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar,null);
        actionBar.setCustomView(actionBarView);

        userImage = findViewById(R.id.custom_profile_image);
        userName =findViewById(R.id.custom_profile_name);
        userLastSeen = findViewById(R.id.custom_user_last_seen);

        SendMessageButton=findViewById(R.id.send_message_button);
        SendGroupFilesBtn = findViewById(R.id.send_group_files_btn);
        UserMessageInput=findViewById(R.id.input_group_message);

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = findViewById(R.id.Group_chat_list);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat CurrentDate = new SimpleDateFormat("dd-MMM-yyyy");
        currentDate = CurrentDate.format(calendar.getTime());

        SimpleDateFormat CurrentTime = new SimpleDateFormat("hh:mm a");
        currentTime = CurrentTime.format(calendar.getTime());

    }

    private void GetUserInfo()
    {

        UserRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists())
                {
                    currentUserName=dataSnapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendMessageInfoToDatabase()
    {

        String message = UserMessageInput.getText().toString();
        //GroupNameRef will go to database inside "Groups" and will create a unique key for each message
        String messageKey = GroupNameRef.push().getKey();

        if(TextUtils.isEmpty(message))
        {
            Toast.makeText(this, "Please write any message", Toast.LENGTH_SHORT).show();
        }
        else
        {
            GroupMessageKeyRef = GroupNameRef.child(messageKey);
            //using GroupMessageKeyRef we will store our message to database

            HashMap<String, Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("name", currentUserName);
            messageInfoMap.put("message", message);
            messageInfoMap.put("date", currentDate);
            messageInfoMap.put("time", currentTime);;
            messageInfoMap.put("type", "text");
            messageInfoMap.put("from",FirebaseAuth.getInstance().getCurrentUser().getUid());
            messageInfoMap.put("to", "");
            messageInfoMap.put("messageID",messageKey);

            GroupMessageKeyRef.updateChildren(messageInfoMap);

        }
    }

}
