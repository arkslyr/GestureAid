package com.example.gestureaid;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gestures")
public class GestureEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String elderlyId;
    public String gestureName;

    // NEW fields
    public String actionType;  // "call" or "app"
    public String actionData;  // phoneNumber or packageName

    public byte[] gestureData;

    public GestureEntity(String elderlyId, String gestureName, String actionType, String actionData, byte[] gestureData) {
        this.elderlyId = elderlyId;
        this.gestureName = gestureName;
        this.actionType = actionType;
        this.actionData = actionData;
        this.gestureData = gestureData;
    }
}
