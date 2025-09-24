package com.example.gestureaid;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewGesturesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private String elderlyId;
    private AppDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_gestures);

        recyclerView = findViewById(R.id.recyclerViewGestures);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        elderlyId = getIntent().getStringExtra("elderlyId");
        if (elderlyId == null) {
            Toast.makeText(this, "Elderly ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        localDb = AppDatabase.getInstance(this);

        loadGestures();
    }

    private void loadGestures() {
        AsyncTask.execute(() -> {
            List<GestureEntity> gestures = localDb.gestureDao().getGesturesByElderlyId(elderlyId);
            runOnUiThread(() -> recyclerView.setAdapter(new GestureAdapter(gestures)));
        });
    }

    private class GestureAdapter extends RecyclerView.Adapter<GestureAdapter.GestureViewHolder> {

        private final List<GestureEntity> gestureList;

        GestureAdapter(List<GestureEntity> gestures) {
            this.gestureList = gestures;
        }

        @NonNull
        @Override
        public GestureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new GestureViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GestureViewHolder holder, int position) {
            GestureEntity ge = gestureList.get(position);
            holder.name.setText(ge.gestureName);
            holder.action.setText(ge.actionType + ": " + ge.actionData);

            holder.itemView.setOnClickListener(v -> showGesturePreview(ge));
        }

        @Override
        public int getItemCount() {
            return gestureList.size();
        }

        class GestureViewHolder extends RecyclerView.ViewHolder {
            TextView name, action;

            GestureViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(android.R.id.text1);
                action = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    private void showGesturePreview(GestureEntity ge) {
        Gesture gesture = deserializeGesture(ge.gestureData);

        GestureOverlayView preview = new GestureOverlayView(this);
        preview.setGesture(gesture);
        preview.setGestureColor(Color.BLUE);
        preview.setUncertainGestureColor(Color.RED);
        preview.setGestureStrokeWidth(8);  // make it thicker
        preview.setEventsInterceptionEnabled(false);
        preview.setFadeOffset(0);           // don't fade

        new AlertDialog.Builder(this)
                .setTitle(ge.gestureName)
                .setMessage("Action: " + ge.actionType + " → " + ge.actionData)
                .setView(preview)
                .setPositiveButton("Close", null)
                .show();
    }


    private Gesture deserializeGesture(byte[] data) {
        android.os.Parcel parcel = android.os.Parcel.obtain();
        parcel.unmarshall(data, 0, data.length);
        parcel.setDataPosition(0);
        Gesture gesture = Gesture.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return gesture;
    }
}
