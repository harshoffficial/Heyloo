package com.harsh.heyloo;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    private boolean isAnimationReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        splashScreen.setKeepOnScreenCondition(() -> !isAnimationReady);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottieView);

        // Set an animator update listener to detect when animation starts
        lottieAnimationView.addAnimatorUpdateListener(animation -> {
            if (animation.getAnimatedFraction() > 0) {
                isAnimationReady = true;
            }
        });

        // Listen for animation completion
        lottieAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                // Start the login activity when the animation ends
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        // Add a fade-out animation for the splash screen
        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            splashScreenView.getView().animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(splashScreenView::remove)
                    .start();
        });




    }
}