package com.example.gestureaid;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestureSetupActivity extends AppCompatActivity {

    private static final int PICK_CONTACT = 100;

    private GestureOverlayView gestureOverlayView;
    private EditText gestureNameEditText;
    private Spinner actionSpinner;
    private MaterialButton saveGestureBtn;

    private Gesture currentGesture;
    private String elderlyId;
    private String selectedActionData = "";

    private FirebaseFirestore firestore;
    private AppDatabase localDb;

    private boolean firstSpinnerCall = true; // 👈 prevents auto-trigger

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_setup);

        gestureOverlayView = findViewById(R.id.gestureOverlay);
        gestureNameEditText = findViewById(R.id.gestureNameEditText);
        actionSpinner = findViewById(R.id.actionSpinner);
        saveGestureBtn = findViewById(R.id.saveGestureBtn);

        firestore = FirebaseFirestore.getInstance();
        localDb = AppDatabase.getInstance(this);

        elderlyId = getIntent().getStringExtra("elderlyId");
        if (elderlyId == null) {
            Toast.makeText(this, "Error: Elderly ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Spinner with "Select Action" + options
        String[] actions = {"Select Action", "Call Contact", "Open App"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, actions);
        actionSpinner.setAdapter(adapter);

        gestureOverlayView.addOnGesturePerformedListener((overlay, gesture) -> {
            currentGesture = gesture;
            Toast.makeText(this, "Gesture Captured", Toast.LENGTH_SHORT).show();
        });

        actionSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (firstSpinnerCall) { // 👈 skip first call
                    firstSpinnerCall = false;
                    return;
                }

                if (pos == 1) { // Call Contact
                    pickContact();
                } else if (pos == 2) { // Open App
                    pickApp();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        saveGestureBtn.setOnClickListener(v -> saveGesture());
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    private void pickApp() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        ArrayAdapter<ApplicationInfo> appAdapter = new ArrayAdapter<ApplicationInfo>(
                this, R.layout.dialog_app_item, apps) {

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.dialog_app_item, parent, false);
                }

                ApplicationInfo appInfo = getItem(position);

                TextView text = convertView.findViewById(R.id.appName);
                ImageView icon = convertView.findViewById(R.id.appIcon);

                if (appInfo != null) {
                    text.setText(pm.getApplicationLabel(appInfo));
                    icon.setImageDrawable(pm.getApplicationIcon(appInfo));
                }

                return convertView;
            }
        };

        new AlertDialog.Builder(this)
                .setTitle("Select App")
                .setAdapter(appAdapter, (dialog, which) -> {
                    ApplicationInfo appInfo = apps.get(which);
                    selectedActionData = appInfo.packageName;
                    gestureNameEditText.setText("Open " + pm.getApplicationLabel(appInfo));
                })
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            Cursor cursor = getContentResolver().query(contactUri,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    }, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String contactName = cursor.getString(0);
                String contactNumber = cursor.getString(1);
                selectedActionData = contactNumber;
                gestureNameEditText.setText("Call " + contactName);
                cursor.close();
            }
        }
    }

    private void saveGesture() {
        String gestureName = gestureNameEditText.getText().toString().trim();
        String actionType = actionSpinner.getSelectedItem().toString().equals("Call Contact") ? "call" : "app";

        if (gestureName.isEmpty() || currentGesture == null || selectedActionData.isEmpty()) {
            Toast.makeText(this, "Fill all fields and capture gesture", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] gestureData = serializeGesture(currentGesture);
        if (gestureData == null) {
            Toast.makeText(this, "Failed to serialize gesture", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to Room
        GestureEntity entity = new GestureEntity(elderlyId, gestureName, actionType, selectedActionData, gestureData);
        AsyncTask.execute(() -> localDb.gestureDao().insertGesture(entity));

        // Save to Firestore
        Map<String, Object> gestureMap = new HashMap<>();
        gestureMap.put("elderlyId", elderlyId);
        gestureMap.put("gestureName", gestureName);
        gestureMap.put("actionType", actionType);
        gestureMap.put("actionData", selectedActionData);
        gestureMap.put("gestureData", Blob.fromBytes(gestureData));

        firestore.collection("gestures")
                .add(gestureMap)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Gesture saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving gesture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private byte[] serializeGesture(Gesture gesture) {
        try {
            Parcel parcel = Parcel.obtain();
            gesture.writeToParcel(parcel, 0);
            byte[] data = parcel.marshall();
            parcel.recycle();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
