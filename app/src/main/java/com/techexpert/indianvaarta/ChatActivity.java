package com.techexpert.indianvaarta;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.techexpert.indianvaarta.Notifications.APIService;
import com.techexpert.indianvaarta.Notifications.Client;
import com.techexpert.indianvaarta.Notifications.Data;
import com.techexpert.indianvaarta.Notifications.MyResponse;
import com.techexpert.indianvaarta.Notifications.Sender;
import com.techexpert.indianvaarta.Notifications.Token;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity
{

    String messageReceiverID, messageReceiverName, messageReceiverImage, messageSenderID;
    TextView userName, userLastSeen;
    CircleImageView userImage;

    Toolbar chatToolbar;

    ImageButton SendMessageButton, SendFilesButton;
    EditText MessageInputText;

    FirebaseAuth mAuth;
    DatabaseReference RootRef;

    String saveCurrentTime, saveCurrentDate;

    final List<Messages> messagesList = new ArrayList<>();
    LinearLayoutManager linearLayoutManager;
    MessageAdapter messageAdapter;

    String checker = "", myUrl = "";
    Uri fileUri;
    StorageTask uploadTask;

    ProgressDialog loadingBar;

    RecyclerView userMessagesList;

    APIService apiService;

    boolean notify = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser()!=null)
        {
            messageSenderID = mAuth.getCurrentUser().getUid();

                messageReceiverID = getIntent().getStringExtra("visit_user_id");
                messageReceiverName = getIntent().getStringExtra("visit_user_name");
                messageReceiverImage = getIntent().getStringExtra("user_image");
        }

        RootRef = FirebaseDatabase.getInstance().getReference();

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        InitializeControllers();

        userName.setText(messageReceiverName);

        Picasso.get().load(messageReceiverImage)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.profile_image)
                .into(userImage, new Callback() {
                    @Override
                    public void onSuccess() {
                    }
                    @Override
                    public void onError(Exception e)
                    {
                        Picasso.get().load(messageReceiverImage)
                                .placeholder(R.drawable.profile_image)
                                .into(userImage);
                    }
                });

        DisplayLastSeen();

        SendMessageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SendMessage();
            }
        });

        SendFilesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CharSequence[] options = new CharSequence[]
                        {
                                "Images",
                                "PDF File",
                                "MS Word File"
                        };

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select File");
                builder.setItems(options, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        if (i == 0)
                        {
                            checker = "image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent, "Select Image"), 5);
                        }
                        if (i == 1)
                        {
                            checker = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent, "Select PDF File"), 5);

                        }
                        if (i == 2)
                        {
                            checker = "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent, "Select Ms Word File"), 5);
                        }
                    }
                });
                builder.show();
            }
        });

        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener()
                {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s)
                    {
                        Messages messages = dataSnapshot.getValue(Messages.class);
                        messagesList.add(messages);

                        messageAdapter.notifyDataSetChanged();

                        userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s)
                    {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot)
                    {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s)
                    {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {

                    }
                });
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

//    @Override
//    protected void onDestroy() {
//        MainActivity.UpdateUserStatus("offline");
//        super.onDestroy();
//    }

    private void InitializeControllers()
    {

        chatToolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar,null);
        actionBar.setCustomView(actionBarView);


        userImage = findViewById(R.id.custom_profile_image);
        userName =findViewById(R.id.custom_profile_name);
        userLastSeen = findViewById(R.id.custom_user_last_seen);

        SendMessageButton = findViewById(R.id.send_message_btn);
        SendFilesButton = findViewById(R.id.send_files_btn);
        MessageInputText = findViewById(R.id.input_message);

        loadingBar = new ProgressDialog(this);

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        //linearLayoutManager.setStackFromEnd(true);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);


        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

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

                //putting file in Storage
                final String MessageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String MessageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

                DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID)
                        .child(messageReceiverID).push(); //it will create a key

                //each message has unique key
                final String messagePushId = userMessageKeyRef.getKey();

                //it pointing to folder of images in database and we are giving unique key for each image
                final StorageReference Filepath = storageReference.child(messagePushId + "." + checker);

                //putting file in firebase from where user can retrieve it
                Filepath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>()
                {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task)
                    {
                        if(task.isSuccessful())
                        {

                            FirebaseStorage storageReference = FirebaseStorage.getInstance();

                            StorageReference storageRef = storageReference.getReference().child("Document Files");


                            storageRef.child(messagePushId + "."+ checker).getDownloadUrl()
                                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task)
                                {
                                    String PdfUrl = task.getResult().toString();

                                    Map messageFile = new HashMap();
                                    messageFile.put("message",PdfUrl); //link to file present in Storage
                                    messageFile.put("name",fileUri.getLastPathSegment());
                                    messageFile.put("type", checker);
                                    messageFile.put("from",messageSenderID);
                                    messageFile.put("to",messageReceiverID);
                                    messageFile.put("messageID",messagePushId);
                                    messageFile.put("time",saveCurrentTime);
                                    messageFile.put("date",saveCurrentDate);


                                    Map messageBodyDetails = new HashMap();
                                    messageBodyDetails.put(MessageSenderRef + "/" + messagePushId, messageFile);
                                    messageBodyDetails.put(MessageReceiverRef + "/" + messagePushId, messageFile);

                                    RootRef.updateChildren(messageBodyDetails);

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
                                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                                                    {
                                                        @Override
                                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                                                        {

                                                            //Toast.makeText(holder.itemView.getContext(), "Image sent", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener()
                                                    {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e)
                                                        {
                                                            Toast.makeText(ChatActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                        else if(checker.equals("docx"))
                                        {
                                            final File outputFile =new File(ImageFolder,messagePushId+".docx");

                                            storageImageRef.child(messagePushId+".docx").getFile(outputFile)
                                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                                                    {
                                                        @Override
                                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                                                        {

                                                            //Toast.makeText(holder.itemView.getContext(), "Image sent", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener()
                                                    {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e)
                                                        {
                                                            Toast.makeText(ChatActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }
                                    else
                                    {
                                        Toast.makeText(ChatActivity.this, "Please grant permission for storage...", Toast.LENGTH_SHORT).show();
                                    }

                                    loadingBar.dismiss();

                                }
                            });

                        }
                    }
                }).addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
                {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot)
                    {
                        double p = (100.0*taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        loadingBar.setMessage((int) p + "%  Uploading....");
                    }
                });

            }


            else if(checker.equals("image"))
            {
                //it will create a folder in storage of firebase
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

                //reference
                final String MessageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String MessageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

                DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID)
                        .child(messageReceiverID).push(); //it will create a key

                //each message has unique key
                final String messagePushId = userMessageKeyRef.getKey();

                //it pointing to folder of images in database and we are giving unique key for each image
                final StorageReference Filepath = storageReference.child(messagePushId + ".jpg");

                //putting file inside the storage
                //fileUri contains image
                uploadTask = Filepath.putFile(fileUri);

                uploadTask.continueWithTask(new Continuation()
                {
                    @Override
                    public Object then(@NonNull Task task) throws Exception
                    {
                        if(!task.isSuccessful())
                        {
                            throw task.getException();
                        }
                        return Filepath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task)
                    {
                        if(task.isSuccessful())
                        {
                            Uri downloadUrl = task.getResult();

                            //myUrl will contain link of the image
                            myUrl = downloadUrl.toString();


                            Map messageImageBody = new HashMap();
                            messageImageBody.put("message",myUrl);
                            messageImageBody.put("name",fileUri.getLastPathSegment());
                            messageImageBody.put("type", checker);
                            messageImageBody.put("from",messageSenderID);
                            messageImageBody.put("to",messageReceiverID);
                            messageImageBody.put("messageID",messagePushId);
                            messageImageBody.put("time",saveCurrentTime);
                            messageImageBody.put("date",saveCurrentDate);


                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(MessageSenderRef + "/" + messagePushId, messageImageBody);
                            messageBodyDetails.put(MessageReceiverRef + "/" + messagePushId, messageImageBody);

                            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener()
                            {
                                @Override
                                public void onComplete(@NonNull Task task)
                                {
                                    if(task.isSuccessful())
                                    {
                                        loadingBar.dismiss();
                                        Toast.makeText(ChatActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();

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
                                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>()
                                                    {
                                                        @Override
                                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                                                        {

                                                            //Toast.makeText(holder.itemView.getContext(), "Image sent", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener()
                                                    {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e)
                                                        {
                                                            Toast.makeText(ChatActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                        else
                                        {
                                            Toast.makeText(ChatActivity.this, "Please grant permission for storage...", Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                    else
                                    {
                                        loadingBar.dismiss();
                                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                    }
                                    MessageInputText.setText("");
                                }
                            });

                        }
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

    private void DisplayLastSeen()
    {
        RootRef.child("Users").child(messageReceiverID).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.child("userState").hasChild("state"))
                {
                    String state = dataSnapshot.child("userState").child("state").getValue().toString();
                    String date = dataSnapshot.child("userState").child("date").getValue().toString();
                    String time = dataSnapshot.child("userState").child("time").getValue().toString();

                    //checking state if online or offline
                    if(state.equals("online"))
                    {
                        userLastSeen.setText("Online");
                    }
                    else if(state.equals("offline"))
                    {
                        userLastSeen.setText("Last Seen: "+ date +" "+ time);
                    }
                }
                //for those who have not updated their user profile and had just had just made the id
                else
                {
                    userLastSeen.setText("Offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void SendMessage()
    {
        notify = true;

        final String MessageText = MessageInputText.getText().toString();

        MessageInputText.setText(null);

        if(TextUtils.isEmpty(MessageText))
        {
            Toast.makeText(this, "Please write your message...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            //creating node for messages
            String MessageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String MessageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

            DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID)
                    .child(messageReceiverID).push(); //it will create a key

            //each message we will have unique key
            String messagePushId = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message", MessageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("to", messageReceiverID);
            messageTextBody.put("messageID", messagePushId);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);


            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(MessageSenderRef + "/" + messagePushId, messageTextBody);
            messageBodyDetails.put(MessageReceiverRef + "/" + messagePushId, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful())
                    {
                        Toast.makeText(ChatActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();

                        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(messageSenderID);
                        reference.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot)
                            {

                                contacts user = snapshot.getValue(contacts.class);
                                if(notify)
                                {
                                    sendNotification(messageReceiverID,user.getName(),MessageText, user.getName(), user.getImage());
                                }

                                notify = false;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });


                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
    }

    private void sendNotification(final String receiver, final String userName, final String message, final String Name, final String Image)
    {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                for(DataSnapshot snapshot1 : snapshot.getChildren())
                {
                    Token token = snapshot1.getValue(Token.class);
                    Data data = new Data(messageSenderID,R.mipmap.ic_launcher,message,
                            Name, messageReceiverID, Name, Image);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new retrofit2.Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, retrofit2.Response<MyResponse> response) {

                                    Toast.makeText(ChatActivity.this, ""+response.message(), Toast.LENGTH_SHORT).show();

                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}
