package com.example.gestureaid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PhysicalButtonSetupActivity extends AppCompatActivity {

    private Spinner spinnerVolumeUpLong, spinnerVolumeDownLong;
    private MaterialButton saveButtonConfigBtn;
    private FirebaseFirestore firestore;

    private String elderlyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_physical_button_setup);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Get Elderly ID from intent
        elderlyId = getIntent().getStringExtra("elderlyId");
        if (elderlyId == null) {
            Toast.makeText(this, "No elderly profile found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind UI
        spinnerVolumeUpLong = findViewById(R.id.spinnerVolumeUpLong);
        spinnerVolumeDownLong = findViewById(R.id.spinnerVolumeDownLong);
        saveButtonConfigBtn = findViewById(R.id.saveButtonConfigBtn);

        // Load existing config from Firestore if exists
        loadExistingButtonConfig();

        // Save button click
        saveButtonConfigBtn.setOnClickListener(v -> saveButtonConfiguration());
    }

    private void loadExistingButtonConfig() {
        DocumentReference docRef = firestore.collection("elderly").document(elderlyId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> buttonConfig = (Map<String, Object>) documentSnapshot.get("physicalButtonConfig");
                if (buttonConfig != null) {
                    String volUp = (String) buttonConfig.get("volumeUpLong");
                    String volDown = (String) buttonConfig.get("volumeDownLong");

                    setSpinnerSelection(spinnerVolumeUpLong, volUp);
                    setSpinnerSelection(spinnerVolumeDownLong, volDown);
                }
            }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void saveButtonConfiguration() {
        String volUpAction = spinnerVolumeUpLong.getSelectedItem().toString();
        String volDownAction = spinnerVolumeDownLong.getSelectedItem().toString();

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("volumeUpLong", volUpAction);
        configMap.put("volumeDownLong", volDownAction);

        firestore.collection("elderly").document(elderlyId)
                .update("physicalButtonConfig", configMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Button configuration saved!", Toast.LENGTH_SHORT).show();
                    finish(); // go back to dashboard
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save configuration", Toast.LENGTH_SHORT).show());
    }
}
