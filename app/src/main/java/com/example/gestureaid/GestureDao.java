package com.example.gestureaid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GestureDao {

    @Insert
    void insertGesture(GestureEntity gesture);

    // Fetch gestures for a specific elderly
    @Query("SELECT * FROM gestures WHERE elderlyId = :elderlyId")
    List<GestureEntity> getGesturesByElderlyId(String elderlyId);
}
