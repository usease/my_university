package com.example.usease.myuniversity;

import android.graphics.Color;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> mMessageList;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public MessageAdapter(List<Messages> mMessageList) {
        this.mMessageList = mMessageList;
    }

    @Override
    public MessageAdapter.MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout, parent, false);

        return new MessageViewHolder(v);
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{

        private TextView messageText, messageUserName, messageTime;
        private CircleImageView profileImage;
        private ImageView messageImage;


        public MessageViewHolder(View itemView) {
            super(itemView);
            //Referencing views
            messageImage = (ImageView) itemView.findViewById(R.id.message_image_layout);
            messageText = (TextView) itemView.findViewById(R.id.message_single_message);
            messageUserName = (TextView) itemView.findViewById(R.id.message_single_name);
            messageTime = (TextView) itemView.findViewById(R.id.message_single_time);
            messageText = (TextView) itemView.findViewById(R.id.message_single_message);
            profileImage = (CircleImageView) itemView.findViewById(R.id.message_single_image);

        }
    }

    //Function is responsible for retrieving messages and setting them properly
    @Override
    public void onBindViewHolder(final MessageAdapter.MessageViewHolder holder, int position) {
            //Creating message object
            Messages m = mMessageList.get(position);
            holder.messageText.setTextColor(Color.WHITE);
            //Getting current user ID
            String currentUserID = mAuth.getCurrentUser().getUid();
            //Getting message sender user ID
            String from_user = m.getFrom();
            //Getting the type of the message
            String message_type = m.getType();

            if(message_type.equals("text")) {
                //Making the image invisible since the message type is text
                holder.messageImage.setVisibility(View.GONE);
                holder.messageText.setVisibility(View.VISIBLE);

                //Setting message text
                holder.messageText.setText(m.getMessage());

            } else {
                //Making the image visibile since the message type is image
                holder.messageImage.setVisibility(View.VISIBLE);
                holder.messageText.setVisibility(View.GONE);
                //Loading image
                Picasso.with(holder.messageImage.getContext()).load(m.getMessage()).placeholder(R.drawable.default_image).into(holder.messageImage);
            }

            //Checking who sent the message
            if(currentUserID.equals(from_user)) {

                //If the message is ours, changing layout slightly
                holder.messageText.setBackgroundResource(R.drawable.current_user_border);
                holder.messageText.setTextColor(Color.WHITE);

                FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Getting user name and image from cloud
                        String name = dataSnapshot.child("name").getValue(String.class);
                        final String thumb_image = dataSnapshot.child("thumb_image").getValue(String.class);

                        //Setting user name
                        holder.messageUserName.setText(name);
                        //Setting user image
                        //Initially, we search image locally
                        Picasso.with(holder.profileImage.getContext()).load(thumb_image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(holder.profileImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                //If Picasso successfully finds the local cache of image, do nothing
                            }
                            @Override
                            public void onError() {
                                //else, that means we dont have the local image. Thus, there is a need to download
                                Picasso.with(holder.profileImage.getContext()).load(thumb_image).placeholder(R.drawable.default_avatar).into(holder.profileImage);
                            }
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

            } else {
                //Else, that means message is not ours, thus, changing the layout back to its previous condition
                holder.messageText.setBackgroundResource(R.drawable.border);
                holder.messageText.setTextColor(Color.DKGRAY);
                //Getting user sender name and image from the cloud
                FirebaseDatabase.getInstance().getReference().child("Users").child(from_user).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Getting name and image
                        String name = dataSnapshot.child("name").getValue(String.class);
                        final String thumb_image = dataSnapshot.child("thumb_image").getValue(String.class);

                        //Setting user name
                        holder.messageUserName.setText(name);

                        //Setting user image
                        //Initially, we search image locally
                        Picasso.with(holder.profileImage.getContext()).load(thumb_image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(holder.profileImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                //If Picasso successfully finds the local cache of image, do nothing
                            }
                            @Override
                            public void onError() {
                                //else, that means we dont have the local image. Thus, there is a need to download
                                Picasso.with(holder.profileImage.getContext()).load(thumb_image).placeholder(R.drawable.default_avatar).into(holder.profileImage);
                            }
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }

            //Setting message time in proper format
            long time = m.getTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            Date date = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            String converted_date =  sdf.format(date);
            holder.messageTime.setText(converted_date);


    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }


}
