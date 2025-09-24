package com.example.gestureaid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { Caregiver.class, Elderly.class, GestureEntity.class },
        version = 5,                // bump if you change any entity
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "gestureaid_db";
    private static volatile AppDatabase INSTANCE;

    public abstract CaregiverDao caregiverDao();
    public abstract ElderlyDao elderlyDao();
    public abstract GestureDao gestureDao();

    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            // For development: wipes old schema when entities change.
                            // Remove and add proper Migrations for production.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
