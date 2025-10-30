package com.example.duckduckgoose;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class OrganizerManagerActivity extends AppCompatActivity {

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
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, counts);
            dropFilter.setAdapter(adapter);
        }

        RecyclerView rv = findViewById(R.id.rvOrganizers);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            List<UserItem> organizers = Arrays.asList(
                    new UserItem("Alice", "user123", "7"),
                    new UserItem("Bob", "user456", "2"),
                    new UserItem("Charlie", "user789", "15"),
                    new UserItem("David", "user101", "1"),
                    new UserItem("Eve", "user112", "9")
            );
            rv.setAdapter(new UserManagerAdapter(organizers));
        }
    }

    static class UserItem implements UserManagerAdapter.BaseUserItem {
        String name, userId, extra;
        UserItem(String n, String u, String e) { name = n; userId = u; extra = e; }

        @Override public String getName() { return name; }
        @Override public String getUserId() { return userId; }
        @Override public String getExtra() { return "Events: " + extra; }
    }
}