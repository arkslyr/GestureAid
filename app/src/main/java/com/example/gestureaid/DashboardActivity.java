package com.example.gestureaid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private TextView welcomeText;
    private MaterialButton elderlyProfileBtn, gestureSetupBtn, logoutBtn, elderlyModeBtn, viewGesturesBtn, physicalButtonSetupBtn;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Bind UI
        welcomeText = findViewById(R.id.welcomeText);
        elderlyProfileBtn = findViewById(R.id.elderlyProfileBtn);
        gestureSetupBtn = findViewById(R.id.gestureSetupBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        elderlyModeBtn = findViewById(R.id.elderlyModeBtn);
        viewGesturesBtn = findViewById(R.id.viewGesturesBtn);
        physicalButtonSetupBtn = findViewById(R.id.physicalButtonSetupBtn);

        firestore = FirebaseFirestore.getInstance();

        loadCaregiverProfile();

        // Elderly Profile
        elderlyProfileBtn.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ElderlyProfileActivity.class)));

        // Gesture Setup (touchscreen)
        gestureSetupBtn.setOnClickListener(v -> {
            fetchElderlyFromFirestore(elderlyId -> {
                if (elderlyId != null) {
                    Intent intent = new Intent(DashboardActivity.this, GestureSetupActivity.class);
                    intent.putExtra("elderlyId", elderlyId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "No elderly profile found. Please create one first.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // View Gestures
        viewGesturesBtn.setOnClickListener(v -> {
            fetchElderlyFromFirestore(elderlyId -> {
                if (elderlyId != null) {
                    Intent intent = new Intent(DashboardActivity.this, ViewGesturesActivity.class);
                    intent.putExtra("elderlyId", elderlyId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "No elderly profile found.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Physical Button Gestures Setup
        physicalButtonSetupBtn.setOnClickListener(v -> {
            fetchElderlyFromFirestore(elderlyId -> {
                if (elderlyId != null) {
                    Intent intent = new Intent(DashboardActivity.this, PhysicalButtonSetupActivity.class);
                    intent.putExtra("elderlyId", elderlyId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "No elderly profile found. Please create one first.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Elderly Mode (PIN)
        elderlyModeBtn.setOnClickListener(v -> {
            fetchElderlyFromFirestore(elderlyId -> {
                if (elderlyId != null) {
                    DocumentReference elderlyDoc = firestore.collection("elderly").document(elderlyId);
                    elderlyDoc.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedPin = documentSnapshot.getString("pin");
                            final EditText pinInput = new EditText(DashboardActivity.this);
                            pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

                            if (storedPin == null || storedPin.isEmpty()) {
                                new androidx.appcompat.app.AlertDialog.Builder(DashboardActivity.this)
                                        .setTitle("Set PIN")
                                        .setMessage("Set a PIN for Elderly Mode")
                                        .setView(pinInput)
                                        .setPositiveButton("Save", (dialog, which) -> {
                                            String newPin = pinInput.getText().toString().trim();
                                            if (newPin.length() >= 4) {
                                                elderlyDoc.update("pin", newPin)
                                                        .addOnSuccessListener(aVoid -> activateElderlyMode(elderlyId))
                                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to save PIN", Toast.LENGTH_SHORT).show());
                                            } else {
                                                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            } else {
                                new androidx.appcompat.app.AlertDialog.Builder(DashboardActivity.this)
                                        .setTitle("Enter PIN")
                                        .setMessage("Please enter the PIN to activate Elderly Mode")
                                        .setView(pinInput)
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            String enteredPin = pinInput.getText().toString().trim();
                                            if (enteredPin.equals(storedPin)) {
                                                activateElderlyMode(elderlyId);
                                            } else {
                                                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            }
                        } else {
                            Toast.makeText(this, "Elderly profile not found", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch elderly profile", Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(this, "No elderly profile found. Please create one first.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Logout
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            getSharedPreferences("GestureAidPrefs", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void activateElderlyMode(String elderlyId) {
        getSharedPreferences("GestureAidPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("elderly_mode_active", true)
                .apply();

        Intent intent = new Intent(DashboardActivity.this, ElderlyModeActivity.class);
        intent.putExtra("elderlyId", elderlyId);
        startActivity(intent);
        finish();
    }

    private void loadCaregiverProfile() {
        String caregiverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("caregivers").document(caregiverId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        welcomeText.setText("Welcome, " + name + "!");
                    } else {
                        welcomeText.setText("Welcome, Caregiver!");
                    }
                });
    }

    private void fetchElderlyFromFirestore(ElderlyCallback callback) {
        String caregiverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("elderly")
                .whereEqualTo("caregiverId", caregiverId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        callback.onResult(doc.getId());
                    } else callback.onResult(null);
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    interface ElderlyCallback {
        void onResult(String elderlyId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean elderlyModeActive = getSharedPreferences("GestureAidPrefs", MODE_PRIVATE)
                .getBoolean("elderly_mode_active", false);

        if (elderlyModeActive) {
            fetchElderlyFromFirestore(elderlyId -> {
                if (elderlyId != null) {
                    Intent intent = new Intent(DashboardActivity.this, ElderlyModeActivity.class);
                    intent.putExtra("elderlyId", elderlyId);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
}
