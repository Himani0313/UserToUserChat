package com.example.geniusplaza.usertouserchat;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geniusplaza.usertouserchat.Adapters.ConversationAdapter;
import com.example.geniusplaza.usertouserchat.Model.InboxObj;
import com.example.geniusplaza.usertouserchat.Model.Message;
import com.example.geniusplaza.usertouserchat.Model.User;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pkmmte.view.CircularImageView;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import es.dmoral.toasty.Toasty;

public class ChatActivity extends AppCompatActivity {

    private CircularImageView receiverProfilePic;
    private TextView receiverName;
    private User receivingUser;
    private EditText textMsgContent;
    private ImageView sendtextMsg,sendImgMsg;
    private RecyclerView chatRecyclerView;
    private SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private DatabaseReference mDatabase= FirebaseDatabase.getInstance().getReference();
    private static final int RESULT_LOAD_IMG=1;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private ArrayList<Message> conversation= new ArrayList<>();
    private ConversationAdapter conversationAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private GoogleApiClient mGoogleApiClient;
    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        receiverProfilePic= (CircularImageView) findViewById(R.id.imageViewReceiverProfilePic);
        receiverName= (TextView) findViewById(R.id.textViewReceiverName);
        textMsgContent= (EditText) findViewById(R.id.editTextTextMsg);
        sendtextMsg= (ImageView) findViewById(R.id.imageViewSendTextMsg);
        sendImgMsg= (ImageView) findViewById(R.id.ImageViewOpenGallery);
        chatRecyclerView= (RecyclerView) findViewById(R.id.chatRecyclerView);
        linearLayout =(LinearLayout)findViewById(R.id.linearLayoutChat);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        chatRecyclerView.setLayoutManager(layoutManager);
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(chatRecyclerView);

        textMsgContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromInputMethod(textMsgContent.getWindowToken(),0);
            }
        });

        if (getIntent().getExtras()!=null){
            receivingUser= (User) getIntent().getExtras().getSerializable("receivingUser");
            receiverName.setText(receivingUser.getFirstName());
            if (receivingUser.getUserPicUrl()!=null && !receivingUser.getUserPicUrl().isEmpty()){
                Picasso.with(getApplicationContext()).load(receivingUser.getUserPicUrl()).into(receiverProfilePic);
            }
        }

        sendtextMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!textMsgContent.getText().toString().isEmpty() && textMsgContent.getText().toString()!=null){
                    Message message=new Message();
                    message.setMsgContent(textMsgContent.getText().toString());
                    message.setImgMsgUrl("");
                    message.setMsgTimeStamp(dateFormat.format(new Date()));
                    message.setSenderId(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    message.setReceiverId(receivingUser.getUid());

                    String key=mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child("chatroom").child(receivingUser.getUid()).child("messages").push().getKey();

                    message.setMsgKey(key);
                    Log.d("demo","msgObj"+message.toString());
                    //putting msg in sender child
                    mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child("chatroom").child(receivingUser.getUid()).child("messages").child(key).setValue(message);
                    //putting msg in receiver child
                    mDatabase.child("users").child(receivingUser.getUid()).child("chatroom")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("messages").child(key).setValue(message);
                    //set inboxObj to check is user has read the msg or not
                    InboxObj inboxObj=new InboxObj();
                    inboxObj.setLastMsg(message);
                    inboxObj.setIslastMsgSeen(false);
                    inboxObj.setReceiverID(receivingUser.getUid());
                    inboxObj.setSenderID(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    mDatabase.child("users").child(receivingUser.getUid()).child("inboxObjs").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(inboxObj);

                    inboxObj.setIslastMsgSeen(true);
                    mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("inboxObjs").child(receivingUser.getUid()).setValue(inboxObj);

                    textMsgContent.setText("");
                }else {
                    Toasty.warning(getApplicationContext(),"Please enter your message",Toast.LENGTH_SHORT, true).show();
                }
            }
        });

        sendImgMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
            }
        });

        mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("chatroom").child(receivingUser.getUid()).child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                conversation.clear();
                Message msg=new Message();
                InboxObj inboxObj= new InboxObj();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    Message message=ds.getValue(Message.class);
                    conversation.add(message);
                    Log.d("demo","conversation List"+conversation.toString());
                }
//                if(conversation.size() == 0){
//                    msg.setImgMsgUrl("");
//                    msg.setMsgContent("");
//                    msg.setReceiverId(receivingUser.getUid());
//                    msg.setSenderId(FirebaseAuth.getInstance().getCurrentUser().getUid());
//                    msg.setMsgTimeStamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
//                    msg.setMsgKey("");
//                }else{
//                    msg = conversation.get(conversation.size()-1);
//                }
//
//                inboxObj.setLastMsg(msg);
//                inboxObj.setIslastMsgSeen(true);
//                inboxObj.setReceiverID(receivingUser.getUid());
//
//                mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("inboxObjs").child(receivingUser.getUid()).setValue(inboxObj);

                showConversation();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // When an Image is picked
        if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                && null != data) {
            // Get the Image from data
            Uri url = data.getData();
            //save photo to firebase storage
            StorageReference photosRef= storage.getReference("imageMessages/" + url.getLastPathSegment());

            UploadTask uploadTask=photosRef.putFile(url);
            uploadTask.addOnProgressListener(ChatActivity.this, new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Toast.makeText(getApplicationContext(),"Upload is " + progress + "% done",Toast.LENGTH_SHORT).show();
                }
            });
            uploadTask.addOnSuccessListener(ChatActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl= taskSnapshot.getDownloadUrl();
                    Message message=new Message();
                    if (!textMsgContent.getText().toString().isEmpty() && textMsgContent.getText().toString()!=null){
                        message.setMsgContent(textMsgContent.getText().toString());
                    }else {
                        message.setMsgContent("");
                    }
                    message.setImgMsgUrl(downloadUrl.toString());
                    message.setMsgTimeStamp(dateFormat.format(new Date()));
                    message.setSenderId(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    message.setReceiverId(receivingUser.getUid());

                    String key=mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child("chatroom").child(receivingUser.getUid()).child("messages").push().getKey();

                    message.setMsgKey(key);
                    Log.d("demo","msgObj"+message.toString());
                    //putting msg in sender child
                    mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child("chatroom").child(receivingUser.getUid()).child("messages").child(key).setValue(message);
                    //putting msg in receiver child
                    mDatabase.child("users").child(receivingUser.getUid()).child("chatroom")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("messages").child(key).setValue(message);
                    //set inboxObj to check is user has read the msg or not
                    InboxObj inboxObj=new InboxObj();
                    inboxObj.setLastMsg(message);
                    inboxObj.setIslastMsgSeen(false);
                    inboxObj.setReceiverID(receivingUser.getUid());
                    inboxObj.setSenderID(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    mDatabase.child("users").child(receivingUser.getUid()).child("inboxObjs").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(inboxObj);

                    inboxObj.setIslastMsgSeen(true);
                    mDatabase.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("inboxObjs").child(receivingUser.getUid()).setValue(inboxObj);


                    textMsgContent.setText("");
                }
            });

            uploadTask.addOnFailureListener(ChatActivity.this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toasty.error(getApplicationContext(),"Image Sending Failed..Please try again",Toast.LENGTH_SHORT, true).show();
                }
            });
        }
    }

    public void showConversation(){
        if (conversation!=null && !conversation.isEmpty()){
            Log.d("demo","showconvo not empty"+conversation.toString());
            conversationAdapter=new ConversationAdapter(ChatActivity.this,conversation);
            chatRecyclerView.setAdapter(conversationAdapter);
            conversationAdapter.notifyDataSetChanged();

        }else {
            Log.d("demo","inside showconvo empty");
            Toasty.info(getApplicationContext(),"Send Message/Image to start conversation",Toast.LENGTH_SHORT, true).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_option_view_profile:
                Intent intent= new Intent(this,ProfileDetailsActivity.class);
                startActivity(intent);
                return true;
            case R.id.item_option_logout:
                FirebaseAuth.getInstance().signOut();
                Intent intent1=new Intent(this, MainActivity.class);
                startActivity(intent1);
                finish();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent=new Intent(ChatActivity.this,UserPageActivity.class);
        startActivity(intent);
        finish();
    }


}

