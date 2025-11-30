/**
 * Admin screen to browse, sort, search, and open details for events stored in Firestore.
 *
 * Loads all events, supports multiple sort orders, provides live search suggestions, and
 * opens EventDetailsAdminActivity. Only accessible to users with accountType "admin".
 *
 * Expected layout ids: btnBack, dropSortEvents, dropSearchEvents, rvEvents.
 *
 * @author DuckDuckGoose Development Team
 */
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
import java.util.Collections;
import java.util.Comparator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity for administrators to manage and inspect events.
 *
 * Responsibilities include verifying the signed-in user is an admin,
 * fetching events from Firestore and displaying them in a RecyclerView,
 * providing sorting by event date, registration windows, and cost,
 * providing live type-ahead search by event name, and launching
 * EventDetailsAdminActivity for selected events and reflecting deletions on return.
 */
public class EventManagerActivity extends AppCompatActivity {

    /** Request code for launching event details expecting a result. */
    private static final int EVENT_DETAILS_REQUEST = 1;

    /** Working list backing the RecyclerView (changes with search/sort). */
    private List<Event> events;

    /** Full unfiltered list used to restore/reset the working list after searches. */
    private List<Event> allEvents;

    /** RecyclerView adapter responsible for binding event data to views. */
    private EventManagerAdapter adapter;

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /** Reference to the "events" collection in Firestore. */
    private CollectionReference eventsRef;

    /** Search dropdown for quick navigation to an event by name. */
    private AutoCompleteTextView dropSearch;

    /** Optional organizer filter: if non-null, only show events owned by this organizer. */
    private String filterOrganizerEmail;

    /** Reference to the "users" collection (for ownedEvents lookup). */
    private CollectionReference usersRef;

    /**
     * Initializes the activity and sets up UI components.
     *
     * @param savedInstanceState - Saved activity state, if any
     */
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

        // Check if we are filtering by a specific organizer (coming from OrganizerManagerActivity)
        filterOrganizerEmail = getIntent().getStringExtra("filterOrganizerEmail");

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

            // Wire the sort selection to actually sort the list
            dropSort.setOnItemClickListener((parent, view, position, id) -> {
                String sel = (String) parent.getItemAtPosition(position);
                if (sel == null) return;
                switch (sel) {
                    case "Date (Soonest)":
                        sortByEventDate(true);
                        break;
                    case "Date (Latest)":
                        sortByEventDate(false);
                        break;
                    case "Registration Opens":
                        sortByRegistrationOpens();
                        break;
                    case "Registration Deadline":
                        sortByRegistrationCloses();
                        break;
                    case "Cost":
                        sortByCost();
                        break;
                }
                adapter.notifyDataSetChanged();
                if (allEvents != null) {
                    // keep search list in same order
                    allEvents.clear();
                    allEvents.addAll(events);
                    if (dropSearch != null) {
                        List<String> names = new ArrayList<>();
                        for (Event ev : allEvents) if (ev.getName() != null) names.add(ev.getName());
                        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
                        dropSearch.setAdapter(searchAdapter);
                    }
                }
            });
        }

        // Search dropdown (admin)
        dropSearch = findViewById(R.id.dropSearchEvents);

        // Event list
        RecyclerView rv = findViewById(R.id.rvEvents);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            events = new ArrayList<>(Arrays.asList());
            adapter = new EventManagerAdapter(events);
            adapter.setOnItemClickListener(event -> {
                Intent intent = new Intent(EventManagerActivity.this, EventDetailsAdminActivity.class);
                intent.putExtra("eventTitle", event.getName());

                // prefer passing eventId if available
                if (event.getEventId() != null) intent.putExtra("eventId", event.getEventId());
                startActivityForResult(intent, EVENT_DETAILS_REQUEST);
            });
            rv.setAdapter(adapter);

            // Initialize Firestore and collections
            db = FirebaseFirestore.getInstance();
            eventsRef = db.collection("events");
            usersRef = db.collection("users");

            // Load events (filtered or all)
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

    /**
     * Loads events from Firestore.
     * If filterOrganizerUserId is set, only loads events in that user's ownedEvents list.
     * Otherwise, loads all events.
     */
    private void loadEventsFromFirestore() {
        // If we have a userId to filter by, load that user's ownedEvents
        if (filterOrganizerEmail != null && !filterOrganizerEmail.trim().isEmpty()) {
            usersRef.whereEqualTo("email", filterOrganizerEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(userSnap -> {
                        if (userSnap.isEmpty()) {
                            // No such user -> just show empty list
                            events.clear();
                            if (allEvents == null) allEvents = new ArrayList<>();
                            allEvents.clear();
                            adapter.notifyDataSetChanged();
                            if (dropSearch != null) {
                                dropSearch.setAdapter(null);
                                dropSearch.setEnabled(false);
                            }
                            return;
                        }

                        DocumentSnapshot userDoc = userSnap.getDocuments().get(0);

                        @SuppressWarnings("unchecked")
                        List<String> ownedEvents = (List<String>) userDoc.get("ownedEvents");

                        if (ownedEvents == null || ownedEvents.isEmpty()) {
                            // User has no owned events
                            events.clear();
                            if (allEvents == null) allEvents = new ArrayList<>();
                            allEvents.clear();
                            adapter.notifyDataSetChanged();
                            if (dropSearch != null) {
                                dropSearch.setAdapter(null);
                                dropSearch.setEnabled(false);
                            }
                            return;
                        }

                        // Fetch each event by its document ID in ownedEvents
                        events.clear();
                        if (allEvents == null) allEvents = new ArrayList<>();
                        allEvents.clear();

                        final int total = ownedEvents.size();
                        final List<Event> temp = new ArrayList<>();

                        for (String eventId : ownedEvents) {
                            if (eventId == null || eventId.trim().isEmpty()) continue;

                            eventsRef.document(eventId)
                                    .get()
                                    .addOnSuccessListener(evDoc -> {
                                        if (evDoc != null && evDoc.exists()) {
                                            Event e = evDoc.toObject(Event.class);
                                            if (e != null) {
                                                if (e.getEventId() == null) e.setEventId(evDoc.getId());
                                                temp.add(e);
                                            }
                                        }
                                    })
                                    .addOnCompleteListener(task -> {
                                        // When all requests have completed, update UI
                                        if (temp.size() + (total - ownedEvents.size()) >= total) {
                                            events.clear();
                                            allEvents.clear();
                                            events.addAll(temp);
                                            allEvents.addAll(temp);
                                            adapter.notifyDataSetChanged();

                                            if (dropSearch != null) {
                                                List<String> names = new ArrayList<>();
                                                for (Event ev : allEvents)
                                                    if (ev.getName() != null) names.add(ev.getName());
                                                ArrayAdapter<String> searchAdapter =
                                                        new ArrayAdapter<>(this,
                                                                android.R.layout.simple_dropdown_item_1line,
                                                                names);
                                                dropSearch.setAdapter(searchAdapter);
                                                dropSearch.setEnabled(true);
                                            }
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventManager", "Failed to load ownedEvents user", e);
                        Toast.makeText(this, "Failed to load organizer's events", Toast.LENGTH_SHORT).show();
                    });

        } else {
            // Original behavior: load all events
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
                            ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this,
                                    android.R.layout.simple_dropdown_item_1line, names);
                            dropSearch.setAdapter(searchAdapter);
                            dropSearch.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventManager", "Failed to load events", e);
                    });
        }
    }

    /**
     * Attempts to parse a date string using several common formats.
     * Supported formats include "MM/dd/yy", "MM/dd/yyyy", "MMM d, yyyy",
     * "MMM d", "MMM dd", and "yyyy-MM-dd".
     *
     * @param s - The date string to parse
     * @return A Date object if parsing succeeds, null otherwise
     */
    private Date parseDate(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        String[] patterns = {"MM/dd/yy", "MM/dd/yyyy", "MMM d, yyyy", "MMM d", "MMM dd", "yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(p, Locale.US);
                return fmt.parse(trimmed);
            } catch (ParseException ignored) {}
        }
        return null;
    }

    /**
     * Normalizes a cost string to a numeric value for sorting purposes.
     * Handles currency symbols, "Free", and invalid formats.
     *
     * @param s - Cost string (e.g., "$15", "Free", "CAD 12.50")
     * @return Numeric value of the cost, 0.0 for "Free", MAX_VALUE for invalid/null
     */
    private double parseCost(String s) {
        if (s == null) return Double.MAX_VALUE;
        String trimmed = s.trim();
        if (trimmed.equalsIgnoreCase("free") || trimmed.equalsIgnoreCase("—")) return 0.0;
        // remove non-digit and non-dot
        String cleaned = trimmed.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return Double.MAX_VALUE;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return Double.MAX_VALUE;
        }
    }

    /**
     * Sorts the events list by event date.
     * Null dates are sorted to the end of the list.
     *
     * @param ascending - true for chronological order (soonest first),
     *                    false for reverse chronological order
     */
    private void sortByEventDate(boolean ascending) {
        Comparator<Event> cmp = (a, b) -> {
            Date da = parseDate(a.getEventDate());
            Date db = parseDate(b.getEventDate());
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        };
        if (!ascending) cmp = cmp.reversed();
        Collections.sort(events, cmp);
    }

    /**
     * Sorts the events list by registration opening date.
     * Events are sorted in chronological order with earliest opening dates first.
     * Events with null registration dates are sorted to the end of the list.
     */
    private void sortByRegistrationOpens() {
        Comparator<Event> cmp = (a, b) -> {
            Date da = parseDate(a.getRegistrationOpens());
            Date db = parseDate(b.getRegistrationOpens());
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        };
        Collections.sort(events, cmp);
    }

    /**
     * Sorts the events list by registration closing date.
     * Events are sorted in chronological order with earliest closing dates first.
     * Events with null registration dates are sorted to the end of the list.
     */
    private void sortByRegistrationCloses() {
        Comparator<Event> cmp = (a, b) -> {
            Date da = parseDate(a.getRegistrationCloses());
            Date db = parseDate(b.getRegistrationCloses());
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        };
        Collections.sort(events, cmp);
    }

    /**
     * Sorts the events list by cost in ascending order.
     * Free events are sorted first, followed by events with numeric costs.
     * Events with unparseable or missing costs are sorted to the end.
     */
    private void sortByCost() {
        Comparator<Event> cmp = (a, b) -> {
            double ca = parseCost(a.getCost());
            double cb = parseCost(b.getCost());
            return Double.compare(ca, cb);
        };
        Collections.sort(events, cmp);
    }

    /**
     * Processes results returned from EventDetailsAdminActivity.
     * Handles event deletions by removing the deleted event from the list.
     *
     * @param requestCode - Identifier for the child request (should match EVENT_DETAILS_REQUEST)
     * @param resultCode - Result status returned by the child activity
     * @param data - Optional intent data which may contain "eventTitleToDelete" for deletions
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