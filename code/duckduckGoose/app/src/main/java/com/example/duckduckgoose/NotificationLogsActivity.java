/**
 * Activity for displaying notification logs for entrant users.
 *
 * Shows a list of notifications with sorting options and real Firebase data.
 * Fetches organizer name from events and displays message with timestamp.
 *
 * @author DuckDuckGoose Development Team
 */
package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays notification logs in a scrollable list with sorting capability.
 *
 * Provides a simple interface for entrants to view their notification history.
 * Loads notification data from Firebase and fetches organizer names for display.
 */
public class NotificationLogsActivity extends AppCompatActivity {

    /** RecyclerView for displaying notification items. */
    private RecyclerView recyclerNotifications;

    /** Adapter for binding notification data to the RecyclerView. */
    private NotificationLogsAdapter adapter;

    /** List of notifications loaded from Firebase. */
    private List<NotificationItem> notifications = new ArrayList<>();

    /** Firestore instance for data access. */
    private FirebaseFirestore db;

    /** Dropdown for sort options. */
    private MaterialAutoCompleteTextView dropSort;

    /**
     * Initializes the notification logs screen and loads real data from Firebase.
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
        setContentView(R.layout.activity_notification_logs);

        db = FirebaseFirestore.getInstance();

        // Attach top bar profile sheet
        TopBarWiring.attachProfileSheet(this);

        // Initialize RecyclerView
        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));

        // Set up adapter with empty list initially
        adapter = new NotificationLogsAdapter(notifications);
        recyclerNotifications.setAdapter(adapter);

        // Set up sort dropdown
        dropSort = findViewById(R.id.dropSort);
        String[] sortOptions = {"Newest First", "Oldest First"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                sortOptions
        );
        dropSort.setAdapter(sortAdapter);
        dropSort.setText(sortOptions[0], false);

        dropSort.setOnItemClickListener((parent, view, position, id) -> {
            String selected = sortAdapter.getItem(position);
            sortList(selected);
        });

        // Load notifications from Firebase
        loadNotifications();

        // Mark notifications as read when viewing
        markNotificationsAsRead();
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
     * Loads notifications from Firebase for the current user.
     *
     * Also fetches event and organizer details for each notification and
     * filters out muted organizer/admin notifications based on user settings.
     */
    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        android.util.Log.d("NotificationLogs", "Loading notifications for user: " + uid);

        // First, read the user's preference for receiving organizer/admin notifications.
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    boolean receive = true;
                    com.google.firebase.Timestamp optOutTs = null;
                    if (userDoc != null && userDoc.exists()) {
                        Boolean b = userDoc.getBoolean("receive_notifications");
                        if (b != null) receive = b;
                        optOutTs = userDoc.getTimestamp("opt_out_updated_at");
                    }

                    // Attach listener to notifications; if user has muted organizer/admin notices
                    // we will filter out event-related notifications that were created at/after opt-out.
                    final boolean finalReceive = receive;
                    final com.google.firebase.Timestamp finalOptOutTs = optOutTs;

                    db.collection("notifications")
                            .whereEqualTo("userId", uid)
                            .addSnapshotListener((value, error) -> {
                                if (error != null) {
                                    android.util.Log.e("NotificationLogs", "Error loading notifications", error);
                                    Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                notifications.clear();
                                if (value != null && !value.isEmpty()) {
                                    android.util.Log.d("NotificationLogs", "Found " + value.size() + " notifications");
                                    for (QueryDocumentSnapshot doc : value) {
                                        // If the user has muted organizer/admin notifications, skip event-related notifications
                                        // that were created at/after the opt-out timestamp. If no opt-out timestamp exists,
                                        // fall back to skipping all event-related notifications (legacy behavior).
                                        String eventId = doc.getString("eventId");
                                        if (!finalReceive && eventId != null && !eventId.isEmpty()) {
                                            com.google.firebase.Timestamp notifTs = doc.getTimestamp("timestamp");
                                            if (finalOptOutTs == null) {
                                                // No opt-out timestamp available: use legacy behavior and skip the notification.
                                                continue;
                                            } else {
                                                // Skip only notifications created at/after the opt-out timestamp.
                                                if (notifTs != null && notifTs.toDate().compareTo(finalOptOutTs.toDate()) >= 0) {
                                                    continue;
                                                }
                                            }
                                        }

                                        String message = doc.getString("message");
                                        String sentBy = doc.getString("sentBy");

                                        // Handle Firestore Timestamp properly
                                        Date timestamp;
                                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                                        if (ts != null) {
                                            timestamp = ts.toDate();
                                        } else {
                                            timestamp = new Date();
                                        }

                                        NotificationItem item = new NotificationItem(message, "", timestamp, eventId, sentBy);
                                        notifications.add(item);

                                        // Fetch organizer name from users collection using sentBy
                                        if (sentBy != null && !sentBy.isEmpty()) {
                                            final NotificationItem finalItem = item;
                                            db.collection("users").document(sentBy).get()
                                                    .addOnSuccessListener(userDoc2 -> {
                                                        if (userDoc2.exists()) {
                                                            String organizerName = userDoc2.getString("fullName");
                                                            if (organizerName != null && !organizerName.isEmpty()) {
                                                                finalItem.setOrganizerName("From: " + organizerName);
                                                                adapter.notifyDataSetChanged();
                                                            }
                                                        }
                                                    });
                                        } else if (eventId != null && !eventId.isEmpty()) {
                                            // Fallback: fetch organizer from event if sentBy not available
                                            final NotificationItem finalItem = item;
                                            db.collection("events").document(eventId).get()
                                                    .addOnSuccessListener(eventDoc -> {
                                                        if (eventDoc.exists()) {
                                                            String organizerId = eventDoc.getString("organizerId");
                                                            if (organizerId != null) {
                                                                db.collection("users").document(organizerId).get()
                                                                        .addOnSuccessListener(userDoc2 -> {
                                                                            if (userDoc2.exists()) {
                                                                                String organizerName = userDoc2.getString("fullName");
                                                                                if (organizerName != null && !organizerName.isEmpty()) {
                                                                                    finalItem.setOrganizerName("From: " + organizerName);
                                                                                    adapter.notifyDataSetChanged();
                                                                                }
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                } else {
                                    android.util.Log.d("NotificationLogs", "No notifications found for user");
                                }
                                sortList(dropSort.getText().toString());
                            });

                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationLogs", "Failed to read user preferences", e);
                    // Fallback to attaching listener without filtering
                    db.collection("notifications")
                            .whereEqualTo("userId", uid)
                            .addSnapshotListener((value, error) -> {
                                if (error != null) {
                                    android.util.Log.e("NotificationLogs", "Error loading notifications", error);
                                    Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                notifications.clear();
                                if (value != null && !value.isEmpty()) {
                                    for (QueryDocumentSnapshot doc : value) {
                                        String message = doc.getString("message");
                                        String eventId = doc.getString("eventId");
                                        String sentBy = doc.getString("sentBy");
                                        Date timestamp;
                                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                                        if (ts != null) timestamp = ts.toDate(); else timestamp = new Date();
                                        NotificationItem item = new NotificationItem(message, "", timestamp, eventId, sentBy);
                                        notifications.add(item);
                                    }
                                }
                                sortList(dropSort.getText().toString());
                            });
                });
    }

    /**
     * Marks notifications as read by updating the user's new_notifications flag.
     */
    private void markNotificationsAsRead() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid)
                .update("new_notifications", false)
                .addOnFailureListener(e ->
                        android.util.Log.e("NotificationLogs", "Failed to mark notifications as read", e));
    }

    /**
     * Sorts the notification list based on the selected criterion.
     *
     * @param criterion - Sort option ("Newest First" or "Oldest First")
     */
    private void sortList(String criterion) {
        if (notifications.isEmpty()) return;

        if ("Oldest First".equals(criterion)) {
            Collections.sort(notifications, Comparator.comparing(NotificationItem::getDate));
        } else {
            // Newest First (default)
            Collections.sort(notifications, (a, b) -> b.getDate().compareTo(a.getDate()));
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Data class for holding notification information.
     */
    public static class NotificationItem {
        private String title;
        private String organizerName;
        private Date date;
        private String eventId;
        private String sentBy;

        /**
         * Creates a new notification item.
         *
         * @param title - The notification message text
         * @param organizerName - The organizer name (may be empty if not yet resolved)
         * @param date - The notification timestamp
         * @param eventId - The event ID associated with this notification
         * @param sentBy - The user ID who sent the notification
         */
        public NotificationItem(String title, String organizerName, Date date, String eventId, String sentBy) {
            this.title = title;
            this.organizerName = organizerName;
            this.date = date;
            this.eventId = eventId;
            this.sentBy = sentBy;
        }

        /** Returns the notification title text. */
        public String getTitle() {
            return title;
        }

        /** Returns the organizer name label. */
        public String getOrganizerName() {
            return organizerName;
        }

        /**
         * Sets the organizer name label.
         *
         * @param organizerName - Organizer name to display
         */
        public void setOrganizerName(String organizerName) {
            this.organizerName = organizerName;
        }

        /** Returns the notification timestamp. */
        public Date getDate() {
            return date;
        }

        /** Returns the associated event ID, if any. */
        public String getEventId() {
            return eventId;
        }

        /** Returns the sender user ID. */
        public String getSentBy() {
            return sentBy;
        }
    }

    /**
     * RecyclerView adapter for displaying notification items.
     *
     * Renders notification message text, organizer name (if available),
     * and formatted timestamp for each row.
     */
    private class NotificationLogsAdapter extends RecyclerView.Adapter<NotificationLogsAdapter.ViewHolder> {
        /** Backing list of notification items. */
        private List<NotificationItem> data;
        /** Formatter for displaying notification dates. */
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        /**
         * Creates a new adapter instance with the given data list.
         *
         * @param items - List of notifications to display
         */
        NotificationLogsAdapter(List<NotificationItem> items) {
            this.data = items;
        }

        /**
         * Inflates the notification row layout and creates a ViewHolder.
         *
         * @param parent - Parent ViewGroup
         * @param viewType - View type for the row
         * @return ViewHolder wrapping the inflated row view
         */
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

        /**
         * Binds a NotificationItem to the row views at the given position.
         *
         * @param holder - ViewHolder containing row views
         * @param position - Position in the adapter data set
         */
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = data.get(position);

            // Set message as title (centered in layout)
            holder.txtTitle.setText(item.getTitle());

            // Show organizer name and timestamp
            String orgName = item.getOrganizerName();
            String timestamp = sdf.format(item.getDate());
            if (orgName != null && !orgName.isEmpty()) {
                holder.txtOrganizer.setText(orgName + " • " + timestamp);
            } else {
                holder.txtOrganizer.setText(timestamp);
            }
        }

        /**
         * Returns the number of notification items in the adapter.
         *
         * @return Total item count
         */
        @Override
        public int getItemCount() {
            return data.size();
        }

        /**
         * ViewHolder for notification log rows.
         *
         * Holds references to title and organizer/timestamp text views.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtOrganizer;

            /**
             * Creates a new ViewHolder and binds view references.
             *
             * @param itemView - Root view for the notification row
             */
            ViewHolder(View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txtNotifTitle);
                txtOrganizer = itemView.findViewById(R.id.txtOrganizer);
            }
        }
    }
}