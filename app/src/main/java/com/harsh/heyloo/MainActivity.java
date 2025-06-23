package com.harsh.heyloo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageButton menuButton;
    NavigationView navigationView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private static final int UPDATE_PROFILE_REQUEST = 1001;
    
    // RecyclerView and Adapter for users
    private RecyclerView recyclerView;
    private User_Adapter userAdapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView and user list
        recyclerView = findViewById(R.id.recycler_view);
        userList = new ArrayList<>();
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new User_Adapter(this, userList);
        recyclerView.setAdapter(userAdapter);
        
        System.out.println("MainActivity - RecyclerView and adapter set up");
        
        // Set up click listeners
        userAdapter.setOnUserItemClickListener(user -> {
            System.out.println("MainActivity - User item clicked: " + user.getUserName());
            try {
                // Launch MessagingActivity with user information
                Intent intent = new Intent(MainActivity.this, MessagingActivity.class);
                intent.putExtra("selected_user_id", user.getUserId());
                intent.putExtra("selected_user_name", user.getUserName());
                intent.putExtra("selected_user_profile_image", user.getProfileImageUrl());
                intent.putExtra("selected_user_email", user.getEmail());
                System.out.println("MainActivity - Starting MessagingActivity");
                startActivity(intent);
            } catch (Exception e) {
                System.out.println("Error launching MessagingActivity: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        System.out.println("MainActivity - Click listener set on adapter");

        // Initialize drawer and menu button
        drawerLayout = findViewById(R.id.drawer_layout);
        menuButton = findViewById(R.id.menu_button);
        navigationView = findViewById(R.id.nav_view);

        // Set up menu button to open drawer
        menuButton.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END);
            }
        });

        // Set up navigation item selected listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_gallery) {
                // Update Profile item clicked
                Intent intent = new Intent(MainActivity.this, UpdateProfileActivity.class);
                startActivityForResult(intent, UPDATE_PROFILE_REQUEST);
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END);
                return true;
            } else if (id == R.id.nav_logout) {
                // Logout item clicked
                showLogoutDialog();
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END);
                return true;
            }
            // Handle other menu items here if needed
            return false;
        });

        // Load user data and update drawer header
        loadUserData();
        
        // Load all users for the RecyclerView (only once in onCreate)
        loadAllUsers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode); // Debug log
        if (requestCode == UPDATE_PROFILE_REQUEST && resultCode == RESULT_OK) {
            System.out.println("Profile update successful, refreshing drawer header"); // Debug log
            // Profile was updated successfully, refresh the drawer header
            forceRefreshDrawerHeader();
            // Also refresh the user list to show updated information
            loadAllUsers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only refresh user list if needed (e.g., after profile update)
        // loadAllUsers(); // Removed to prevent unnecessary reloading
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Get the header view
            View headerView = navigationView.getHeaderView(0);
            if (headerView == null) {
                // If header view is null, try to get it again after a short delay
                new android.os.Handler().postDelayed(this::loadUserData, 100);
                return;
            }
            
            ImageView profileImage = headerView.findViewById(R.id.profile_image);
            TextView userName = headerView.findViewById(R.id.user_name);
            TextView userEmail = headerView.findViewById(R.id.user_email);

            // Set email from Firebase Auth
            userEmail.setText(currentUser.getEmail());

            // Load user data from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Set user name
                            String name = documentSnapshot.getString("userName");
                            if (name != null && !name.isEmpty()) {
                                userName.setText(name);
                            }

                            // Set profile image
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            System.out.println("Profile Image URL from Firestore: " + profileImageUrl); // Debug log
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                System.out.println("Loading image with Picasso: " + profileImageUrl); // Debug log
                                Picasso.get()
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.person)
                                        .error(R.drawable.person)
                                        .into(profileImage);
                            } else {
                                System.out.println("Profile image URL is null or empty"); // Debug log
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error - keep default values
                        System.out.println("Failed to load user data: " + e.getMessage()); // Debug log
                    });
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    logout();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void logout() {
        // Sign out from Firebase
        auth.signOut();
        
        // Navigate back to LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void forceRefreshDrawerHeader() {
        System.out.println("forceRefreshDrawerHeader called"); // Debug log
        // Add a small delay to ensure the update is complete, then refresh
        new android.os.Handler().postDelayed(() -> {
            // Reload user data
            loadUserData();
        }, 500);
    }

    private void loadAllUsers() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            System.out.println("MainActivity - Loading all users for current user: " + currentUser.getUid());
            
            // Use get() with source parameter to prioritize cache
            db.collection("users")
                    .get(com.google.firebase.firestore.Source.CACHE)
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<User> allUsers = new ArrayList<>();
                        System.out.println("MainActivity - Found " + queryDocumentSnapshots.size() + " total users from cache");
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                user.setUserId(document.getId()); // Set the document ID as userId
                                if (!user.getUserId().equals(currentUser.getUid())) {
                                    allUsers.add(user);
                                    System.out.println("MainActivity - Added user: " + user.getUserName() + " (ID: " + user.getUserId() + ")");
                                }
                            }
                        }
                        
                        // If no users found, add a test user for debugging
                        if (allUsers.isEmpty()) {
                            System.out.println("MainActivity - No users found, adding test user");
                            User testUser = new User("test_id", "Test User", "", "test@example.com");
                            allUsers.add(testUser);
                        }
                        
                        System.out.println("MainActivity - Total users to display: " + allUsers.size());
                        // Update adapter with users
                        userAdapter.setUsers(allUsers);
                        
                        // Now try to get fresh data from server in background
                        loadFreshUsers();
                    })
                    .addOnFailureListener(e -> {
                        System.out.println("Failed to load users from cache: " + e.getMessage());
                        // Try to load from server directly
                        loadFreshUsers();
                    });
        } else {
            System.out.println("MainActivity - Current user is null, cannot load users");
        }
    }
    
    private void loadFreshUsers() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<User> allUsers = new ArrayList<>();
                        System.out.println("MainActivity - Found " + queryDocumentSnapshots.size() + " total users from server");
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                user.setUserId(document.getId());
                                if (!user.getUserId().equals(currentUser.getUid())) {
                                    allUsers.add(user);
                                }
                            }
                        }
                        
                        if (!allUsers.isEmpty()) {
                            System.out.println("MainActivity - Updating with fresh data: " + allUsers.size() + " users");
                            userAdapter.setUsers(allUsers);
                        }
                    })
                    .addOnFailureListener(e -> {
                        System.out.println("Failed to load fresh users: " + e.getMessage());
                    });
        }
    }
}