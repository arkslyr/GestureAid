package com.example.gestureaid;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "elderly")
public class Elderly {

    @PrimaryKey
    @NonNull
    public String id;   // Firestore Doc ID

    public String caregiverId; // FirebaseAuth UID
    public String name;
    public int age;
    public String gender;
    public String phone;

    public Elderly(@NonNull String id, String caregiverId, String name, int age, String gender, String phone) {
        this.id = id;
        this.caregiverId = caregiverId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
    }

    public Elderly() {} // Empty constructor for Firestore
}
