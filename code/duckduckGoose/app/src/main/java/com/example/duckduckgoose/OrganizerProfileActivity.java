/**
 * @file OrganizerProfileActivity.java
 * @brief Activity for viewing an organizer’s profile.
 *
 * Displays the organizer’s name and email passed through the launching Intent,
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
 * @brief Activity to display basic organizer information.
 *
 * Shows organizer name and email, and allows an admin to remove the organizer.
 */
public class OrganizerProfileActivity extends AppCompatActivity {

    /**
     * @brief Initializes the layout, loads organizer data from the Intent, and wires button actions.
     * @param savedInstanceState Saved activity state.
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

        /** TextView displaying the organizer’s full name. */
        TextView organizerName = findViewById(R.id.organizer_name);

        /** TextView displaying the organizer’s email address. */
        TextView organizerEmail = findViewById(R.id.organizer_email);

        /** Button for deleting or removing the organizer. */
        MaterialButton deleteButton = findViewById(R.id.delete_organizer_button);

        // Retrieve data from intent
        Intent intent = getIntent();
        String name = intent.getStringExtra("organizerName");
        String email = intent.getStringExtra("organizerEmail");

        if (name != null) {
            organizerName.setText(name);
        }
        if (email != null) {
            organizerEmail.setText(email);
        }

        /** @brief Finishes the activity after triggering the delete action. */
        deleteButton.setOnClickListener(v -> {
            // Implement delete logic here
            // For now, just finish the activity
            finish();
        });
    }
}
