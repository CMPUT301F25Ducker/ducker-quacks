/**
 * Organizer's event list screen displaying all events they've created.
 *
 * Loads events from Firestore filtered by organizer ID, displays them in a
 * RecyclerView, and provides navigation to individual event detail screens.
 * Also includes sorting options and handles event list refreshes after deletion.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyEventsActivity extends AppCompatActivity {

    private static final int EVENT_DETAILS_REQUEST = 1;

    private List<Event> events;
    private EventManagerAdapter adapter;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private String organizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        TopBarWiring.attachProfileSheet(this);

        Intent intent = getIntent();
        organizerId = intent.getStringExtra("organizerId");

        if (organizerId == null || organizerId.isEmpty()) {
            Toast.makeText(this, "Organizer ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Wire up "New Event" button to launch EventEditActivity
        View btnNewEvent = findViewById(R.id.btnNewEvent);
        if (btnNewEvent != null) {
            btnNewEvent.setOnClickListener(v -> {
                Intent createIntent = new Intent(MyEventsActivity.this, EventEditActivity.class);
                createIntent.putExtra("organizerId", organizerId);
                startActivityForResult(createIntent, EVENT_DETAILS_REQUEST);
            });
        }

        MaterialAutoCompleteTextView drop = findViewById(R.id.dropSortMy);
        if (drop != null) {
            String[] sorts = {"Date (Soonest)", "Date (Latest)", "Registration Opens", "Registration Deadline", "Cost"};
            drop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sorts));
        }

        RecyclerView rv = findViewById(R.id.rvMyEvents);
        rv.setLayoutManager(new LinearLayoutManager(this));
        events = new ArrayList<>();
        adapter = new EventManagerAdapter(events);
        adapter.setOnItemClickListener(event -> {
            Intent detailsIntent = new Intent(MyEventsActivity.this, EventDetailsOrganizerActivity.class);
            detailsIntent.putExtra("eventId", event.getEventId());
            startActivityForResult(detailsIntent, EVENT_DETAILS_REQUEST);
        });
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        loadEventsFromFirestore();
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

    private void loadEventsFromFirestore() {
        eventsRef.whereEqualTo("organizerId", organizerId).get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    events.clear();
                    for (DocumentSnapshot ds : querySnapshot.getDocuments()) {
                        Event e = ds.toObject(Event.class);
                        if (e != null) {
                            if (e.getEventId() == null) {
                                e.setEventId(ds.getId());
                            }
                            events.add(e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("MyEventsActivity", "Failed to load events", e);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EVENT_DETAILS_REQUEST && resultCode == RESULT_OK && data != null) {
            boolean deleted = data.getBooleanExtra("deleted", false);
            if (deleted) {
                loadEventsFromFirestore(); // Reload events if one was deleted
            }
        }
    }
}
