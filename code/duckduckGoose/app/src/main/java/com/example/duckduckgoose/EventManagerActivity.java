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

    // ----------------------
    // Sorting helpers
    // ----------------------
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

    private void sortByCost() {
        Comparator<Event> cmp = (a, b) -> {
            double ca = parseCost(a.getCost());
            double cb = parseCost(b.getCost());
            return Double.compare(ca, cb);
        };
        Collections.sort(events, cmp);
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
