/**
 * @file AttendeeManagerActivity.java
 * @brief Manager screen for viewing and controlling event attendees/waitlist.
 *
 * Loads waitlisted users for a given event, supports basic filtering and exports,
 * and provides quick actions (map, random selection, messaging).
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;
import android.content.Intent;
import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @class AttendeeManagerActivity
 * @brief Activity to manage the attendee/waitlist roster for an event.
 *
 * Binds users to a RecyclerView, supports filters, CSV export, and organizer actions.
 */
public class AttendeeManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {
    /** Current event id used to scope attendees/waitlist. */
    private String eventId;

    /** Visible list of attendees after filtering. */
    private List<User> attendees;
    /** Full unfiltered attendee list (source for filters). */
    private List<User> allAttendees; // Full list for filtering
    /** Adapter for rendering attendee rows. */
    private UserManagerAdapter adapter;

    /** Count/summary UI elements. */
    private RecyclerView rvAttendees;
    private CardView mapPopup;
    private View mapPopupBackground;
    private TextView txtCount;
    private TextView txtInCircle;
    private MaterialButton btnExportCSV;
    private MaterialButton btnRevokeTicket;
    private MaterialButton btnSendMessage;
    private MaterialButton btnWorldMap;
    private MaterialButton btnSelectRandom;

    /** Filter dropdown for attendee status. */
    private AutoCompleteTextView dropFilterAttendees;
    
    /** Firestore reference for data access. */
    private FirebaseFirestore db;

    /** True if the current user is the event's organizer. */
    private boolean isOrganizer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    // Get eventId from intent
    eventId = getIntent().getStringExtra("eventId");
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

        /**
         * @brief Initializes UI, wires listeners, and loads initial waitlist data.
         * @param savedInstanceState Saved activity state.
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_manager);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Initialize views
        initializeViews();
        // Setup Firestore and load waitlist for provided event id (if any)
        db = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        if (intent != null) {
            eventId = intent.getStringExtra("eventId");
        }

        setupDropdownFilter();
        setupButtonListeners();
        setupRecyclerView();

        // Load waitlist entrants for the event (if eventId provided)
        loadWaitlistEntrants();
    }

    /**
     * @brief Finds and caches view references from the layout.
     */
    private void initializeViews() {
        rvAttendees = findViewById(R.id.rvAttendees);
        mapPopup = findViewById(R.id.mapPopup);
        mapPopupBackground = findViewById(R.id.mapPopupBackground);
        txtCount = findViewById(R.id.txtCount);
        txtInCircle = findViewById(R.id.txtInCircle);
        btnExportCSV = findViewById(R.id.btnExportCSV);
        btnRevokeTicket = findViewById(R.id.btnRevokeTicket);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnWorldMap = findViewById(R.id.btnWorldMap);
        btnSelectRandom = findViewById(R.id.btnSelectRandom);
        dropFilterAttendees = findViewById(R.id.dropFilterAttendees);
    }

    /**
     * @brief Prepares the attendee filter dropdown and applies selection handling.
     */
    private void setupDropdownFilter() {
        if (dropFilterAttendees != null) {
            String[] filters = {"Selected/Waiting", "Not Selected", "Duck", "Goose"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filters);
            dropFilterAttendees.setAdapter(adapter);

            /** @brief Applies the selected filter to the attendee list. */
            dropFilterAttendees.setOnItemClickListener((parent, view, position, id) -> {
                applyFilter(filters[position]);
            });
        }
    }

    /**
     * @brief Wires all button click listeners for actions (export, revoke, message, map, random).
     */
    private void setupButtonListeners() {
        // Export CSV button
        if (btnExportCSV != null) {
            /** @brief Exports the current attendee list to a CSV file in Downloads. */
            btnExportCSV.setOnClickListener(v -> {
                if (attendees == null || attendees.isEmpty()) {
                    Toast.makeText(this, "No attendees to export", Toast.LENGTH_SHORT).show();
                    return;
                }
                StringBuilder csv = new StringBuilder();
                csv.append("UserId,FullName,Email,AccountType\n");
                for (User u : attendees) {
                    csv.append(u.getUserId() != null ? u.getUserId() : "").append(",")
                       .append(u.getFullName() != null ? u.getFullName() : "").append(",")
                       .append(u.getEmail() != null ? u.getEmail() : "").append(",")
                       .append(u.getAccountType() != null ? u.getAccountType() : "").append("\n");
                }

                try {
                    String fileName = "attendees_" + (eventId != null ? eventId : "event") + ".csv";
                    java.io.File downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    java.io.File file = new java.io.File(downloads, fileName);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                    fos.write(csv.toString().getBytes());
                    fos.close();
                    Toast.makeText(this, "CSV exported to Downloads/" + fileName, Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(this, "Failed to export CSV: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        /** @brief Placeholder action for revoking a ticket (not yet implemented). */
        if (btnRevokeTicket != null) {
            btnRevokeTicket.setOnClickListener(v ->
                Toast.makeText(this, "Revoke Ticket - Feature coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        /**
         * @brief Sends a notification to waitlisted entrants (organizer action only).
         *
         * Prompts for a message, then writes a notification doc per user.
         * 
         * Will prompt for message and write notifications for waiting-list users.
         */
        if (btnSendMessage != null) {
            btnSendMessage.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(this, "Please sign in as organizer to send messages", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (eventId == null) {
                    Toast.makeText(this, "No event selected", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isOrganizer) {
                    Toast.makeText(this, "Only the organizer can send notifications to entrants", Toast.LENGTH_SHORT).show();
                    return;
                }

                final android.widget.EditText input = new android.widget.EditText(this);
                input.setHint("Enter message to send to waiting list");

                new AlertDialog.Builder(this)
                    .setTitle("Notify Waiting List")
                    .setView(input)
                    .setPositiveButton("Send", (dialog, which) -> {
                        String message = input.getText().toString().trim();
                        if (message.isEmpty()) {
                            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Send a notification document for each user in the list
                        int total = attendees.size();
                        if (total == 0) {
                            Toast.makeText(this, "No waiting-list entrants to notify", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int[] sentCount = {0};
                        int[] failedCount = {0};

                        for (User u : attendees) {
                            if (u == null || u.getUserId() == null) continue;
                            Map<String, Object> notif = new HashMap<>();
                            notif.put("userId", u.getUserId());
                            notif.put("message", message);
                            notif.put("eventId", eventId);
                            notif.put("sentBy", currentUser.getUid());
                            notif.put("timestamp", com.google.firebase.Timestamp.now());

                            db.collection("notifications")
                                .add(notif)
                                .addOnSuccessListener(docRef -> {
                                    sentCount[0]++;
                                    if (sentCount[0] + failedCount[0] >= total) {
                                        Toast.makeText(this, "Sent to " + sentCount[0] + " / " + total, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    failedCount[0]++;
                                    if (sentCount[0] + failedCount[0] >= total) {
                                        Toast.makeText(this, "Sent to " + sentCount[0] + " / " + total + " (" + failedCount[0] + " failed)", Toast.LENGTH_SHORT).show();
                                    }
                                });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }

        /** @brief Shows the world map popup. */
        if (btnWorldMap != null) {
            btnWorldMap.setOnClickListener(v -> showMapPopup());
        }

        // Select Random button
        if (btnSelectRandom != null) {
            btnSelectRandom.setOnClickListener(v -> selectRandomAttendees());
        }

        // Close map button
        View btnCloseMap = findViewById(R.id.btnCloseMap);
        if (btnCloseMap != null) {
            btnCloseMap.setOnClickListener(v -> hideMapPopup());
        }
    }

    /**
     * @brief Initializes RecyclerView, adapter, and loads initial waitlist users.
     */
    private void setupRecyclerView() {
        if (rvAttendees != null && eventId != null) {
            rvAttendees.setLayoutManager(new LinearLayoutManager(this));

            allAttendees = new ArrayList<>();
            attendees = new ArrayList<>(allAttendees);
            adapter = new UserManagerAdapter(attendees);

            /** @brief Opens profile sheet for the selected attendee. */
            adapter.setOnItemClickListener(user -> {
                String status = user.getAccountType();
                ProfileSheet.newInstance(user, true, false, status, true)
                    .show(getSupportFragmentManager(), "ProfileSheet");
            });
            rvAttendees.setAdapter(adapter);

            // Load waitlisted users for this event from Firestore
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            db.collection("waitlist")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allAttendees.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId != null) {
                            // Optionally fetch user details from 'users' collection
                            db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    User user = userDoc.toObject(User.class);
                                    if (user != null) {
                                        allAttendees.add(user);
                                        attendees.clear();
                                        attendees.addAll(allAttendees);
                                        if (adapter != null) adapter.notifyDataSetChanged();
                                        updateCountDisplay();
                                    }
                                });
                        }
                    }
                });
        }
    }

    /**
     * @brief Loads waitlist entries for the current event and resolves user details.
     *
     * Also checks whether current user is the organizer for the event so we can enable
     * the send-notification action only for organizers.
     */
    private void loadWaitlistEntrants() {
        if (db == null) db = FirebaseFirestore.getInstance();
        if (eventId == null || eventId.isEmpty()) {
            // Nothing to load
            return;
        }

        // Verify organizer by loading event document
        db.collection("events").document(eventId).get()
            .addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String organizerId = doc.getString("organizerId");
                    FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
                    isOrganizer = (cur != null && organizerId != null && organizerId.equals(cur.getUid()));
                    if (!isOrganizer) {
                        // disable send message button for non-organizers
                        if (btnSendMessage != null) btnSendMessage.setEnabled(false);
                    }
                }
            });

        // Query waitlist entries for this event with status "waiting"
        db.collection("waitlist")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", "waiting")
            .get()
            .addOnSuccessListener((QuerySnapshot snapshot) -> {
                if (snapshot == null || snapshot.isEmpty()) {
                    // No waiting-list entries
                    return;
                }

                for (DocumentSnapshot entryDoc : snapshot.getDocuments()) {
                    String uid = entryDoc.getString("userId");
                    if (uid == null) continue;

                    // Fetch user document for display
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener(userDoc -> {
                            if (userDoc != null && userDoc.exists()) {
                                User u = userDoc.toObject(User.class);
                                if (u != null) {
                                    // ensure userId is set (model may not map id field)
                                    try {
                                        java.lang.reflect.Field f = User.class.getDeclaredField("userId");
                                        f.setAccessible(true);
                                        if (f.get(u) == null) f.set(u, uid);
                                    } catch (Exception ex) {
                                        // ignore reflection failures
                                    }
                                    allAttendees.add(u);
                                    attendees.add(u);
                                    if (adapter != null) adapter.notifyDataSetChanged();
                                    updateCountDisplay();
                                } else {
                                    // fallback: create minimal User
                                    User fallback = new User();
                                    try { java.lang.reflect.Field f = User.class.getDeclaredField("userId"); f.setAccessible(true); f.set(fallback, uid); } catch (Exception ignored) {}
                                    try { java.lang.reflect.Field f2 = User.class.getDeclaredField("fullName"); f2.setAccessible(true); f2.set(fallback, entryDoc.getString("userName")); } catch (Exception ignored) {}
                                    allAttendees.add(fallback);
                                    attendees.add(fallback);
                                    if (adapter != null) adapter.notifyDataSetChanged();
                                    updateCountDisplay();
                                }
                            }
                        });
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to load waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * @brief Applies a status-based filter to the attendee list.
     * @param filter One of: "Selected/Waiting", "Not Selected", "Duck", "Goose".
     */
    private void applyFilter(String filter) {
        attendees.clear();

        switch (filter) {
            case "Selected/Waiting":
                for (User user : allAttendees) {
                    if ("Selected".equals(user.getAccountType()) || "Waiting".equals(user.getAccountType())) {
                        attendees.add(user);
                    }
                }
                break;
            case "Not Selected":
                for (User user : allAttendees) {
                    if ("Not Selected".equals(user.getAccountType())) {
                        attendees.add(user);
                    }
                }
                break;
            case "Duck":
                for (User user : allAttendees) {
                    if ("Duck".equals(user.getAccountType())) {
                        attendees.add(user);
                    }
                }
                break;
            case "Goose":
                for (User user : allAttendees) {
                    if ("Goose".equals(user.getAccountType())) {
                        attendees.add(user);
                    }
                }
                break;
            default:
                attendees.addAll(allAttendees);
                break;
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateCountDisplay();
    }

    /**
     * @brief Updates visible counts (total and in-circle summary).
     */
    private void updateCountDisplay() {
        if (txtCount != null) {
            txtCount.setText("Count: " + attendees.size() + "/Spots");
        }
        if (txtInCircle != null) {
            // Calculate how many are "Duck" or "Goose" (in circle)
            int inCircle = 0;
            for (User user : attendees) {
                if ("Duck".equals(user.getAccountType()) || "Goose".equals(user.getAccountType())) {
                    inCircle++;
                }
            }
            txtInCircle.setText("In Circle: " + inCircle);
        }
    }

    /**
     * @brief Picks a random non-empty subset from allAttendees and displays it.
     */
    private void selectRandomAttendees() {
        // Select random subset of attendees (for demonstration)
        if (allAttendees.isEmpty()) return;

        Random random = new Random();
        int randomCount = random.nextInt(allAttendees.size()) + 1;

        attendees.clear();
        List<User> tempList = new ArrayList<>(allAttendees);
        for (int i = 0; i < randomCount && !tempList.isEmpty(); i++) {
            int randomIndex = random.nextInt(tempList.size());
            attendees.add(tempList.remove(randomIndex));
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateCountDisplay();
        Toast.makeText(this, "Selected " + randomCount + " random attendees", Toast.LENGTH_SHORT).show();
    }

    /** @brief Shows the map popup and dims the background. */
    private void showMapPopup() {
        if (mapPopup != null) {
            mapPopup.setVisibility(View.VISIBLE);
        }
        if (mapPopupBackground != null) {
            mapPopupBackground.setVisibility(View.VISIBLE);
        }
    }

    /** @brief Hides the map popup and restores the background. */
    private void hideMapPopup() {
        if (mapPopup != null) {
            mapPopup.setVisibility(View.GONE);
        }
        if (mapPopupBackground != null) {
            mapPopupBackground.setVisibility(View.GONE);
        }
    }

    /**
     * @brief Removes a kicked attendee from lists and updates the UI.
     * @param userId ID of the removed attendee.
     */
    @Override
    public void onProfileDeleted(String userId) {
        // For attendee manager, this acts as "Kick"
        for (int i = 0; i < attendees.size(); i++) {
            if (attendees.get(i).getUserId().equals(userId)) {
                String name = attendees.get(i).getFullName();
                attendees.remove(i);
                if (adapter != null) {
                    adapter.notifyItemRemoved(i);
                }
                Toast.makeText(this, name + " has been kicked", Toast.LENGTH_SHORT).show();
                break;
            }
        }
        // Also remove from full list
        for (int i = 0; i < allAttendees.size(); i++) {
            if (allAttendees.get(i).getUserId().equals(userId)) {
                allAttendees.remove(i);
                break;
            }
        }
        updateCountDisplay();
    }

    /**
     * @brief No-op in attendee manager; events button is unused here.
     * @param userId Target user id.
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        // Not used for attendees
    }
}
