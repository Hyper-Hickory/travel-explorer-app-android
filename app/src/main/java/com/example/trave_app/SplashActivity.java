package com.example.trave_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find views
        ImageView logoImage = findViewById(R.id.logoImage);
        TextView appTitle = findViewById(R.id.appTitle);
        TextView appSubtitle = findViewById(R.id.appSubtitle);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);

        // Apply animations
        logoImage.startAnimation(fadeIn);
        appTitle.startAnimation(slideUp);
        appSubtitle.startAnimation(slideUp);

        // Start pulse animation after initial animations
        new Handler().postDelayed(() -> {
            logoImage.startAnimation(pulse);
        }, 1000);

        // Navigate to login activity after splash duration
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
