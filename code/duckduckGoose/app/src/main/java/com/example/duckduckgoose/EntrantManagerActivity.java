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

public class EntrantManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    private List<UserItem> entrants;
    private List<UserItem> allEntrants; // Full list for filtering
    private UserManagerAdapter adapter;
    
    private RecyclerView rvEntrants;
    private TextView txtCount;
    private AutoCompleteTextView dropFilterEntrants;

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
        setContentView(R.layout.activity_entrant_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Initialize views
        initializeViews();
        setupDropdownFilter();
        setupRecyclerView();
    }

    private void initializeViews() {
        rvEntrants = findViewById(R.id.rvEntrants);
        txtCount = findViewById(R.id.txtCount);
        dropFilterEntrants = findViewById(R.id.dropFilterEntrants);
    }

    private void setupDropdownFilter() {
        if (dropFilterEntrants != null) {
            String[] filters = {"All", "Active", "Inactive"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filters);
            dropFilterEntrants.setAdapter(adapter);
            dropFilterEntrants.setOnItemClickListener((parent, view, position, id) -> {
                applyFilter(filters[position]);
            });
        }
    }

    private void setupRecyclerView() {
        if (rvEntrants != null) {
            rvEntrants.setLayoutManager(new LinearLayoutManager(this));
            
            // Initialize with fake data
            allEntrants = new ArrayList<>(Arrays.asList(
                    new UserItem("John Doe", "user001", "Active"),
                    new UserItem("Jane Smith", "user002", "Active"),
                    new UserItem("Bob Johnson", "user003", "Inactive"),
                    new UserItem("Alice Williams", "user004", "Active"),
                    new UserItem("Charlie Brown", "user005", "Active"),
                    new UserItem("Diana Prince", "user006", "Inactive"),
                    new UserItem("Eve Adams", "user007", "Active"),
                    new UserItem("Frank Castle", "user008", "Active")
            ));
            entrants = new ArrayList<>(allEntrants);
            adapter = new UserManagerAdapter(entrants, false); // false = hide checkboxes
            adapter.setOnItemClickListener(user -> {
                // Show profile sheet for admin viewing entrant with past/pooled events
                String status = user.getExtra(); // getExtra() returns status for UserItem
                ProfileSheet.newInstance(user.getName(), user.getUserId(), true, false, status, true)
                    .show(getSupportFragmentManager(), "ProfileSheet");
            });
            rvEntrants.setAdapter(adapter);
            
            updateCountDisplay();
        }
    }

    private void applyFilter(String filter) {
        entrants.clear();
        
        if ("All".equals(filter)) {
            entrants.addAll(allEntrants);
        } else {
            for (UserItem entrant : allEntrants) {
                if (entrant.getExtra().contains(filter)) {
                    entrants.add(entrant);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateCountDisplay();
    }

    private void updateCountDisplay() {
        if (txtCount != null) {
            txtCount.setText("Total Attendees: " + entrants.size());
        }
    }

    @Override
    public void onProfileDeleted(String userId) {
        // Remove entrant from list
        for (int i = 0; i < entrants.size(); i++) {
            if (entrants.get(i).getUserId().equals(userId)) {
                entrants.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
        // Also remove from allEntrants
        for (int i = 0; i < allEntrants.size(); i++) {
            if (allEntrants.get(i).getUserId().equals(userId)) {
                allEntrants.remove(i);
                break;
            }
        }
        updateCountDisplay();
        Toast.makeText(this, "Attendee deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEventsButtonClicked(String userId) {
        // Not applicable to entrants
    }

    static class UserItem implements UserManagerAdapter.BaseUserItem {
        String name, userId, extra;
        UserItem(String n, String u, String e) { name = n; userId = u; extra = e; }

        @Override public String getName() { return name; }
        @Override public String getUserId() { return userId; }
        @Override public String getExtra() { return extra; }
    }
}
