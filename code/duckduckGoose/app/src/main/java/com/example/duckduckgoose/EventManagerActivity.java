package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;
import android.widget.AutoCompleteTextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventManagerActivity extends AppCompatActivity {

    private static final int EVENT_DETAILS_REQUEST = 1;
    private List<Event> events;
    private List<Event> allEvents; // full list for searching/filtering
    private EventManagerAdapter adapter;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private AutoCompleteTextView dropSearch;

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
        setContentView(R.layout.activity_event_manager);

        TopBarWiring.attachProfileSheet(this);

        // Require admin sign-in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in as an administrator", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Optionally verify accountType == Admin from users collection
        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String acct = doc.getString("accountType");
                    if (acct == null || !acct.equalsIgnoreCase("admin")) {
                        Toast.makeText(this, "You must be an administrator to access this screen", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(this, "User profile not found; admin access required", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to validate admin status", Toast.LENGTH_SHORT).show();
                finish();
            });

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

        // Search dropdown (admin)
        dropSearch = findViewById(R.id.dropSearchEvents);

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
                // prefer passing eventId if available
                if (event.getEventId() != null) intent.putExtra("eventId", event.getEventId());
                startActivityForResult(intent, EVENT_DETAILS_REQUEST);
            });
            rv.setAdapter(adapter);
            // Initialize Firestore and load events from the database
            db = FirebaseFirestore.getInstance();
            eventsRef = db.collection("events");
            loadEventsFromFirestore();

            // Wire search selection -> open event details
            if (dropSearch != null) {
                dropSearch.setOnItemClickListener((parent, view, position, id) -> {
                    String selected = (String) parent.getItemAtPosition(position);
                    if (selected == null) return;
                    // find event by name
                    for (Event e : allEvents) {
                        if (selected.equals(e.getName())) {
                            Intent intent = new Intent(EventManagerActivity.this, EventDetailsAdminActivity.class);
                            intent.putExtra("eventTitle", e.getName());
                            if (e.getEventId() != null) intent.putExtra("eventId", e.getEventId());
                            startActivityForResult(intent, EVENT_DETAILS_REQUEST);
                            return;
                        }
                    }
                });

                // Filter list as admin types
                EditText edt = dropSearch;
                edt.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String q = s.toString().toLowerCase();
                        events.clear();
                        if (q.isEmpty()) {
                            events.addAll(allEvents);
                        } else {
                            for (Event ev : allEvents) {
                                if (ev.getName() != null && ev.getName().toLowerCase().contains(q)) {
                                    events.add(ev);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
            }
        }
    }

    private void loadEventsFromFirestore() {
        eventsRef.get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    events.clear();
                    if (allEvents == null) allEvents = new ArrayList<>();
                    allEvents.clear();
                    for (DocumentSnapshot ds : querySnapshot.getDocuments()) {
                        Event e = ds.toObject(Event.class);
                        if (e != null) {
                            // Ensure eventId is populated from the document id
                            if (e.getEventId() == null) e.setEventId(ds.getId());
                            events.add(e);
                            allEvents.add(e);
                        } else {
                            Log.w("EventManager", "Document returned null Event: " + ds.getId());
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Populate search suggestions (event names)
                    if (dropSearch != null && allEvents != null) {
                        List<String> names = new ArrayList<>();
                        for (Event ev : allEvents) if (ev.getName() != null) names.add(ev.getName());
                        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
                        dropSearch.setAdapter(searchAdapter);
                        dropSearch.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EventManager", "Failed to load events", e);
                });
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
