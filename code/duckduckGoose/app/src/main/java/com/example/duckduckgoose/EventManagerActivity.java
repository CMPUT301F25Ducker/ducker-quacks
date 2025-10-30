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

public class EventManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_manager);

        TopBarWiring.attachProfileSheet(this);

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Sort dropdown
        AutoCompleteTextView dropSort = findViewById(R.id.dropSortEvents);
        if (dropSort != null) {
            String[] sorts = {"Date (Soonest)", "Date (Latest)", "Registration Opens", "Registration Deadline", "Cost"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sorts);
            dropSort.setAdapter(adapter);
        }

        // Event list
        RecyclerView rv = findViewById(R.id.rvEvents);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            List<MainActivity.Event> events = Arrays.asList(
                    new MainActivity.Event("City Swim Classic", "Nov 20–22", "Nov 1", "Nov 15", "$25", "12/40"),
                    new MainActivity.Event("Downtown 5K Run", "Dec 3", "Nov 10", "Dec 1", "Free", "80/100"),
                    new MainActivity.Event("Autumn Cycling Tour", "Oct 12", "Sep 25", "Oct 5 (Closed)", "$15", "Filled")
            );
            rv.setAdapter(new EventManagerAdapter(events));
        }
    }
}
