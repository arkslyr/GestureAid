package com.example.gestureaid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface CaregiverDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Caregiver caregiver);

    @Query("SELECT * FROM caregivers WHERE id = :id LIMIT 1")
    Caregiver getById(String id);
}
