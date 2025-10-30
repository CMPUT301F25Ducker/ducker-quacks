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

public class AttendeeManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        AutoCompleteTextView dropFilter = findViewById(R.id.dropFilterAttendees);
        if (dropFilter != null) {
            String[] events = {"All Events", "City Swim Classic", "Downtown 5K Run", "Autumn Cycling Tour"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, events);
            dropFilter.setAdapter(adapter);
        }

        RecyclerView rv = findViewById(R.id.rvAttendees);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            List<UserItem> attendees = Arrays.asList(
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null),
                    new UserItem("Full Name", "User ID", null)
            );
            rv.setAdapter(new UserManagerAdapter(attendees));
        }
    }

    static class UserItem implements UserManagerAdapter.BaseUserItem {
        String name, userId, extra;
        UserItem(String n, String u, String e) { name = n; userId = u; extra = e; }

        @Override public String getName() { return name; }
        @Override public String getUserId() { return userId; }
        @Override public String getExtra() { return extra; }
    }
}
