package com.example.gestureaid;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ElderlyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Elderly elderly);

    @Query("SELECT * FROM elderly WHERE caregiverId = :caregiverId LIMIT 1")
    Elderly getFirstElderlyByCaregiver(String caregiverId);

    @Query("SELECT * FROM elderly WHERE caregiverId = :caregiverId")
    List<Elderly> getAllByCaregiver(String caregiverId);

    @Query("SELECT * FROM elderly WHERE id = :elderlyId LIMIT 1")
    Elderly getById(String elderlyId);


}
