package com.example.gestureaid;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if elderly mode is active (persistent in SharedPreferences)
        boolean elderlyModeActive = getSharedPreferences("GestureAidPrefs", MODE_PRIVATE)
                .getBoolean("elderly_mode_active", false);

        if (elderlyModeActive) {
            // Elderly mode is enabled → launch ElderlyModeActivity
            String caregiverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Always fetch the latest elderly profile in the background
            new Thread(() -> {
                Elderly elderly = AppDatabase.getInstance(this)
                        .elderlyDao()
                        .getFirstElderlyByCaregiver(caregiverId);

                if (elderly != null) {
                    runOnUiThread(() -> launchElderlyMode(elderly.id));
                } else {
                    // fallback if no elderly profile exists
                    runOnUiThread(this::launchOnboarding);
                }
            }).start();

        } else {
            // Default flow → go to onboarding / login
            launchOnboarding();
        }
    }

    private void launchElderlyMode(String elderlyId) {
        Intent intent = new Intent(this, ElderlyModeActivity.class);
        intent.putExtra("elderlyId", elderlyId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // ensures clean start
        startActivity(intent);
        finish();
    }

    private void launchOnboarding() {
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // ensures clean start
        startActivity(intent);
        finish();
    }
}
