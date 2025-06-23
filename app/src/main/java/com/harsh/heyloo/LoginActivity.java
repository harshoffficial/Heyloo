package com.harsh.heyloo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.harsh.heyloo.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView forgotPassword;
    private TextView signUp;
    ActivityLoginBinding loginBinding;
    FirebaseAuth auth= FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(loginBinding.getRoot());

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        signUp = findViewById(R.id.signUp);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // User is already logged in, redirect to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        // Set up login button click listener
        loginBinding.loginButton.setOnClickListener(v -> handleLogin());

        // Set up MotionLayout transition listener
        MotionLayout motionLayout = findViewById(R.id.main);
        motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {}

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {}

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                // Enable login button when animation completes
                loginButton.setEnabled(true);
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {}
        });

        // Initially disable login button until animation completes
        loginButton.setEnabled(false);



        loginBinding.signUp.setOnClickListener(v -> {
            // Redirect to SignUpActivity
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });



    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        loginButton.setEnabled(false);
        
        // Attempt to sign in with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    FirebaseUser user = auth.getCurrentUser();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loginButton.setEnabled(true);
                }
            });
    }
}