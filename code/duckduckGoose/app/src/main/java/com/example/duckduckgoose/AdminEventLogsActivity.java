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
                        // Group notifications by message and sentBy to show batches
                        Map<String, EventLogItem> groupedNotifs = new HashMap<>();

                        for (QueryDocumentSnapshot doc : value) {
                            String message = doc.getString("message");
                            String sentBy = doc.getString("sentBy");
                            String userId = doc.getString("userId");
                            Timestamp ts = doc.getTimestamp("timestamp");

                            if (message == null) message = "(no message)";

                            // Create a key for grouping (message + sentBy + rough timestamp)
                            String key = message + "_" + (sentBy != null ? sentBy : "unknown");

                            if (!groupedNotifs.containsKey(key)) {
                                EventLogItem item = new EventLogItem(message, "Loading organizer...", new ArrayList<>());
                                item.setSentBy(sentBy);
                                item.setTimestamp(ts != null ? ts.toDate() : new Date());
                                groupedNotifs.put(key, item);

                                // Fetch organizer name
                                if (sentBy != null && !sentBy.isEmpty()) {
                                    final EventLogItem finalItem = item;
                                    db.collection("users").document(sentBy).get()
                                            .addOnSuccessListener(userDoc -> {
                                                if (userDoc.exists()) {
                                                    String organizerName = userDoc.getString("fullName");
                                                    if (organizerName != null && !organizerName.isEmpty()) {
                                                        finalItem.setOrganizer("From: " + organizerName);
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                }
                                            });
                                }
                            }

                            // Add recipient to the list
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
        private String title;
        private String organizer;
        private List<String> recipients;
        private String sentBy;
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

        public String getTitle() {
            return title;
        }

        public String getOrganizer() {
            return organizer;
        }

        public List<String> getRecipients() {
            return recipients;
        }

        public void setOrganizer(String organizer) {
            this.organizer = organizer;
        }

        public String getSentBy() {
            return sentBy;
        }

        public void setSentBy(String sentBy) {
            this.sentBy = sentBy;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }
}
