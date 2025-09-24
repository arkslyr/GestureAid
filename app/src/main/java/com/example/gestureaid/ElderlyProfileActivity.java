package com.example.gestureaid;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ElderlyProfileActivity extends AppCompatActivity {

    private EditText nameEditText, ageEditText, phoneEditText;
    private RadioButton maleRadio, femaleRadio;
    private MaterialButton saveBtn;

    private FirebaseFirestore firestore;
    private AppDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elderly_profile);

        // Bind UI
        nameEditText = findViewById(R.id.elderlyNameEditText);
        ageEditText = findViewById(R.id.elderlyAgeEditText);
        phoneEditText = findViewById(R.id.elderlyPhoneEditText);
        maleRadio = findViewById(R.id.maleRadio);
        femaleRadio = findViewById(R.id.femaleRadio);
        saveBtn = findViewById(R.id.saveElderlyBtn);

        firestore = FirebaseFirestore.getInstance();
        localDb = AppDatabase.getInstance(this);

        saveBtn.setOnClickListener(v -> saveElderlyProfile());
    }

    private void saveElderlyProfile() {
        String name = nameEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String gender = maleRadio.isChecked() ? "Male" : "Female";

        if (name.isEmpty() || ageStr.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(ageStr);
        String caregiverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Firestore docId
        String elderlyId = firestore.collection("elderly").document().getId();

        Elderly elderly = new Elderly(elderlyId, caregiverId, name, age, gender, phone);

        // Save to Firestore
        firestore.collection("elderly").document(elderlyId)
                .set(elderly)
                .addOnSuccessListener(aVoid -> {
                    // Save to Room
                    new Thread(() -> {
                        localDb.elderlyDao().insert(elderly);
                    }).start();

                    Toast.makeText(this, "Elderly profile saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
