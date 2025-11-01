package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class EventDetailsOrganizerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        WindowInsetsController controller = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller = getWindow().getInsetsController();
        }
        if (controller != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details_organizer);

        TopBarWiring.attachProfileSheet(this);

        TextView eventTitle = findViewById(R.id.txtEventTitle);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");

        if (title != null) {
            eventTitle.setText(title);
        }

        MaterialButton deleteButton = findViewById(R.id.delete_event_button);
        deleteButton.setOnClickListener(v -> {
            // In production, show confirmation dialog
            finish();
        });

        MaterialButton attendeeManagerButton = findViewById(R.id.attendee_manager_button);
        attendeeManagerButton.setOnClickListener(v -> {
            Intent attendeeIntent = new Intent(this, AttendeeManagerActivity.class);
            attendeeIntent.putExtra("eventTitle", title);
            startActivity(attendeeIntent);
        });

        MaterialButton editEventButton = findViewById(R.id.edit_event_button);
        editEventButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(this, EventEditActivity.class);
            editIntent.putExtra("mode", "edit");
            editIntent.putExtra("title", title);
            startActivity(editIntent);
        });
    }

    public void goBack(View view) {
        finish();
    }
}
