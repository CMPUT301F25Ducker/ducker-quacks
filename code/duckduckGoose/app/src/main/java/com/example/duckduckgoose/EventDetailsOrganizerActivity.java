package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class EventDetailsOrganizerActivity extends AppCompatActivity {
    private String eventId;
    private ActivityResultLauncher<Intent> editLauncher;

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
        this.eventId = intent.getStringExtra("eventId");

        // Register launcher to handle results from EventEditActivity so we can react immediately
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            boolean deleted = data.getBooleanExtra("deleted", false);
                            String returnedId = data.getStringExtra("eventId");
                            // If item was deleted in the editor, close this details view so caller (list) can show updated state
                            if (deleted) {
                                setResult(RESULT_OK, data);
                                finish();
                                return;
                            }
                            // If saved/edited, reload current event (id may be same)
                            if (returnedId != null && returnedId.equals(this.eventId)) {
                                loadEvent();
                            } else if (returnedId != null) {
                                // If the id changed for some reason, update and load
                                this.eventId = returnedId;
                                loadEvent();
                            } else {
                                // no id provided, just reload
                                loadEvent();
                            }
                        }
                    }
                }
        );

        // Load the event (will also be called from onResume to refresh after edits)
        if (this.eventId != null) {
            loadEvent();
        } else {
            // Fallback: if no eventId was passed, optionally use a title extra (legacy behaviour)
            String title = intent.getStringExtra("title");
            if (title != null && eventTitle != null) eventTitle.setText(title);
        }

        MaterialButton deleteButton = findViewById(R.id.delete_event_button);
        deleteButton.setOnClickListener(v -> {
            // In production, show confirmation dialog and delete the Firestore document if desired
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If we have an eventId, re-load from Firestore so any edits are reflected.
        if (this.eventId != null) loadEvent();
    }

    private void loadEvent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(this.eventId).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (doc != null && doc.exists()) {
                        Event event = doc.toObject(Event.class);
                        TextView eventTitle = findViewById(R.id.txtEventTitle);
                        if (event != null) {
                            if (eventTitle != null) eventTitle.setText(event.getName());

                            TextView txtDates = findViewById(R.id.txtDates);
                            TextView txtOpen = findViewById(R.id.txtOpen);
                            TextView txtDeadline = findViewById(R.id.txtDeadline);
                            TextView txtCost = findViewById(R.id.txtCost);
                            TextView txtSpots = findViewById(R.id.txtSpots);
                            TextView txtDescription = findViewById(R.id.txtDescription);

                            if (txtDates != null) txtDates.setText(event.getEventDate() != null ? event.getEventDate() : "TBD");
                            if (txtOpen != null) txtOpen.setText("Registration Opens: " + (event.getRegistrationOpens() == null ? "TBD" : event.getRegistrationOpens()));
                            if (txtDeadline != null) txtDeadline.setText("Registration Deadline: " + (event.getRegistrationCloses() == null ? "TBD" : event.getRegistrationCloses()));
                            if (txtCost != null) txtCost.setText("Cost: " + (event.getCost() == null ? "—" : event.getCost()));
                            if (txtSpots != null) txtSpots.setText("Spots: " + (event.getMaxSpots() == null ? "—" : event.getMaxSpots()));
                            if (txtDescription != null) txtDescription.setText("Event description loaded from backend.");

                            // update buttons to pass eventId so other screens can operate on this document
                            MaterialButton attendeeManagerButton = findViewById(R.id.attendee_manager_button);
                            if (attendeeManagerButton != null) {
                                attendeeManagerButton.setOnClickListener(v -> {
                                    Intent attendeeIntent = new Intent(this, AttendeeManagerActivity.class);
                                    attendeeIntent.putExtra("eventId", this.eventId);
                                    startActivity(attendeeIntent);
                                });
                            }

                            MaterialButton editEventButton = findViewById(R.id.edit_event_button);
                            if (editEventButton != null) {
                                editEventButton.setOnClickListener(v -> {
                                    Intent editIntent = new Intent(this, EventEditActivity.class);
                                    editIntent.putExtra("mode", "edit");
                                    editIntent.putExtra("eventId", this.eventId);
                                    // Launch edit activity for result so we can react immediately
                                    if (editLauncher != null) editLauncher.launch(editIntent);
                                    else startActivity(editIntent);
                                });
                            }
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(ex -> Toast.makeText(this, "Failed to load event: " + ex.getMessage(), Toast.LENGTH_LONG).show());
    }

    public void goBack(View view) {
        finish();
    }
}
