package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class AdminConsoleActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_console);


        auth = FirebaseAuth.getInstance();

        TopBarWiring.attachProfileSheet(this);

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

        btnManageEvents.setOnClickListener(v ->
                startActivity(new Intent(this, EventManagerActivity.class)));

        btnManageAttendees.setOnClickListener(v ->
                startActivity(new Intent(this, EntrantManagerActivity.class)));

        btnManageImages.setOnClickListener(v ->
                startActivity(new Intent(this, ImageManagerActivity.class)));

        btnManageOrganizers.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerManagerActivity.class)));

        btnManageAdminAccounts.setOnClickListener(v ->
                startActivity(new Intent(this, AdminManagerActivity.class)));
    }
}
