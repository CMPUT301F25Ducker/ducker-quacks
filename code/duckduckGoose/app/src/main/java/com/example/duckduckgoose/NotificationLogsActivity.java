/**
 * Activity for displaying notification logs for entrant users.
 *
 * Shows a list of notifications with sorting options. Currently displays
 * placeholder data that will be replaced with real notifications later.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays notification logs in a scrollable list with sorting capability.
 *
 * Provides a simple interface for entrants to view their notification history.
 * Currently populated with mock data for UI demonstration purposes.
 */
public class NotificationLogsActivity extends AppCompatActivity {

    /** RecyclerView for displaying notification items. */
    private RecyclerView recyclerNotifications;

    /** Adapter for binding notification data to the RecyclerView. */
    private NotificationAdapter adapter;

    /** List of mock notifications for demonstration. */
    private List<NotificationItem> notifications;

    /**
     * Initializes the notification logs screen and populates with mock data.
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

        // Attach top bar profile sheet
        TopBarWiring.attachProfileSheet(this);

        // Initialize RecyclerView
        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));

        // Create mock notifications
        notifications = createMockNotifications();

        // Set up adapter
        adapter = new NotificationAdapter(notifications);
        recyclerNotifications.setAdapter(adapter);

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
     * Creates a list of mock notifications for demonstration.
     *
     * @return List of placeholder NotificationItem objects
     */
    private List<NotificationItem> createMockNotifications() {
        List<NotificationItem> mockNotifications = new ArrayList<>();

        mockNotifications.add(new NotificationItem(
                "Notification Text",
                "Organizer Name:"
        ));

        mockNotifications.add(new NotificationItem(
                "Notification Text",
                "Organizer Name:"
        ));

        mockNotifications.add(new NotificationItem(
                "Notification Text",
                "Organizer Name:"
        ));

        mockNotifications.add(new NotificationItem(
                "Notification Text",
                "Organizer Name:"
        ));

        return mockNotifications;
    }

    /**
     * Simple data class for holding notification information.
     */
    public static class NotificationItem {
        private String title;
        private String organizer;

        /**
         * Creates a new notification item.
         *
         * @param title - The notification message text
         * @param organizer - The organizer information
         */
        public NotificationItem(String title, String organizer) {
            this.title = title;
            this.organizer = organizer;
        }

        public String getTitle() {
            return title;
        }

        public String getOrganizer() {
            return organizer;
        }
    }
}
