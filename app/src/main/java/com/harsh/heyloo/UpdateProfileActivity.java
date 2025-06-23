package com.harsh.heyloo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harsh.heyloo.databinding.ActivityUpdateProfileBinding;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpdateProfileActivity extends AppCompatActivity {

    ActivityUpdateProfileBinding updateProfileBinding;
    ActivityResultLauncher<String[]> permissionsResultLauncher;
    ActivityResultLauncher<Intent> photoPickerLauncher;
    ActivityResultLauncher<Intent> cropPhotoLauncher;
    
    Uri cropSelectedImage;
    String currentUserName;
    String currentUserEmail;
    String currentProfileImageUrl;
    String uploadedImageUrl;
    
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser;
    
    boolean imageControl = false;
    String userUniqueId;
    ArrayList<String> permissionsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateProfileBinding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        setContentView(updateProfileBinding.getRoot());

        // Initialize current user
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, go back to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Set up permissions
        if (Build.VERSION.SDK_INT > 33) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsList.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        } else if (Build.VERSION.SDK_INT > 32) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        registerActivityForMultiplePermissions();
        registerActivityForPhotoPicker();
        registerActivityForCropPhoto();

        // Load current user data
        loadCurrentUserData();

        // Set up click listeners
        updateProfileBinding.profile.setOnClickListener(v -> {
            if (hasPermissions()) {
                photoPicker();
            } else {
                shouldShowPermission();
            }
        });

        updateProfileBinding.signUpButton.setOnClickListener(v -> {
            updateProfile();
        });

    }

    private void loadCurrentUserData() {
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("userName");
                        currentUserEmail = documentSnapshot.getString("userEmail");
                        currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Set current data in UI
                        updateProfileBinding.usernameInput.setText(currentUserName);
                        // Note: Email is typically not editable, so we might not want to show it in an editable field

                        // Load current profile image
                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            Picasso.get()
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.person)
                                    .error(R.drawable.person)
                                    .into(updateProfileBinding.profile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(updateProfileBinding.getRoot(), "Failed to load user data: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                });
    }

    private void updateProfile() {
        String newName = updateProfileBinding.usernameInput.getText().toString().trim();

        if (newName.isEmpty()) {
            Snackbar.make(updateProfileBinding.getRoot(), "Please fill in the name field", Snackbar.LENGTH_SHORT).show();
            return;
        }

        updateProfileBinding.signUpButton.setEnabled(false);
        updateProfileBinding.progressBarSignUp.setVisibility(View.VISIBLE);

        // If image is selected, upload to Cloudinary first (EXACT SAME AS SIGNUP)
        if (imageControl && cropSelectedImage != null) {
            uploadProfileImage(newName);
        } else {
            // No image change, just update text data
            updateUserData(newName, currentProfileImageUrl);
        }
    }

    public void uploadProfileImage(String newName) {
        if (auth.getCurrentUser() != null) {
            userUniqueId = auth.getCurrentUser().getUid();

            if (imageControl && cropSelectedImage != null) {
                updateProfileBinding.progressBarSignUp.setVisibility(View.VISIBLE);
                
                // Run upload in background thread
                new Thread(() -> {
                    try {
                        // Convert URI to File
                        File imageFile = new File(cropSelectedImage.getPath());
                        
                        // Generate signature for signed upload
                        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                        String publicId = "profile_images/" + userUniqueId;
                        String apiSecret = "7a0rWVE158DZArTU0vxrAcC0zng";
                        
                        // Create signature string
                        String signatureString = "public_id=" + publicId + "&timestamp=" + timestamp;
                        String signature = generateSignature(signatureString, apiSecret);
                        
                        OkHttpClient client = new OkHttpClient.Builder()
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .writeTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS)
                                .build();

                        MediaType mediaType = MediaType.parse("image/*");
                        RequestBody requestBody = RequestBody.create(mediaType, imageFile);

                        MultipartBody.Builder builder = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("file", imageFile.getName(), requestBody)
                                .addFormDataPart("public_id", publicId)
                                .addFormDataPart("api_key", "969396162765785")
                                .addFormDataPart("timestamp", timestamp)
                                .addFormDataPart("signature", signature);

                        Request request = new Request.Builder()
                                .url("https://api.cloudinary.com/v1_1/dui9ioqwm/image/upload")
                                .post(builder.build())
                                .build();

                        Response response = client.newCall(request).execute();

                        if (response.isSuccessful() && response.body() != null) {
                            String result = response.body().string();
                            
                            // Parse JSON response to extract the image URL
                            try {
                                JSONObject jsonResponse = new JSONObject(result);
                                uploadedImageUrl = jsonResponse.getString("secure_url");
                            } catch (JSONException e) {
                                uploadedImageUrl = ""; // Set empty if parsing fails
                            }
                            
                            runOnUiThread(() -> {
                                updateProfileBinding.progressBarSignUp.setVisibility(View.GONE);
                                if (!uploadedImageUrl.isEmpty()) {
                                    Snackbar.make(updateProfileBinding.getRoot(), "Profile image uploaded successfully", Snackbar.LENGTH_SHORT).show();
                                    updateUserData(newName, uploadedImageUrl);
                                } else {
                                    Snackbar.make(updateProfileBinding.getRoot(), "Failed to upload image", Snackbar.LENGTH_SHORT).show();
                                    updateProfileBinding.signUpButton.setEnabled(true);
                                }
                            });
                        } else {
                            String errorBody = "";
                            if (response.body() != null) {
                                errorBody = response.body().string();
                            }
                            runOnUiThread(() -> {
                                updateProfileBinding.progressBarSignUp.setVisibility(View.GONE);
                                Snackbar.make(updateProfileBinding.getRoot(), "Profile image upload failed: " + response.code(), Snackbar.LENGTH_SHORT).show();
                                updateProfileBinding.signUpButton.setEnabled(true);
                            });
                        }
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            updateProfileBinding.progressBarSignUp.setVisibility(View.GONE);
                            Snackbar.make(updateProfileBinding.getRoot(), "Profile image upload failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        });
                    }
                }).start();
                
            } else {
                updateProfileBinding.progressBarSignUp.setVisibility(View.GONE);
                Snackbar.make(updateProfileBinding.getRoot(), "Please select a profile image", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUserData(String newName, String profileImageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("userName", newName);
        updates.put("profileImageUrl", profileImageUrl);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Snackbar.make(updateProfileBinding.getRoot(), "Profile updated successfully!", Snackbar.LENGTH_SHORT).show();
                    updateProfileBinding.signUpButton.setEnabled(true);
                    updateProfileBinding.progressBarSignUp.setVisibility(View.GONE);
                    
                    // Set result to indicate successful update
                    setResult(RESULT_OK);
                    
                    // Go back to MainActivity
                    finish();
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(updateProfileBinding.getRoot(), "Failed to update profile: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    updateProfileBinding.signUpButton.setEnabled(true);
                    updateProfileBinding.progressBarSignUp.setVisibility(View.GONE);
                });
    }

    // Permission and image picker methods (same as SignUpActivity)
    public void registerActivityForMultiplePermissions() {
        permissionsResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allPermissionsGranted = true;
            for (Boolean isGranted : result.values()) {
                if (!isGranted) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                photoPicker();
            } else {
                Snackbar.make(updateProfileBinding.getRoot(), "Permissions required to select image", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    public void photoPicker() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerLauncher.launch(photoPickerIntent);
    }

    public void registerActivityForPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    Uri selectedImageUri = data.getData();
                    cropSelectedImage(selectedImageUri);
                }
            }
        });
    }

    public void registerActivityForCropPhoto() {
        cropPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                // Handle the cropped image result
                Uri croppedImageUri = UCrop.getOutput(result.getData());
                if (croppedImageUri != null) {
                    // Store the cropped image URI
                    cropSelectedImage = croppedImageUri;
                    
                    // Use the cropped image URI as needed
                    Picasso.get()
                            .load(croppedImageUri)
                            .into(updateProfileBinding.profile);
                    imageControl = true; // Set the flag to indicate that an image has been selected
                }
            } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                // Handle any errors that occurred during cropping
                Throwable cropError = UCrop.getError(result.getData());
                if (cropError != null) {
                    // Show error message or handle it accordingly
                    Snackbar.make(updateProfileBinding.getRoot(), "Image cropping failed: " + cropError.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void cropSelectedImage(Uri imageUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped" + System.currentTimeMillis()));
        Intent cropIntent = UCrop.of(imageUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(500, 500)
                .getIntent(this);
        cropPhotoLauncher.launch(cropIntent);
    }

    public void shouldShowPermission() {
        ArrayList<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissionsList) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                deniedPermissions.add(permission);
            }
        }
        if (!deniedPermissions.isEmpty()) {
            Snackbar.make(updateProfileBinding.getRoot(), "Please grant the required permissions to continue using the app.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Grant Permissions", v -> {
                        permissionsResultLauncher.launch(deniedPermissions.toArray(new String[0]));
                    })
                    .show();
        } else {
            permissionsResultLauncher.launch(permissionsList.toArray(new String[0]));
        }
    }

    public boolean hasPermissions() {
        for (String permission : permissionsList) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    // Generate SHA-1 signature for Cloudinary signed uploads
    private String generateSignature(String params, String apiSecret) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest((params + apiSecret).getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }
}