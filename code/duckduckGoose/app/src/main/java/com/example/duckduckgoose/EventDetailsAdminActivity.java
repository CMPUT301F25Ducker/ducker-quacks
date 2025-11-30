/**
 * Admin-only event details screen for managing destructive actions.
 *
 * Provides UI for administrators to delete an event (with entrant notifications),
 * view event logs (placeholder), and manage the event poster images.
 *
 * @author DuckDuckGoose Development Team
 */
package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Activity for administrative actions on a specific event.
 *
 * Handles UI wiring for admin-only controls such as deleting an event
 * (with notifications to affected users) and linking to the image manager.
 */
public class EventDetailsAdminActivity extends AppCompatActivity {
    /** Unique identifier for the event being managed. */
    private String eventId;

    /** TextView displaying the event title. */
    private TextView eventTitle;

    /** TextView displaying the waiting list count. */
    private TextView txtWaitingList;

    /** TextView displaying the event dates. */
    private TextView txtDates;

    /** TextView displaying when registration opens. */
    private TextView txtOpen;

    /** TextView displaying the registration deadline. */
    private TextView txtDeadline;

    /** TextView displaying the event cost. */
    private TextView txtCost;

    /** TextView displaying the number of available spots. */
    private TextView txtSpots;

    /** TextView displaying the event description. */
    private TextView txtDescription;

    /**
     * Initializes the activity and wires up admin controls.
     *
     * Sets up edge-to-edge UI, adjusts system bar appearance when available,
     * binds view references, and attaches click handlers for delete, logs,
     * and image poster actions.
     *
     * @param savedInstanceState - Previously saved instance state bundle (may be null)
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
        setContentView(R.layout.activity_event_details_admin);

        TopBarWiring.attachProfileSheet(this);

        // Bind views once
        eventTitle = findViewById(R.id.txtEventTitle);
        txtWaitingList = findViewById(R.id.txtWaitingList);
        txtDates = findViewById(R.id.txtDates);
        txtOpen = findViewById(R.id.txtOpen);
        txtDeadline = findViewById(R.id.txtDeadline);
        txtCost = findViewById(R.id.txtCost);
        txtSpots = findViewById(R.id.txtSpots);
        txtDescription = findViewById(R.id.txtDescription);

        Intent intent = getIntent();
        String title = intent.getStringExtra("eventTitle");
        this.eventId = intent.getStringExtra("eventId"); // changed this to avoid shadow boxing

        // load stuff from Firebase
        if (this.eventId != null && !this.eventId.isEmpty()) {
            loadEvent();
        }

        if (title != null) {
            eventTitle.setText(title);
        }

        MaterialButton deleteButton = findViewById(R.id.delete_event_button);
        deleteButton.setOnClickListener(v -> {
            // Ensure admin is signed in
            FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
            if (cur == null) {
                Toast.makeText(this, "Please sign in as administrator to perform this action", Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirm
            new AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event? This action will notify entrants.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (eventId == null || eventId.isEmpty()) {
                            Toast.makeText(this, "Event identifier not available; cannot delete", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        performAdminDelete(eventId, title, cur.getUid());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        MaterialButton eventLogsButton = findViewById(R.id.event_logs_button);
        eventLogsButton.setOnClickListener(v -> {
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(this, "Event identifier not available", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent eventLogsIntent = new Intent(EventDetailsAdminActivity.this, AdminEventLogsActivity.class);
            eventLogsIntent.putExtra("eventId", eventId);
            startActivity(eventLogsIntent);
        });

        MaterialButton imagePosterButton = findViewById(R.id.image_poster_button);
        imagePosterButton.setOnClickListener(v -> {
            Intent imageManagerIntent = new Intent(EventDetailsAdminActivity.this, ImageManagerActivity.class);
            startActivity(imageManagerIntent);
        });
    }

    /**
     * Refreshes the event details whenever the activity regains focus.
     *
     * If an event ID is present, re-queries Firestore and updates the UI with
     * the latest information.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (this.eventId != null && !this.eventId.isEmpty()) {
            loadEvent();
        }
    }

    /**
     * Loads event data from Firestore and updates all UI text fields.
     * Also populates the image gallery with event photos or a placeholder.
     */
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
                        if (eventTitle != null) {
                            eventTitle.setText(event.getName() != null ? event.getName() : "Untitled Event");
                        }

                        // Waiting list (hide if empty)
                        if (txtWaitingList != null) {
                            int waitingListSize = event.getWaitingList().size();
                            txtWaitingList.setText("Waiting List: " + waitingListSize + (waitingListSize == 1 ? " person" : " people"));
                            txtWaitingList.setVisibility(waitingListSize > 0 ? View.VISIBLE : View.GONE);
                        }

                        // Detail fields with safe fallbacks
                        if (txtDates != null)
                            txtDates.setText("\nEvent Date: " + (event.getEventDate() == null ? "TBD" : event.getEventDate()));
                        if (txtOpen != null)
                            txtOpen.setText("Registration Opens: " + (event.getRegistrationOpens() == null ? "TBD" : event.getRegistrationOpens()));
                        if (txtDeadline != null)
                            txtDeadline.setText("Registration Deadline: " + (event.getRegistrationCloses() == null ? "TBD" : event.getRegistrationCloses()));
                        if (txtCost != null)
                            txtCost.setText("Cost: $" + (event.getCost() == null ? "—" : event.getCost()));
                        if (txtSpots != null)
                            txtSpots.setText("Spots: " + (event.getMaxSpots() == null ? "—" : event.getMaxSpots()));
                        if (txtDescription != null)
                            txtDescription.setText((event.getDescription() == null || event.getDescription().trim().isEmpty() ? "No description provided by the Organizer." : event.getDescription()));

                        LinearLayout gallery = findViewById(R.id.imageGallery);
                        if (gallery != null) {
                            gallery.removeAllViews();
                            List<String> paths = event.getImagePaths();

                            int screenW = getResources().getDisplayMetrics().widthPixels;
                            int heightPx = (int) (280 * getResources().getDisplayMetrics().density);

                            if (paths != null && !paths.isEmpty()) {
                                for (String url : paths) {
                                    ImageView img = new ImageView(this);
                                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(screenW, heightPx);
                                    lp.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
                                    img.setLayoutParams(lp);
                                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                    Glide.with(this)
                                            .load(url)
                                            .placeholder(R.drawable.poolphoto)
                                            .error(R.drawable.poolphoto)
                                            .into(img);

                                    gallery.addView(img);
                                }
                            }
                            else {
                                ImageView img = new ImageView(this);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(screenW, heightPx);
                                img.setLayoutParams(lp);
                                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                img.setImageResource(R.drawable.image_placeholder);
                                gallery.addView(img);
                            }
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
     * Navigates back to the previous screen.
     * Finishes the activity in response to a back/up button tap.
     *
     * @param view - The View that triggered the action
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * Orchestrates the admin deletion process by notifying all affected entrants
     * before deleting the event document from Firestore.
     *
     * @param eventId - The unique identifier of the event to delete
     * @param title - The event title for notification messages
     * @param adminUid - The user ID of the admin performing the deletion
     */
    private void performAdminDelete(String eventId, String title, String adminUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch event to gather entrant lists
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> recipients = new ArrayList<>();
                    Object regObj = doc.get("registeredUsers");
                    Object waitObj = doc.get("waitingList");
                    Object accObj = doc.get("acceptedFromWaitlist");

                    if (regObj instanceof List) {
                        for (Object o : (List) regObj) if (o != null) recipients.add(o.toString());
                    }
                    if (waitObj instanceof List) {
                        for (Object o : (List) waitObj) if (o != null) recipients.add(o.toString());
                    }
                    if (accObj instanceof List) {
                        for (Object o : (List) accObj) if (o != null) recipients.add(o.toString());
                    }

                    // dedupe
                    List<String> unique = new ArrayList<>();
                    for (String u : recipients) if (!unique.contains(u)) unique.add(u);

                    final int total = unique.size();
                    final int[] done = {0};

                    String message = "The event '" + (title != null ? title : "(untitled)") + "' has been deleted by an administrator.";

                    if (total == 0) {
                        // No recipients; proceed to delete
                        deleteEventDoc(db, eventId, title);
                        return;
                    }

                    for (String uid : unique) {
                        Map<String, Object> notif = new HashMap<>();
                        notif.put("userId", uid);
                        notif.put("message", message);
                        notif.put("eventId", eventId);
                        notif.put("sentBy", adminUid);
                        notif.put("timestamp", com.google.firebase.Timestamp.now());

                        db.collection("notifications").add(notif)
                                .addOnSuccessListener(r -> {
                                    done[0]++;
                                    if (done[0] >= total) {
                                        // After notifications created, delete the event
                                        deleteEventDoc(db, eventId, title);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    done[0]++;
                                    // even on failure continue; once all attempted, delete event
                                    if (done[0] >= total) {
                                        deleteEventDoc(db, eventId, title);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Deletes the event document from Firestore and returns a result to the caller.
     * Sets the activity result to indicate successful deletion before finishing.
     *
     * @param db - Firestore database instance
     * @param eventId - The unique identifier of the event to delete
     * @param title - The event title to include in the result
     */
    private void deleteEventDoc(FirebaseFirestore db, String eventId, String title) {
        db.collection("events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted and entrants notified", Toast.LENGTH_SHORT).show();
                    Intent result = new Intent();
                    result.putExtra("eventId", eventId);
                    result.putExtra("eventTitleToDelete", title);
                    result.putExtra("deleted", true);
                    setResult(RESULT_OK, result);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete event: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}