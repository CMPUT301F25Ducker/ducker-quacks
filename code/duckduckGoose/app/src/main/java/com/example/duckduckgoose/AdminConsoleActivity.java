/**
 * @file AdminConsoleActivity.java
 * @brief Activity providing admin access to management features.
 *
 * Displays buttons to navigate to event, attendee, organizer, image, and admin
 * management screens. Allows admins to sign out and return to the login screen.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * @class AdminConsoleActivity
 * @brief Main control panel for admin users.
 *
 * Handles navigation to different manager activities and provides a logout option.
 */
public class AdminConsoleActivity extends AppCompatActivity {

    /** Firebase authentication instance for managing sign-out. */
    private FirebaseAuth auth;

    /**
     * @brief Initializes the admin console and sets up button listeners.
     * @param savedInstanceState Saved activity state for recreation.
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
        setContentView(R.layout.activity_admin_console);


        auth = FirebaseAuth.getInstance();

        TopBarWiring.attachProfileSheet(this);

        /** @brief Signs out the admin and returns to the login screen. */
        MaterialButton btnLogout = findViewById(R.id.btnBack);
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(AdminConsoleActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("startOn", "LOGIN");
            startActivity(intent);
            finish();
        });

        MaterialButton btnManageEvents = findViewById(R.id.btnManageEvents);
        MaterialButton btnManageAttendees = findViewById(R.id.btnManageAttendees);
        MaterialButton btnManageImages = findViewById(R.id.btnManageImages);
        MaterialButton btnManageOrganizers = findViewById(R.id.btnManageOrganizers);
        MaterialButton btnManageAdminAccounts = findViewById(R.id.btnManageAdminAccounts);

        /** @brief Opens the event management activity. */
        btnManageEvents.setOnClickListener(v ->
                startActivity(new Intent(this, EventManagerActivity.class)));

        /** @brief Opens the attendee (the entrant in other words) management activity. */
        btnManageAttendees.setOnClickListener(v ->
                startActivity(new Intent(this, EntrantManagerActivity.class)));

        /** @brief Opens the image management activity. */
        btnManageImages.setOnClickListener(v ->
                startActivity(new Intent(this, ImageManagerActivity.class)));

        /** @brief Opens the organizer management activity. */
        btnManageOrganizers.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerManagerActivity.class)));

        /** @brief Opens the admin account management activity. */
        btnManageAdminAccounts.setOnClickListener(v ->
                startActivity(new Intent(this, AdminManagerActivity.class)));
    }
}
