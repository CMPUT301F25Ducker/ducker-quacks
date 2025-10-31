package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class OrganizerProfileActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_organizer_profile);

        TextView organizerName = findViewById(R.id.organizer_name);
        TextView organizerEmail = findViewById(R.id.organizer_email);
        MaterialButton deleteButton = findViewById(R.id.delete_organizer_button);

        Intent intent = getIntent();
        String name = intent.getStringExtra("organizerName");
        String email = intent.getStringExtra("organizerEmail");

        if (name != null) {
            organizerName.setText(name);
        }
        if (email != null) {
            organizerEmail.setText(email);
        }

        deleteButton.setOnClickListener(v -> {
            // Implement delete logic here
            // For now, just finish the activity
            finish();
        });
    }
}
