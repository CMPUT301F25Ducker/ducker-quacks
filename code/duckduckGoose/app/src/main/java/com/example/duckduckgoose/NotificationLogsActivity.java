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
 * Loads real notification data from Firebase and fetches organizer names.
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
     * Also fetches event and organizer details for each notification.
     */
    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("NotificationLogs", "Loading notifications for user: " + uid);

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
                            String message = doc.getString("message");
                            String eventId = doc.getString("eventId");
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
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String organizerName = userDoc.getString("fullName");
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
                                                            .addOnSuccessListener(userDoc -> {
                                                                if (userDoc.exists()) {
                                                                    String organizerName = userDoc.getString("fullName");
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
    }

    /**
     * Marks notifications as read by setting new_notifications to false.
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
     * @param criterion The sort option ("Newest First" or "Oldest First")
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
         * @param organizerName - The organizer name
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

        public String getTitle() {
            return title;
        }

        public String getOrganizerName() {
            return organizerName;
        }

        public void setOrganizerName(String organizerName) {
            this.organizerName = organizerName;
        }

        public Date getDate() {
            return date;
        }

        public String getEventId() {
            return eventId;
        }

        public String getSentBy() {
            return sentBy;
        }
    }

    /**
     * RecyclerView adapter for displaying notification items.
     */
    private class NotificationLogsAdapter extends RecyclerView.Adapter<NotificationLogsAdapter.ViewHolder> {
        private List<NotificationItem> data;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        NotificationLogsAdapter(List<NotificationItem> items) {
            this.data = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

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

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtOrganizer;

            ViewHolder(View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txtNotifTitle);
                txtOrganizer = itemView.findViewById(R.id.txtOrganizer);
            }
        }
    }
}
