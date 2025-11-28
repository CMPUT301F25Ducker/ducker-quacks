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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /** List of mock event logs for demonstration. */
    private List<EventLogItem> eventLogs;

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

        // Initialize RecyclerView
        recyclerEventLogs = findViewById(R.id.recyclerEventLogs);
        recyclerEventLogs.setLayoutManager(new LinearLayoutManager(this));

        // Create mock event logs
        eventLogs = createMockEventLogs();

        // Set up adapter
        adapter = new AdminEventLogAdapter(this, eventLogs);
        recyclerEventLogs.setAdapter(adapter);

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
     * Creates a list of mock event logs for demonstration.
     *
     * @return List of placeholder EventLogItem objects
     */
    private List<EventLogItem> createMockEventLogs() {
        List<EventLogItem> mockEventLogs = new ArrayList<>();

        mockEventLogs.add(new EventLogItem(
                "Notification Text",
                "Organizer Name:",
                Arrays.asList("Recipient 1", "Recipient 2", "Recipient 3")
        ));

        mockEventLogs.add(new EventLogItem(
                "Notification Text",
                "Organizer Name:",
                Arrays.asList("Recipient 1", "Recipient 2", "Recipient 3", "Recipient 4", "Recipient 5")
        ));

        mockEventLogs.add(new EventLogItem(
                "Notification Text",
                "Organizer Name:",
                Arrays.asList("Recipient 1", "Recipient 2")
        ));

        mockEventLogs.add(new EventLogItem(
                "Notification Text",
                "Organizer Name:",
                Arrays.asList("Recipient 1", "Recipient 2", "Recipient 3", "Recipient 4")
        ));

        return mockEventLogs;
    }

    /**
     * Simple data class for holding event log information.
     */
    public static class EventLogItem {
        private String title;
        private String organizer;
        private List<String> recipients;

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
    }
}
