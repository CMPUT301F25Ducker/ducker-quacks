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

/**
 * Displays all events created by an organizer.
 *
 * Fetches organizer-owned events from Firestore, displays them in a list,
 * and enables navigation to event details or edit screens. Supports creation
 * of new events and refreshes after modifications or deletions.
 */
public class MyEventsActivity extends AppCompatActivity {

    /** Request code used for returning from event detail or creation screens. */
    private static final int EVENT_DETAILS_REQUEST = 1;

    /** List backing the event RecyclerView. */
    private List<Event> events;

    /** Adapter used to render organizer events. */
    private EventManagerAdapter adapter;

    /** Firestore instance. */
    private FirebaseFirestore db;

    /** Firestore reference to the "events" collection. */
    private CollectionReference eventsRef;

    /** Organizer ID passed into the activity. */
    private String organizerId;

    /**
     * Initializes UI, toolbar, sorting dropdown, RecyclerView, and loads
     * organizer events from Firestore.
     *
     * @param savedInstanceState - Saved activity state bundle
     */
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
     * Finishes the activity when the back button is tapped.
     *
     * @param view The View that triggered this navigation
     */
    public void goBack(View view) {
        finish();
    }

    /**
     * Loads all events for the organizer from Firestore and updates the RecyclerView.
     *
     * Fetches events filtered by organizerId, sets the document ID on each event
     * if missing, and refreshes the adapter display.
     */
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

    /**
     * Receives results from event detail or creation screens.
     *
     * If an event was deleted, the list is refreshed from Firestore.
     *
     * @param requestCode The request code originally supplied
     * @param resultCode Result status returned by the child activity
     * @param data Intent containing returned extras, if any
     */
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