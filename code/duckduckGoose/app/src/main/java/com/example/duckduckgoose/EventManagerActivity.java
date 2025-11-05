package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventManagerActivity extends AppCompatActivity {

    private static final int EVENT_DETAILS_REQUEST = 1;
    private List<Event> events;
    private EventManagerAdapter adapter;

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
            ArrayAdapter<String> stringAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sorts);
            dropSort.setAdapter(stringAdapter);
        }

        // Event list
        RecyclerView rv = findViewById(R.id.rvEvents);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            events = new ArrayList<>(Arrays.asList(
//                    new Event("City Swim Classic", "Nov 20–22", "Nov 1", "Nov 15", "$25", "12/40"),
//                    new Event("Downtown 5K Run", "Dec 3", "Nov 10", "Dec 1", "Free", "80/100"),
//                    new Event("Autumn Cycling Tour", "Oct 12", "Sep 25", "Oct 5 (Closed)", "$15", "Filled")
            ));
            adapter = new EventManagerAdapter(events);
            adapter.setOnItemClickListener(event -> {
                Intent intent = new Intent(EventManagerActivity.this, EventDetailsAdminActivity.class);
                intent.putExtra("eventTitle", event.getName());
                startActivityForResult(intent, EVENT_DETAILS_REQUEST);
            });
            rv.setAdapter(adapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EVENT_DETAILS_REQUEST && resultCode == RESULT_OK && data != null) {
            String eventTitleToDelete = data.getStringExtra("eventTitleToDelete");
            if (eventTitleToDelete != null) {
                for (int i = 0; i < events.size(); i++) {
                    if (events.get(i).getName().equals(eventTitleToDelete)) {
                        events.remove(i);
                        adapter.notifyItemRemoved(i);
                        break;
                    }
                }
            }
        }
    }
}
