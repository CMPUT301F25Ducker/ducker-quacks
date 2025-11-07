/**
 * @file EventManagerActivity.java
 * @brief Admin-only activity for managing all events in the system.
 *
 * Allows administrators to view, search, sort, and open details for all events
 * stored in Firestore. Integrates with top-bar navigation and supports sorting
 * by date, registration, and cost fields.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @class EventManagerActivity
 * @brief Displays a searchable and sortable list of events for admin users.
 *
 * Provides admin-level access to all events stored in Firestore.
 * Admins can search, filter, or click an event to open detailed views.
 */
public class EventManagerActivity extends AppCompatActivity {

    private static final int EVENT_DETAILS_REQUEST = 1;
    private List<Event> events;
    private List<Event> allEvents;
    private EventManagerAdapter adapter;
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private AutoCompleteTextView dropSearch;

    /**
     * @brief Sets up admin access validation, UI elements, and Firestore loading.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        WindowInsetsController controller = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            controller = getWindow().getInsetsController();
        }
        if (controller != null) {
            controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_manager);

        // Attach top bar
        TopBarWiring.attachProfileSheet(this);

        // Verify admin privileges
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in as an administrator", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .get()
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

        /** @brief Back button closes the activity. */
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupSortDropdown();
        setupRecyclerView();
    }

    /**
     * @brief Configures the dropdown menu for sorting events.
     */
    private void setupSortDropdown() {
        AutoCompleteTextView dropSort = findViewById(R.id.dropSortEvents);
        if (dropSort != null) {
            String[] sorts = {"Date (Soonest)", "Date (Latest)", "Registration Opens", "Registration Deadline", "Cost"};
            ArrayAdapter<String> stringAdapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sorts);
            dropSort.setAdapter(stringAdapter);

            dropSort.setOnItemClickListener((parent, view, position, id) -> {
                String sel = (String) parent.getItemAtPosition(position);
                if (sel == null) return;

                switch (sel) {
                    case "Date (Soonest)": sortByEventDate(true); break;
                    case "Date (Latest)": sortByEventDate(false); break;
                    case "Registration Opens": sortByRegistrationOpens(); break;
                    case "Registration Deadline": sortByRegistrationCloses(); break;
                    case "Cost": sortByCost(); break;
                }

                adapter.notifyDataSetChanged();
                refreshSearchSuggestions();
            });
        }
    }

    /**
     * @brief Sets up the RecyclerView for displaying event rows.
     */
    private void setupRecyclerView() {
        dropSearch = findViewById(R.id.dropSearchEvents);
        RecyclerView rv = findViewById(R.id.rvEvents);
        if (rv == null) return;

        rv.setLayoutManager(new LinearLayoutManager(this));
        events = new ArrayList<>();
        adapter = new EventManagerAdapter(events);

        adapter.setOnItemClickListener(event -> {
            Intent intent = new Intent(EventManagerActivity.this, EventDetailsAdminActivity.class);
            intent.putExtra("eventTitle", event.getName());
            if (event.getEventId() != null) intent.putExtra("eventId", event.getEventId());
            startActivityForResult(intent, EVENT_DETAILS_REQUEST);
        });

        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        loadEventsFromFirestore();

        setupSearchFilter();
    }

    /**
     * @brief Loads all events from Firestore into the adapter list.
     */
    private void loadEventsFromFirestore() {
        eventsRef.get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    events.clear();
                    if (allEvents == null) allEvents = new ArrayList<>();
                    allEvents.clear();

                    for (DocumentSnapshot ds : querySnapshot.getDocuments()) {
                        Event e = ds.toObject(Event.class);
                        if (e != null) {
                            if (e.getEventId() == null) e.setEventId(ds.getId());
                            events.add(e);
                            allEvents.add(e);
                        } else {
                            Log.w("EventManager", "Document returned null Event: " + ds.getId());
                        }
                    }

                    adapter.notifyDataSetChanged();
                    refreshSearchSuggestions();
                })
                .addOnFailureListener(e -> Log.e("EventManager", "Failed to load events", e));
    }

    /**
     * @brief Refreshes autocomplete search suggestions from the current event list.
     */
    private void refreshSearchSuggestions() {
        if (dropSearch != null && allEvents != null) {
            List<String> names = new ArrayList<>();
            for (Event ev : allEvents) if (ev.getName() != null) names.add(ev.getName());
            ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, names);
            dropSearch.setAdapter(searchAdapter);
            dropSearch.setEnabled(true);
        }
    }

    /**
     * @brief Filters event list dynamically as the admin types in the search box.
     */
    private void setupSearchFilter() {
        if (dropSearch == null) return;
        dropSearch.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (selected == null) return;
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

        dropSearch.addTextChangedListener(new TextWatcher() {
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

    // ----------------------
    // Sorting helpers
    // ----------------------

    /**
     * @brief Parses a string into a Date object, supporting multiple formats.
     */
    private Date parseDate(String s) {
        if (s == null) return null;
        String[] patterns = {"MM/dd/yy", "MM/dd/yyyy", "MMM d, yyyy", "MMM d", "MMM dd", "yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p, Locale.US).parse(s.trim());
            } catch (ParseException ignored) {}
        }
        return null;
    }

    /**
     * @brief Parses cost strings (e.g. "$20", "Free") into numeric values for sorting.
     */
    private double parseCost(String s) {
        if (s == null) return Double.MAX_VALUE;
        String trimmed = s.trim();
        if (trimmed.equalsIgnoreCase("free") || trimmed.equalsIgnoreCase("—")) return 0.0;
        String cleaned = trimmed.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return Double.MAX_VALUE;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return Double.MAX_VALUE;
        }
    }

    /** @brief Sorts events by their event date. */
    private void sortByEventDate(boolean ascending) {
        Comparator<Event> cmp = Comparator.comparing(this::parseDate,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (!ascending) cmp = cmp.reversed();
        Collections.sort(events, cmp);
    }

    /** @brief Sorts events by registration open date. */
    private void sortByRegistrationOpens() {
        Collections.sort(events, Comparator.comparing(
                this::parseDate, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** @brief Sorts events by registration close date. */
    private void sortByRegistrationCloses() {
        Collections.sort(events, Comparator.comparing(
                this::parseDate, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** @brief Sorts events by cost (ascending). */
    private void sortByCost() {
        Collections.sort(events, Comparator.comparingDouble(this::parseCost));
    }

    /**
     * @brief Handles returning from event details (e.g. when an event is deleted).
     */
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
