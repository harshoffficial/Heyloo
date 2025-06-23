package com.harsh.heyloo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harsh.heyloo.databinding.ActivitySignUpBinding;
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

public class SignUpActivity extends AppCompatActivity {

    ActivitySignUpBinding signUpBinding;
    ActivityResultLauncher<String[]> permissionsResultLauncher;
    int permissionDeniedCount = 0;
    ArrayList<String> permissionsList = new ArrayList<>();
    ActivityResultLauncher<Intent> photoPickerLauncher;
    ActivityResultLauncher<Intent> cropPhotoLauncher;

    Uri cropSelectedImage; // This will hold the URI of the cropped image
    String userName;
    String userEmail;
    String userPassword;
    FirebaseAuth auth= FirebaseAuth.getInstance();
    boolean imageControl = false;
    String userUniqueId;
    String uploadedImageUrl; // This will store the Cloudinary URL
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signUpBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(signUpBinding.getRoot());

        if (Build.VERSION.SDK_INT > 33){
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

        signUpBinding.profile.setOnClickListener(v -> {
            // Handle profile image click
            if (hasPermissions()) {
                photoPicker();
            } else {
                shouldshowpermission();
            }

        });
        signUpBinding.signUpButton.setOnClickListener(v -> {
            // Handle sign-up button click
            createNewUser();
        });

        // Add back button functionality
        signUpBinding.back.setOnClickListener(v -> finish());

    }

    public void createNewUser() {
        // Logic to create a new user
        // This method can be called when the user submits the sign-up form
        // You can access the profile image URI from the ImageView or any other data you need
        userName = signUpBinding.usernameInput.getText().toString().trim();
        userEmail = signUpBinding.emailInput.getText().toString().trim();
        userPassword = signUpBinding.passwordInput.getText().toString().trim();

        if (userName.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty()) {
            Snackbar.make(signUpBinding.getRoot(), "Please fill in all fields", Snackbar.LENGTH_SHORT).show();
            return;
        }
        else {
            signUpBinding.signUpButton.setEnabled(false);
            signUpBinding.progressBarSignUp.setVisibility(android.view.View.VISIBLE);

            auth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // User created successfully
                            uploadProfileImage(); // Call the method to upload the profile image
                            Snackbar.make(signUpBinding.getRoot(), "User created successfully", Snackbar.LENGTH_SHORT).show();
                            // You can navigate to the next screen or perform any other action
                        } else {
                            // User creation failed
                            Snackbar.make(signUpBinding.getRoot(), "User creation failed: " + task.getException().getMessage(), Snackbar.LENGTH_SHORT).show();

                        signUpBinding.signUpButton.setEnabled(true);
                        signUpBinding.progressBarSignUp.setVisibility(android.view.View.GONE);
                        }
                    });
        }

    }

    public void uploadProfileImage() {
        if (auth.getCurrentUser() != null) {
            userUniqueId = auth.getCurrentUser().getUid();

            if (imageControl && cropSelectedImage != null) {
                signUpBinding.progressBarSignUp.setVisibility(android.view.View.VISIBLE);
                
                // Run upload in background thread
                new Thread(() -> {
                    try {
                        // Convert URI to File
                        File imageFile = new File(cropSelectedImage.getPath());
                        
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
                                .addFormDataPart("public_id", "profile_images/" + userUniqueId)
                                .addFormDataPart("upload_preset", "unsigned_preset");

                        Request request = new Request.Builder()
                                .url("https://api.cloudinary.com/v1_1/dui9ioqwm/image/upload")
                                .post(builder.build())
                                .build();

                        Response response = client.newCall(request).execute();

                        if (response.isSuccessful() && response.body() != null) {
                            String result = response.body().string();
                            System.out.println("Raw Cloudinary Response: " + result); // Debug log
                            
                            // Parse JSON response to extract the image URL
                            try {
                                JSONObject jsonResponse = new JSONObject(result);
                                uploadedImageUrl = jsonResponse.getString("secure_url");
                                System.out.println("Extracted Cloudinary URL: " + uploadedImageUrl); // Debug log
                            } catch (JSONException e) {
                                System.out.println("JSON Parse Error: " + e.getMessage()); // Debug log
                                System.out.println("Full response: " + result); // Debug log
                                uploadedImageUrl = ""; // Set empty if parsing fails
                            }
                            
                            runOnUiThread(() -> {
                                signUpBinding.progressBarSignUp.setVisibility(android.view.View.GONE);
                                if (!uploadedImageUrl.isEmpty()) {
                                    Snackbar.make(signUpBinding.getRoot(), "Profile image uploaded successfully", Snackbar.LENGTH_SHORT).show();
                                    saveUserInfoToDatabase();
                                } else {
                                    Snackbar.make(signUpBinding.getRoot(), "Failed to upload image", Snackbar.LENGTH_SHORT).show();
                                    signUpBinding.signUpButton.setEnabled(true);
                                }
                            });
                        } else {
                            String errorBody = "";
                            if (response.body() != null) {
                                errorBody = response.body().string();
                            }
                            System.out.println("Upload failed with code: " + response.code() + " Error: " + errorBody); // Debug log
                            runOnUiThread(() -> {
                                signUpBinding.progressBarSignUp.setVisibility(android.view.View.GONE);
                                Snackbar.make(signUpBinding.getRoot(), "Profile image upload failed: " + response.code(), Snackbar.LENGTH_SHORT).show();
                                signUpBinding.signUpButton.setEnabled(true);
                            });
                        }
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            signUpBinding.progressBarSignUp.setVisibility(android.view.View.GONE);
                            Snackbar.make(signUpBinding.getRoot(), "Profile image upload failed: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        });
                    }
                }).start();
                
            } else {
                signUpBinding.progressBarSignUp.setVisibility(android.view.View.GONE);
                Snackbar.make(signUpBinding.getRoot(), "Please select a profile image", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    public void saveUserInfoToDatabase() {
        if (auth.getCurrentUser() != null) {
            // Create a user data map
            Map<String, Object> userData = new HashMap<>();
            userData.put("userName", userName);
            userData.put("userEmail", userEmail);
            userData.put("profileImageUrl", uploadedImageUrl != null ? uploadedImageUrl : "");
            userData.put("createdAt", System.currentTimeMillis());
            userData.put("userId", auth.getCurrentUser().getUid());

            System.out.println("Saving to Firestore - Profile Image URL: " + uploadedImageUrl); // Debug log

            // Save to Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        // User data saved successfully
                        System.out.println("User data saved successfully to Firestore"); // Debug log
                        Snackbar.make(signUpBinding.getRoot(), "Account created successfully!", Snackbar.LENGTH_SHORT).show();
                        
                        // Navigate to MainActivity
                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Failed to save user data
                        System.out.println("Failed to save user data: " + e.getMessage()); // Debug log
                        Snackbar.make(signUpBinding.getRoot(), "Failed to save user data: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        signUpBinding.signUpButton.setEnabled(true);
                        signUpBinding.progressBarSignUp.setVisibility(android.view.View.GONE);
                    });
        }
    }

    public void registerActivityForMultiplePermissions(){

        permissionsResultLauncher= registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),result->{

            boolean allPermissionsGranted = true;
            for (Boolean isGranted : result.values()) {
                if (!isGranted) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                photoPicker();
            }
            else {
                permissionDeniedCount++;
                if (permissionDeniedCount < 2) {
                    shouldshowpermission();

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                    builder.setTitle("Permissions Required")
                            .setMessage("Please grant the required permissions to continue using the app.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // User clicked Cancel, you can close the dialog
                                dialog.dismiss();
                            })
                            .show();
                }

            }

        });

    }



    public void photoPicker() {
        // Launch the photo picker intent
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerLauncher.launch(photoPickerIntent);
    }


    public void registerActivityForPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    Uri selectedImageUri = data.getData();
                    // Handle the selected image URI
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
                            .into(signUpBinding.profile);
                    imageControl = true; // Set the flag to indicate that an image has been selected
                }
            } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                // Handle any errors that occurred during cropping
                Throwable cropError = UCrop.getError(result.getData());
                if (cropError != null) {
                    // Show error message or handle it accordingly
                    Snackbar.make(signUpBinding.getRoot(), "Image cropping failed: " + cropError.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void cropSelectedImage(Uri imageUri) {

        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped"+ System.currentTimeMillis()));
        Intent cropIntent = UCrop.of(imageUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(500, 500)
                .getIntent(this);
        cropPhotoLauncher.launch(cropIntent);
    }
    public void shouldshowpermission() {
        // Check if the permissions are already granted
        ArrayList<String> deniedPermissions = new ArrayList<>();

        for (String permission : permissionsList) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                deniedPermissions.add(permission);
            }
        }
        if(!deniedPermissions.isEmpty()) {
            Snackbar.make(signUpBinding.getRoot(), "Please grant the required permissions to continue using the app.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Grant Permissions", v -> {
                        // Request the denied permissions
                        permissionsResultLauncher.launch(deniedPermissions.toArray(new String[0]));
                    })
                    .show();

        } else {
            // If no permissions are denied, proceed with the photo picker
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
}