package com.harsh.heyloo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class User_Adapter extends RecyclerView.Adapter<User_Adapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private OnUserItemClickListener listener;

    public interface OnUserItemClickListener {
        void onUserItemClick(User user);
    }

    public User_Adapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    public void setOnUserItemClickListener(OnUserItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item_layout, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        
        // Set user name immediately
        holder.userName.setText(user.getUserName());
        
        // Load profile image with high quality
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            System.out.println("User_Adapter - Loading image for " + user.getUserName() + ": " + user.getProfileImageUrl());
            Picasso.get()
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.person)
                    .error(R.drawable.person)
                    .resize(200, 200) // High resolution for crisp images
                    .centerCrop()
                    .into(holder.profileImage, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            System.out.println("User_Adapter - Image loaded successfully for " + user.getUserName());
                        }

                        @Override
                        public void onError(Exception e) {
                            System.out.println("User_Adapter - Image loading failed for " + user.getUserName() + ": " + e.getMessage());
                        }
                    });
        } else {
            holder.profileImage.setImageResource(R.drawable.person);
        }

        // Clear any existing listeners first
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnTouchListener(null);
        
        // Make sure the item view is clickable
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
        holder.itemView.setFocusableInTouchMode(true);
        
        // Set both click and touch listeners for better reliability
        holder.itemView.setOnClickListener(v -> {
            System.out.println("User_Adapter - Item clicked for user: " + user.getUserName());
            if (listener != null) {
                System.out.println("User_Adapter - Calling listener.onUserItemClick");
                listener.onUserItemClick(user);
            } else {
                System.out.println("User_Adapter - Listener is null!");
            }
        });
        
        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                System.out.println("User_Adapter - Item touched for user: " + user.getUserName());
                if (listener != null) {
                    System.out.println("User_Adapter - Calling listener.onUserItemClick from touch");
                    listener.onUserItemClick(user);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setUsers(List<User> userList) {
        System.out.println("User_Adapter - setUsers called with " + userList.size() + " users");
        this.userList.clear();
        this.userList.addAll(userList);
        notifyDataSetChanged();
        System.out.println("User_Adapter - notifyDataSetChanged called");
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.imageview.ShapeableImageView profileImage;
        TextView userName;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.user_Item);
        }
    }
} 