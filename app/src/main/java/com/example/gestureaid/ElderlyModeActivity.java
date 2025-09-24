package com.example.gestureaid;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElderlyModeActivity extends AppCompatActivity implements GestureOverlayView.OnGesturePerformedListener {

    private GestureOverlayView gestureOverlayView;
    private ImageButton exitLockBtn;

    private String elderlyId;
    private AppDatabase localDb;
    private FirebaseFirestore firestore;

    // For physical button actions
    private Map<String, String> buttonConfig = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elderly_mode);

        gestureOverlayView = findViewById(R.id.gestureOverlayView);
        exitLockBtn = findViewById(R.id.exitLockBtn);

        elderlyId = getIntent().getStringExtra("elderlyId");
        if (elderlyId == null) {
            Toast.makeText(this, "Error: Elderly ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        localDb = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        // Ensure volume keys come to this Activity instead of system volume control
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Load physical button config from Firestore
        loadButtonConfigFromFirestore();

        gestureOverlayView.addOnGesturePerformedListener(this);

        exitLockBtn.setOnClickListener(v -> askExitPin());
    }

    // -------------------- Gesture Handling --------------------

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        AsyncTask.execute(() -> {
            List<GestureEntity> gestures = localDb.gestureDao().getGesturesByElderlyId(elderlyId);
            boolean matched = false;

            for (GestureEntity ge : gestures) {
                Gesture savedGesture = deserializeGesture(ge.gestureData);
                if (areGesturesSimilar(savedGesture, gesture)) {
                    matched = true;
                    runOnUiThread(() -> handleAction(ge.actionType, ge.actionData));
                    break;
                }
            }

            if (!matched) {
                runOnUiThread(() ->
                        Toast.makeText(ElderlyModeActivity.this, "Gesture not recognized", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private boolean areGesturesSimilar(Gesture g1, Gesture g2) {
        if (g1 == null || g2 == null) return false;

        RectF b1 = g1.getBoundingBox();
        RectF b2 = g2.getBoundingBox();

        float scaleX = Math.abs(b1.width() - b2.width()) / Math.max(b1.width(), b2.width());
        float scaleY = Math.abs(b1.height() - b2.height()) / Math.max(b1.height(), b2.height());

        return scaleX < 0.3 && scaleY < 0.3;
    }

    private Gesture deserializeGesture(byte[] data) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(data, 0, data.length);
        parcel.setDataPosition(0);
        Gesture gesture = Gesture.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return gesture;
    }

    // -------------------- Physical Button Handling --------------------

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            // Needed to detect long press
            event.startTracking();
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                Toast.makeText(this, "Volume UP pressed", Toast.LENGTH_SHORT).show();
                handleButtonAction("volumeUpShort");
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Toast.makeText(this, "Volume DOWN pressed", Toast.LENGTH_SHORT).show();
                handleButtonAction("volumeDownShort");
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                Toast.makeText(this, "Volume UP long press", Toast.LENGTH_SHORT).show();
                handleButtonAction("volumeUpLong");
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Toast.makeText(this, "Volume DOWN long press", Toast.LENGTH_SHORT).show();
                handleButtonAction("volumeDownLong");
                return true;
            default:
                return super.onKeyLongPress(keyCode, event);
        }
    }

    private void loadButtonConfigFromFirestore() {
        DocumentReference elderlyDoc = firestore.collection("elderly").document(elderlyId);
        elderlyDoc.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Object> config = (Map<String, Object>) doc.get("physicalButtonConfig");
                if (config != null) {
                    for (Map.Entry<String, Object> entry : config.entrySet()) {
                        buttonConfig.put(entry.getKey(), (String) entry.getValue());
                    }
                }
            }
        });
    }

    private void handleButtonAction(String key) {
        String action = buttonConfig.get(key);
        if (action == null || action.equals("None")) return;
        handleAction(action, null);
    }

    // -------------------- Unified Action Handler --------------------

    private void handleAction(String actionType, String actionData) {
        switch (actionType) {
            case "Play Alert Sound":
            case "alert":
                playAlertSound();
                break;
            case "Make Call":
            case "call":
                makeEmergencyCall(actionData);
                break;
            case "Send SMS":
                sendPredefinedSMS();
                break;
            case "Open App":
            case "app":
                openApp(actionData != null ? actionData : "com.example.someapp");
                break;
            case "Trigger Custom Gesture":
                Toast.makeText(this, "Custom gesture triggered", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "Action not configured", Toast.LENGTH_SHORT).show();
        }
    }

    private void playAlertSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.alert_sound);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            } else {
                Toast.makeText(this, "Alert sound not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to play sound", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeEmergencyCall(String number) {
        String phone = (number != null && !number.isEmpty()) ? number : "1234567890";
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void sendPredefinedSMS() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:1234567890"));
        intent.putExtra("sms_body", "Help! I need assistance.");
        startActivity(intent);
    }

    private void openApp(String packageName) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) startActivity(launchIntent);
        else Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show();
    }

    // -------------------- Exit PIN --------------------

    private void askExitPin() {
        final EditText pinInput = new EditText(this);
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Enter PIN")
                .setMessage("Enter your PIN to exit Elderly Mode")
                .setView(pinInput)
                .setPositiveButton("OK", (dialog, which) ->
                        verifyPinAndExit(pinInput.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void verifyPinAndExit(String enteredPin) {
        DocumentReference elderlyDoc = firestore.collection("elderly").document(elderlyId);
        elderlyDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String storedPin = documentSnapshot.getString("pin");
                if (storedPin != null && storedPin.equals(enteredPin)) {
                    getSharedPreferences("GestureAidPrefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("elderly_mode_active", false)
                            .apply();

                    Intent intent = new Intent(ElderlyModeActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Elderly profile not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to fetch profile", Toast.LENGTH_SHORT).show()
        );
    }
}
