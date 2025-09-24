package com.example.gestureaid;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ExitPinActivity extends AppCompatActivity {

    private EditText pinEditText;
    private String correctPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exit_pin);

        pinEditText = findViewById(R.id.pinEditText);

        // Load PIN from SharedPreferences (default 1234)
        SharedPreferences prefs = getSharedPreferences("GestureAidPrefs", MODE_PRIVATE);
        correctPin = prefs.getString("exit_pin", "1234");

        // Bind buttons
        MaterialButton btnClear = findViewById(R.id.btnClear);
        MaterialButton btnOk = findViewById(R.id.btnOk);

        // Number buttons listener
        int[] numBtnIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : numBtnIds) {
            findViewById(id).setOnClickListener(v -> {
                String num = ((MaterialButton) v).getText().toString();
                pinEditText.append(num);
            });
        }

        // Clear button
        btnClear.setOnClickListener(v -> {
            String current = pinEditText.getText().toString();
            if (!current.isEmpty()) {
                pinEditText.setText(current.substring(0, current.length() - 1));
            }
        });

        // OK button
        btnOk.setOnClickListener(v -> {
            String enteredPin = pinEditText.getText().toString();
            if (enteredPin.equals(correctPin)) {
                Toast.makeText(this, "PIN correct. Exiting Elderly Mode.", Toast.LENGTH_SHORT).show();
                finish(); // Exit back to ElderlyModeActivity's caller (Dashboard)
            } else {
                Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                pinEditText.setText("");
            }
        });
    }
}
