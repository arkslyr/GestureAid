package com.example.gestureaid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    MaterialButton startButton;
    ImageView appIcon;
    TextView appName, tagline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Bind UI elements
        startButton = findViewById(R.id.startButton);
        appIcon = findViewById(R.id.appIcon);
        appName = findViewById(R.id.appName);
        tagline = findViewById(R.id.tagline);

        // Handle "Get Started" button click
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Redirect to LoginActivity (change if your next screen is different)
                Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close onboarding so user can't go back to it
            }
        });
    }
}
