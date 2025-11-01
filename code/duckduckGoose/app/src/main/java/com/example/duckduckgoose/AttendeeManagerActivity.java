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

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AttendeeManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    private List<UserItem> attendees;
    private List<UserItem> allAttendees; // Full list for filtering
    private UserManagerAdapter adapter;
    
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
    private AutoCompleteTextView dropFilterAttendees;

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
        setContentView(R.layout.activity_attendee_manager);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Initialize views
        initializeViews();
        setupDropdownFilter();
        setupButtonListeners();
        setupRecyclerView();
    }

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

    private void setupDropdownFilter() {
        if (dropFilterAttendees != null) {
            String[] filters = {"Selected/Waiting", "Not Selected", "Duck", "Goose"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filters);
            dropFilterAttendees.setAdapter(adapter);
            dropFilterAttendees.setOnItemClickListener((parent, view, position, id) -> {
                applyFilter(filters[position]);
            });
        }
    }

    private void setupButtonListeners() {
        // Export CSV button
        if (btnExportCSV != null) {
            btnExportCSV.setOnClickListener(v -> 
                Toast.makeText(this, "Export CSV - Feature coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Revoke Ticket button
        if (btnRevokeTicket != null) {
            btnRevokeTicket.setOnClickListener(v -> 
                Toast.makeText(this, "Revoke Ticket - Feature coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // Send Message button
        if (btnSendMessage != null) {
            btnSendMessage.setOnClickListener(v -> 
                Toast.makeText(this, "Send Message - Feature coming soon", Toast.LENGTH_SHORT).show()
            );
        }

        // World Map button
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

    private void setupRecyclerView() {
        if (rvAttendees != null) {
            rvAttendees.setLayoutManager(new LinearLayoutManager(this));
            
            // Initialize with fake data including Duck/Goose statuses
            allAttendees = new ArrayList<>(Arrays.asList(
                    new UserItem("John Doe", "user001", "Duck"),
                    new UserItem("Jane Smith", "user002", "Goose"),
                    new UserItem("Bob Johnson", "user003", "Duck"),
                    new UserItem("Alice Williams", "user004", "Selected"),
                    new UserItem("Charlie Brown", "user005", "Waiting"),
                    new UserItem("Diana Prince", "user006", "Goose"),
                    new UserItem("Eve Adams", "user007", "Duck"),
                    new UserItem("Frank Castle", "user008", "Not Selected")
            ));
            attendees = new ArrayList<>(allAttendees);
            adapter = new UserManagerAdapter(attendees);
            adapter.setOnItemClickListener(user -> {
                // Show profile sheet with "Kick" instead of "Delete"
                String status = user.getExtra(); // getExtra() returns status for UserItem
                ProfileSheet.newInstance(user.getName(), user.getUserId(), true, false, status, true)
                    .show(getSupportFragmentManager(), "ProfileSheet");
            });
            rvAttendees.setAdapter(adapter);
            
            updateCountDisplay();
        }
    }

    private void applyFilter(String filter) {
        attendees.clear();
        
        switch (filter) {
            case "Selected/Waiting":
                for (UserItem user : allAttendees) {
                    if ("Selected".equals(user.getStatus()) || "Waiting".equals(user.getStatus())) {
                        attendees.add(user);
                    }
                }
                break;
            case "Not Selected":
                for (UserItem user : allAttendees) {
                    if ("Not Selected".equals(user.getStatus())) {
                        attendees.add(user);
                    }
                }
                break;
            case "Duck":
                for (UserItem user : allAttendees) {
                    if ("Duck".equals(user.getStatus())) {
                        attendees.add(user);
                    }
                }
                break;
            case "Goose":
                for (UserItem user : allAttendees) {
                    if ("Goose".equals(user.getStatus())) {
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

    private void updateCountDisplay() {
        if (txtCount != null) {
            txtCount.setText("Count: " + attendees.size() + "/Spots");
        }
        if (txtInCircle != null) {
            // Calculate how many are "Duck" or "Goose" (in circle)
            int inCircle = 0;
            for (UserItem user : attendees) {
                if ("Duck".equals(user.getStatus()) || "Goose".equals(user.getStatus())) {
                    inCircle++;
                }
            }
            txtInCircle.setText("In Circle: " + inCircle);
        }
    }

    private void selectRandomAttendees() {
        // Select random subset of attendees (for demonstration)
        if (allAttendees.isEmpty()) return;
        
        Random random = new Random();
        int randomCount = random.nextInt(allAttendees.size()) + 1;
        
        attendees.clear();
        List<UserItem> tempList = new ArrayList<>(allAttendees);
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

    private void showMapPopup() {
        if (mapPopup != null) {
            mapPopup.setVisibility(View.VISIBLE);
        }
        if (mapPopupBackground != null) {
            mapPopupBackground.setVisibility(View.VISIBLE);
        }
    }

    private void hideMapPopup() {
        if (mapPopup != null) {
            mapPopup.setVisibility(View.GONE);
        }
        if (mapPopupBackground != null) {
            mapPopupBackground.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProfileDeleted(String userId) {
        // For attendee manager, this acts as "Kick"
        for (int i = 0; i < attendees.size(); i++) {
            if (attendees.get(i).getUserId().equals(userId)) {
                String name = attendees.get(i).getName();
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

    @Override
    public void onEventsButtonClicked(String userId) {
        // Not used for attendees
    }

    static class UserItem implements UserManagerAdapter.BaseUserItem {
        String name, userId, status;
        UserItem(String n, String u, String s) { name = n; userId = u; status = s; }

        @Override public String getName() { return name; }
        @Override public String getUserId() { return userId; }
        @Override public String getExtra() { return status; }
        public String getStatus() { return status; }
    }
}
