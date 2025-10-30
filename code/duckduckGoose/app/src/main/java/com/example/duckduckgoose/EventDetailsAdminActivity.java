package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class EventDetailsAdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details_admin);

        TopBarWiring.attachProfileSheet(this);

        TextView eventTitle = findViewById(R.id.txtEventTitle);

        Intent intent = getIntent();
        String title = intent.getStringExtra("eventTitle");

        if (title != null) {
            eventTitle.setText(title);
        }

        MaterialButton deleteButton = findViewById(R.id.delete_event_button);
        deleteButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("eventTitleToDelete", title);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        MaterialButton eventLogsButton = findViewById(R.id.event_logs_button);
        eventLogsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Event logs not implemented yet", Toast.LENGTH_SHORT).show();
        });

        MaterialButton imagePosterButton = findViewById(R.id.image_poster_button);
        imagePosterButton.setOnClickListener(v -> {
            Intent imageManagerIntent = new Intent(EventDetailsAdminActivity.this, ImageManagerActivity.class);
            startActivity(imageManagerIntent);
        });
    }

    public void goBack(View view) {
        finish();
    }
}
