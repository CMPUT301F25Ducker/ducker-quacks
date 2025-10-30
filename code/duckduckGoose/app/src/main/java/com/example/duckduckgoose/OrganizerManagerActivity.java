package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrganizerManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    private List<UserItem> organizers;
    private UserManagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        AutoCompleteTextView dropFilter = findViewById(R.id.dropFilterOrganizers);
        if (dropFilter != null) {
            String[] counts = {"Any", "1-5", "6-10", "11+"};
            ArrayAdapter<String> stringAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, counts);
            dropFilter.setAdapter(stringAdapter);
        }

        RecyclerView rv = findViewById(R.id.rvOrganizers);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            organizers = new ArrayList<>(Arrays.asList(
                    new UserItem("Alice", "user123", "7"),
                    new UserItem("Bob", "user456", "2"),
                    new UserItem("Charlie", "user789", "15"),
                    new UserItem("David", "user101", "1"),
                    new UserItem("Eve", "user112", "9")
            ));
            adapter = new UserManagerAdapter(organizers, false); // false = hide checkboxes
            adapter.setOnItemClickListener(user -> ProfileSheet.newInstance(user.getName(), user.getUserId(), true, true, ((UserItem) user).extra, false).show(getSupportFragmentManager(), "ProfileSheet"));
            rv.setAdapter(adapter);
        }
    }

    @Override
    public void onProfileDeleted(String userId) {
        for (int i = 0; i < organizers.size(); i++) {
            if (organizers.get(i).getUserId().equals(userId)) {
                organizers.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    @Override
    public void onEventsButtonClicked(String userId) {
        startActivity(new Intent(this, EventManagerActivity.class));
    }

    static class UserItem implements UserManagerAdapter.BaseUserItem {
        String name, userId, extra;
        UserItem(String n, String u, String e) { name = n; userId = u; extra = e; }

        @Override public String getName() { return name; }
        @Override public String getUserId() { return userId; }
        @Override public String getExtra() { return "Events: " + extra; }
    }
}
