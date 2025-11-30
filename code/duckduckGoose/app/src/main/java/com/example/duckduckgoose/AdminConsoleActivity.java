/**
 * Admin-only console screen for accessing management features.
 *
 * Displays navigation buttons for event, attendee, organizer, image,
 * and admin account management. Also provides a logout option for
 * returning to the login screen.
 *
 * Author: DuckDuckGoose Development Team
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
 * Main control panel for admin users.
 *
 * Sets up the admin console layout, configures system appearance,
 * and wires all navigation buttons to their respective manager screens.
 * Also handles admin logout.
 */
public class AdminConsoleActivity extends AppCompatActivity {

    /** Firebase authentication instance used for managing logout. */
    private FirebaseAuth auth;

    /**
     * Initializes the admin console and sets up navigation buttons.
     *
     * Prepares the UI, adjusts status bar appearance when supported,
     * attaches the top profile sheet, and wires each button to the
     * appropriate manager activity. Also attaches a listener for the
     * logout button, which returns the admin to the login screen.
     *
     * @param savedInstanceState previously saved state bundle; may be null
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

        // Sign out and return to the login screen
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

        // Opens the event management activity
        btnManageEvents.setOnClickListener(v ->
                startActivity(new Intent(this, EventManagerActivity.class)));

        // Opens the attendee (entrant) management activity
        btnManageAttendees.setOnClickListener(v ->
                startActivity(new Intent(this, EntrantManagerActivity.class)));

        // Opens the image management activity
        btnManageImages.setOnClickListener(v ->
                startActivity(new Intent(this, ImageManagerActivity.class)));

        // Opens the organizer management activity
        btnManageOrganizers.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerManagerActivity.class)));

        // Opens the admin account management activity
        btnManageAdminAccounts.setOnClickListener(v ->
                startActivity(new Intent(this, AdminManagerActivity.class)));
    }
}
