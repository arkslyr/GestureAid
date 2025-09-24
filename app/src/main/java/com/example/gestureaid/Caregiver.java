package com.example.gestureaid;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "caregivers")
public class Caregiver {
    @PrimaryKey
    @NonNull
    public String id;       // Firebase UID

    public String name;
    public String email;
    public String phone;
    public String pin;  // ✅ New field for PIN

    public Caregiver(@NonNull String id, String name, String email, String phone, String pin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.pin = pin;
    }
}
