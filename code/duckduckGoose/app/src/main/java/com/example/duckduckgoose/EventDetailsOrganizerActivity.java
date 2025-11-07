/**
 * @file EventDetailsOrganizerActivity.java
 * @brief Organizer-facing event details screen with edit/delete and attendee management.
 *
 * Loads an event from Firestore, renders key details, and provides actions to
 * launch the attendee manager, edit the event, or delete it (with confirmation).
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.content.DialogInterface;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * @class EventDetailsOrganizerActivity
 * @brief Activity for organizers to view and manage a single event.
 *
 * Fetches event data, displays key fields (dates, cost, capacity, description),
 * and wires actions for attendee management, editing, and deletion.
 */
public class EventDetailsOrganizerActivity extends AppCompatActivity {
    // --- Private fields (grouped): hold current event ID, UI references, and launchers. ---
    private String eventId;
    private ActivityResultLauncher<Intent> editLauncher;

    private TextView eventTitle;
    private TextView txtWaitingList;
    private TextView txtDates;
    private TextView txtOpen;
    private TextView txtDeadline;
    private TextView txtCost;
    private TextView txtSpots;
    private TextView txtDescription;
    private MaterialButton attendeeManagerButton;
    private MaterialButton editEventButton;
    private MaterialButton deleteButton;

    /**
     * @brief Initializes UI, registers activity result launcher, and loads event details.
     *
     * Sets up edge-to-edge content, adjusts system bar appearance on Android R+,
     * binds view references, configures delete/edit/attendee manager actions, and
     * triggers an initial event load if an event ID is available.
     *
     * @param savedInstanceState Previously saved instance state; may be null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        WindowInsetsController controller = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller = getWindow().getInsetsController();
        }
        if (controller != null) {
            controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details_organizer);

        TopBarWiring.attachProfileSheet(this);

        // Bind views once
        eventTitle          = findViewById(R.id.txtEventTitle);
        txtWaitingList      = findViewById(R.id.txtWaitingList);
        txtDates            = findViewById(R.id.txtDates);
        txtOpen             = findViewById(R.id.txtOpen);
        txtDeadline         = findViewById(R.id.txtDeadline);
        txtCost             = findViewById(R.id.txtCost);
        txtSpots            = findViewById(R.id.txtSpots);
        txtDescription      = findViewById(R.id.txtDescription);
        attendeeManagerButton = findViewById(R.id.attendee_manager_button);
        editEventButton       = findViewById(R.id.edit_event_button);
        deleteButton          = findViewById(R.id.delete_event_button);

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
                            if (deleted) {
                                // Bubble up deletion to caller and close
                                setResult(RESULT_OK, data);
                                finish();
                                return;
                            }
                            if (returnedId != null && !returnedId.equals(this.eventId)) {
                                this.eventId = returnedId;
                            }
                            loadEvent();
                        }
                    }
                }
        );

        // Load the event (also refreshed in onResume)
        if (this.eventId != null && !this.eventId.isEmpty()) {
            loadEvent();
        } else {
            // Legacy fallback
            String title = intent.getStringExtra("title");
            if (title != null && eventTitle != null) eventTitle.setText(title);
        }

        // Delete with confirmation + Firestore delete
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                if (eventId == null || eventId.isEmpty()) {
                    finish();
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Delete event?")
                        .setMessage("This will permanently delete the event and its details. This action cannot be undone.")
                        .setPositiveButton("Delete", (DialogInterface dlg, int which) -> {
                            FirebaseFirestore.getInstance().collection("events")
                                    .document(eventId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Intent data = new Intent();
                                        data.putExtra("eventId", eventId);
                                        data.putExtra("deleted", true);
                                        setResult(RESULT_OK, data);
                                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Back button in layout (if wired via onClick)
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    /**
     * @brief Refreshes the event details whenever the activity regains focus.
     *
     * If an event ID is present, re-queries Firestore and updates the UI with
     * the latest information.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (this.eventId != null && !this.eventId.isEmpty()) loadEvent();
    }

    // --- Private helper: loads event data from Firestore and updates bound views. ---
    private void loadEvent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(this.eventId).get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (doc != null && doc.exists()) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) {
                            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Title
                        if (eventTitle != null) eventTitle.setText(
                                event.getName() != null ? event.getName() : "Untitled Event"
                        );

                        // Waiting list (hide if empty)
                        if (txtWaitingList != null) {
                            int waitingListSize = event.getWaitingList().size();
                            txtWaitingList.setText("Waiting List: " + waitingListSize + " people");
                            txtWaitingList.setVisibility(waitingListSize > 0 ? View.VISIBLE : View.GONE);
                        }

                        // Detail fields with safe fallbacks
                        if (txtDates != null)
                            txtDates.setText(event.getEventDate() != null ? event.getEventDate() : "TBD");
                        if (txtOpen != null)
                            txtOpen.setText("Registration Opens: " + (event.getRegistrationOpens() == null ? "TBD" : event.getRegistrationOpens()));
                        if (txtDeadline != null)
                            txtDeadline.setText("Registration Deadline: " + (event.getRegistrationCloses() == null ? "TBD" : event.getRegistrationCloses()));
                        if (txtCost != null)
                            txtCost.setText("Cost: " + (event.getCost() == null ? "—" : event.getCost()));
                        if (txtSpots != null)
                            txtSpots.setText("Spots: " + (event.getMaxSpots() == null ? "—" : event.getMaxSpots()));
                        if (txtDescription != null)
                            txtDescription.setText("Event description loaded from backend.");

                        // Buttons -> pass eventId
                        if (attendeeManagerButton != null) {
                            attendeeManagerButton.setOnClickListener(v -> {
                                Intent attendeeIntent = new Intent(this, AttendeeManagerActivity.class);
                                attendeeIntent.putExtra("eventId", this.eventId);
                                startActivity(attendeeIntent);
                            });
                        }
                        if (editEventButton != null) {
                            editEventButton.setOnClickListener(v -> {
                                Intent editIntent = new Intent(this, EventEditActivity.class);
                                editIntent.putExtra("mode", "edit");
                                editIntent.putExtra("eventId", this.eventId);
                                if (editLauncher != null) editLauncher.launch(editIntent);
                                else startActivity(editIntent);
                            });
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(ex ->
                        Toast.makeText(this, "Failed to load event: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /**
     * @brief Handles the optional XML onClick to navigate back.
     *
     * Finishes the current activity, returning to the previous screen.
     *
     * @param view The source view that triggered the callback.
     */
    // Optional onClick in XML
    public void goBack(View view) {
        finish();
    }
}
