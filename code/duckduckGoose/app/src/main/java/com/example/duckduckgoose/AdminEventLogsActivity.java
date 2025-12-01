/**
 * Activity for displaying event logs for admin users.
 *
 * Shows a list of event notifications with sorting options and the ability
 * to view recipients for each notification. Currently displays placeholder
 * data that will be replaced with real event logs later.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * Displays event logs in a scrollable list with sorting capability.
 *
 * Provides an interface for admins to view event notification history
 * and see who received each notification. Currently populated with
 * mock data for UI demonstration purposes.
 */
public class AdminEventLogsActivity extends AppCompatActivity {

    /** RecyclerView for displaying event log items. */
    private RecyclerView recyclerEventLogs;

    /** Adapter for binding event log data to the RecyclerView. */
    private AdminEventLogAdapter adapter;

    /** List of event logs loaded from Firestore. */
    private List<EventLogItem> eventLogs;

    /** Firestore instance for loading notifications. */
    private FirebaseFirestore db;

    /** Event ID for which to load logs. */
    private String eventId;

    /** Empty state text view. */
    private TextView txtEmptyPlaceholder;

    /**
     * Initializes the event logs screen and populates with mock data.
     *
     * @param savedInstanceState - Saved state bundle
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
        setContentView(R.layout.activity_admin_event_logs);

        // Attach top bar profile sheet
        TopBarWiring.attachProfileSheet(this);

        db = FirebaseFirestore.getInstance();
        eventLogs = new ArrayList<>();

        // Get eventId from intent
        eventId = getIntent() != null ? getIntent().getStringExtra("eventId") : null;

        // Initialize RecyclerView
        recyclerEventLogs = findViewById(R.id.recyclerEventLogs);
        txtEmptyPlaceholder = findViewById(R.id.txtEmptyPlaceholder);
        recyclerEventLogs.setLayoutManager(new LinearLayoutManager(this));

        // Set up adapter
        adapter = new AdminEventLogAdapter(this, eventLogs);
        recyclerEventLogs.setAdapter(adapter);

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event not specified - showing empty list", Toast.LENGTH_SHORT).show();
            updateEmptyState();
        } else {
            loadEventNotifications();
        }

        // Set up sort dropdown
        MaterialAutoCompleteTextView dropSort = findViewById(R.id.dropSort);
        String[] sortOptions = {"Value", "Date", "Priority"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                sortOptions
        );
        dropSort.setAdapter(sortAdapter);
        dropSort.setText(sortOptions[0], false);
    }

    /**
     * Navigates back to the previous screen.
     * Finishes the activity in response to a back button tap.
     *
     * @param view The View that triggered the action
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * Loads all notifications sent by organizers for this specific event.
     * Groups notifications by message and timestamp to show unique sends with recipient lists.
     */
    private void loadEventNotifications() {
        if (db == null) db = FirebaseFirestore.getInstance();

        db.collection("notifications")
                .whereEqualTo("eventId", eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("AdminEventLogs", "Error loading notifications", error);
                        Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    eventLogs.clear();

                    if (value != null && !value.isEmpty()) {
                        android.util.Log.d("AdminEventLogs", "Found " + value.size() + " notifications");
                        // Group notifications by message and sentBy to show batches
                        Map<String, EventLogItem> groupedNotifs = new HashMap<>();

                        for (QueryDocumentSnapshot doc : value) {
                            String message = doc.getString("message");
                            String sentBy = doc.getString("sentBy");
                            String userId = doc.getString("userId");
                            Timestamp ts = doc.getTimestamp("timestamp");
                            
                            android.util.Log.d("AdminEventLogs", "Notification: message=" + message + ", sentBy=" + sentBy + ", userId=" + userId);

                            if (message == null) message = "(no message)";

                            // Create a key for grouping (message + sentBy + rough timestamp)
                            String key = message + "_" + (sentBy != null ? sentBy : "unknown");

                            if (!groupedNotifs.containsKey(key)) {
                                EventLogItem item = new EventLogItem(message, "From: Loading...", new ArrayList<>());
                                item.setSentBy(sentBy);
                                item.setTimestamp(ts != null ? ts.toDate() : new Date());
                                groupedNotifs.put(key, item);

                                // Fetch organizer userId via event -> organizerId -> user
                                final EventLogItem finalItem = item;
                                android.util.Log.d("AdminEventLogs", "Fetching event: " + eventId);
                                db.collection("events").document(eventId).get()
                                        .addOnSuccessListener(eventDoc -> {
                                            if (eventDoc.exists()) {
                                                String organizerId = eventDoc.getString("organizerId");
                                                android.util.Log.d("AdminEventLogs", "Event organizerId: " + organizerId);
                                                
                                                if (organizerId != null && !organizerId.isEmpty()) {
                                                    db.collection("users").document(organizerId).get()
                                                            .addOnSuccessListener(userDoc -> {
                                                                if (userDoc.exists()) {
                                                                    String userIdField = userDoc.getString("userId");
                                                                    android.util.Log.d("AdminEventLogs", "Organizer userId: " + userIdField);
                                                                    if (userIdField != null && !userIdField.isEmpty()) {
                                                                        finalItem.setOrganizer("From: " + userIdField);
                                                                    } else {
                                                                        finalItem.setOrganizer("From: " + organizerId);
                                                                    }
                                                                } else {
                                                                    android.util.Log.d("AdminEventLogs", "Organizer user not found");
                                                                    finalItem.setOrganizer("From: (organizer not found)");
                                                                }
                                                                adapter.notifyDataSetChanged();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                android.util.Log.e("AdminEventLogs", "Error fetching organizer", e);
                                                                finalItem.setOrganizer("From: (error loading organizer)");
                                                                adapter.notifyDataSetChanged();
                                                            });
                                                } else {
                                                    finalItem.setOrganizer("From: (no organizer)");
                                                    adapter.notifyDataSetChanged();
                                                }
                                            } else {
                                                android.util.Log.d("AdminEventLogs", "Event not found");
                                                finalItem.setOrganizer("From: (event not found)");
                                                adapter.notifyDataSetChanged();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("AdminEventLogs", "Error fetching event", e);
                                            finalItem.setOrganizer("From: (error loading event)");
                                            adapter.notifyDataSetChanged();
                                        });
                            }

                            // Add recipient user ID to the list (will fetch names later)
                            if (userId != null) {
                                EventLogItem item = groupedNotifs.get(key);
                                if (item != null && !item.getRecipients().contains(userId)) {
                                    item.getRecipients().add(userId);
                                }
                            }
                        }

                        eventLogs.addAll(groupedNotifs.values());
                        // Sort by timestamp (newest first)
                        eventLogs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    /**
     * Updates the empty state visibility based on whether there are logs.
     */
    private void updateEmptyState() {
        boolean empty = eventLogs.isEmpty();
        if (txtEmptyPlaceholder != null) {
            txtEmptyPlaceholder.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (recyclerEventLogs != null) {
            recyclerEventLogs.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Simple data class for holding event log information.
     */
    public static class EventLogItem {
        /** The notification message text. */
        private String title;

        /** The organizer information. */
        private String organizer;

        /** List of recipients who received the notification. */
        private List<String> recipients;

        /** User ID of the person who sent the notification. */
        private String sentBy;

        /** Timestamp when the notification was sent. */
        private Date timestamp;

        /**
         * Creates a new event log item.
         *
         * @param title - The notification message text
         * @param organizer - The organizer information
         * @param recipients - List of recipients who received the notification
         */
        public EventLogItem(String title, String organizer, List<String> recipients) {
            this.title = title;
            this.organizer = organizer;
            this.recipients = recipients;
        }

        /**
         * Gets the notification message text.
         *
         * @return The notification title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Gets the organizer information.
         *
         * @return The organizer display text
         */
        public String getOrganizer() {
            return organizer;
        }

        /**
         * Gets the list of recipients.
         *
         * @return List of recipient user IDs
         */
        public List<String> getRecipients() {
            return recipients;
        }

        /**
         * Sets the organizer information.
         *
         * @param organizer - The organizer display text
         */
        public void setOrganizer(String organizer) {
            this.organizer = organizer;
        }

        /**
         * Gets the user ID of who sent the notification.
         *
         * @return The sender's user ID
         */
        public String getSentBy() {
            return sentBy;
        }

        /**
         * Sets the user ID of who sent the notification.
         *
         * @param sentBy - The sender's user ID
         */
        public void setSentBy(String sentBy) {
            this.sentBy = sentBy;
        }

        /**
         * Gets the timestamp when the notification was sent.
         *
         * @return The notification timestamp
         */
        public Date getTimestamp() {
            return timestamp;
        }

        /**
         * Sets the timestamp when the notification was sent.
         *
         * @param timestamp - The notification timestamp
         */
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }
}