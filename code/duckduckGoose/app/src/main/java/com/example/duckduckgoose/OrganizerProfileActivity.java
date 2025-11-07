/**
 * @file OrganizerProfileActivity.java
 * @brief Activity for viewing an organizer’s profile.
 *
 * Displays organizer details (name and email) passed through the launching Intent,
 * and provides an action to delete or remove the organizer (currently a placeholder).
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

/**
 * @class OrganizerProfileActivity
 * @brief Displays basic organizer details and provides a delete option.
 *
 * Retrieves organizer information from the intent and renders it on screen.
 * Includes a button for deleting or removing the organizer.
 */
public class OrganizerProfileActivity extends AppCompatActivity {

    /**
     * @brief Initializes UI and loads organizer information from Intent extras.
     * @param savedInstanceState Saved instance state bundle.
     */
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

        /** TextView for displaying the organizer’s full name. */
        TextView organizerName = findViewById(R.id.organizer_name);

        /** TextView for displaying the organizer’s email address. */
        TextView organizerEmail = findViewById(R.id.organizer_email);

        /** Button for deleting or removing the organizer (placeholder functionality). */
        MaterialButton deleteButton = findViewById(R.id.delete_organizer_button);

        // Retrieve organizer data from intent
        Intent intent = getIntent();
        String name = intent.getStringExtra("organizerName");
        String email = intent.getStringExtra("organizerEmail");

        if (name != null) {
            organizerName.setText(name);
        }
        if (email != null) {
            organizerEmail.setText(email);
        }

        /** @brief Finishes the activity when delete is confirmed (placeholder action). */
        deleteButton.setOnClickListener(v -> {
            // Implement delete logic here
            // For now, just finish the activity
            finish();
        });
    }
}
