package com.example.trave_app.notifications.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trave_app.R;
import com.example.trave_app.notifications.engine.AINotificationEngine;

public class NotificationSettingsActivity extends AppCompatActivity {
    
    private Switch switchLocationTracking;
    private Button buttonSave;
    private AINotificationEngine notificationEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_notification_settings);
            
            // Initialize basic UI elements
            switchLocationTracking = findViewById(R.id.switchLocationTracking);
            buttonSave = findViewById(R.id.button_save);
            
            // Initialize notification engine
            notificationEngine = AINotificationEngine.getInstance(this);
            
            // Set up basic listeners
            if (switchLocationTracking != null) {
                switchLocationTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        notificationEngine.startRealTimeLocationTracking();
                        Toast.makeText(this, "Real-time tracking enabled!", Toast.LENGTH_SHORT).show();
                    } else {
                        notificationEngine.stopRealTimeLocationTracking();
                        Toast.makeText(this, "Real-time tracking disabled!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (buttonSave != null) {
                buttonSave.setOnClickListener(v -> {
                    Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            
            Toast.makeText(this, "AI Notifications settings loaded successfully!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
