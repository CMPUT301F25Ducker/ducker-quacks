package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    enum Screen { EVENT_LIST, MY_EVENTS, EVENT_DETAIL, NOTIFICATIONS }
    private Screen current = Screen.EVENT_LIST;

    private void wireTopBarNav() {
        View btnMyEvents = findViewById(R.id.btnMyEvents);
        View btnNewEvent = findViewById(R.id.btnNewEvent);

        // Show/hide buttons based on current screen
        if (current == Screen.MY_EVENTS) {
            // On My Events page: hide "My Events" button, show "New Event" for organizers
            if (btnMyEvents != null) btnMyEvents.setVisibility(View.GONE);
            if (btnNewEvent != null) {
                if (AppConfig.LOGIN_MODE.equals("ORGANIZER")) {
                    btnNewEvent.setVisibility(View.VISIBLE);
                    btnNewEvent.setOnClickListener(v -> {
                        Intent intent = new Intent(this, EventEditActivity.class);
                        intent.putExtra("mode", "create");
                        startActivity(intent);
                    });
                } else {
                    btnNewEvent.setVisibility(View.GONE);
                }
            }
        } else {
            // On other pages: show "My Events" button, hide "New Event"
            if (btnMyEvents != null) {
                btnMyEvents.setVisibility(View.VISIBLE);
                btnMyEvents.setOnClickListener(v -> showMyEvents());
            }
            if (btnNewEvent != null) btnNewEvent.setVisibility(View.GONE);
        }
    }

    private void handleStartOnIntent() {
        String start = getIntent().getStringExtra("startOn");
        if (start == null) return;
        switch (start) {
            case "EVENT_LIST": showEventList(); break;
            case "MY_EVENTS":  showMyEvents();  break;
        }
        getIntent().removeExtra("startOn");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleStartOnIntent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Default to event list, but check intent for specific start screen


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
        showEventList();
        handleStartOnIntent();

        // Intercept system back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                switch (current) {
                    case EVENT_LIST:
                        // Go back to login (logout)
                        goToLogin();
                        break;

                    case MY_EVENTS:
                        // For Organizer mode, go back to login (logout)
                        if (AppConfig.LOGIN_MODE.equals("ORGANIZER")) {
                            goToLogin();
                        } else {
                            // For Entrant mode, go to Event List
                            showEventList();
                        }
                        break;

                    case EVENT_DETAIL:
                    case NOTIFICATIONS:
                        // Go back to event list by default
                        showEventList();
                        break;
                }
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /* ----------------- Screens ----------------- */

    private void showEventList() {
        setContentView(R.layout.activity_event_list);
        current = Screen.EVENT_LIST;
        wireTopBarNav();
        TopBarWiring.attachProfileSheet(this);

        // Top bar nav
        View btnMyEvents = findViewById(R.id.btnMyEvents);
        if (btnMyEvents != null) btnMyEvents.setOnClickListener(v -> showMyEvents());

        // Sort dropdown
        MaterialAutoCompleteTextView drop = findViewById(R.id.dropSort);
        if (drop != null) {
            String[] sorts = {"Date (Soonest)", "Date (Latest)", "Registration Opens", "Registration Deadline", "Cost"};
            drop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sorts));
        }

        // Load events from Firestore and display
        RecyclerView rv = findViewById(R.id.recyclerEvents);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("events").addSnapshotListener((QuerySnapshot snapshots, FirebaseFirestoreException e) -> {
                if (e != null) {
                    Log.e("Firestore", "Listen failed", e);
                    return;
                }
                List<Event> events = new ArrayList<>();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) events.add(event);
                    }
                }
                rv.setAdapter(new EventObjectAdapter(events));
            });
        }
    }

    private void showMyEvents() {
        setContentView(R.layout.activity_my_events);
        current = Screen.MY_EVENTS;
        wireTopBarNav();
        TopBarWiring.attachProfileSheet(this);

        // Sort dropdown
        MaterialAutoCompleteTextView drop = findViewById(R.id.dropSortMy);
        if (drop != null) {
            String[] sorts = {"Date (Soonest)", "Date (Latest)", "Registration Opens", "Registration Deadline", "Cost"};
            drop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sorts));
        }

        // Configure list based on mode
        RecyclerView rv = findViewById(R.id.rvMyEvents);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));

            List<Object> rows = new ArrayList<>();
            if (AppConfig.LOGIN_MODE.equals("ORGANIZER")) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                // Only load events created by the currently authenticated organizer
                com.google.firebase.auth.FirebaseUser fu = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (fu == null) {
                    // No signed-in user: show empty sections
                    rows.clear();
                    rows.add("Past Events:");
                    rows.add("Current Events:");
                    rv.setAdapter(new OrganizerEventAdapter(rows, this));
                } else {
                    String currentUid = fu.getUid();
                    db.collection("events")
                            .whereEqualTo("organizerId", currentUid)
                            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                                if (e != null) {
                                    Log.e("Firestore", "Listen failed", e);
                                    return;
                                }
                                rows.clear();
                                List<Event> pastEvents = new ArrayList<>();
                                List<Event> currentEvents = new ArrayList<>();

                                if (queryDocumentSnapshots != null) {
                                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                        Event event = doc.toObject(Event.class);
                                        // make sure the Event knows its Firestore document id
                                        if (event != null) event.setEventId(doc.getId());
                                        if (event != null && event.getEventDate() != null) {
                                            try {
                                                // Parse your date format: "MM/dd/yy"
                                                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
                                                Date eventDate = sdf.parse(event.getEventDate());
                                                Date today = new Date();

                                                if (eventDate != null) {
                                                    // Compare the event date to today
                                                    if (eventDate.before(today)) {
                                                        pastEvents.add(event);
                                                    } else {
                                                        currentEvents.add(event);
                                                    }
                                                }
                                            } catch (ParseException ex) {
                                                Log.e("Firestore", "Date parse error for event: " + event.getEventDate(), ex);
                                                currentEvents.add(event); // Default to current if parsing fails
                                            }
                                        }
                                    }
                                }

                                // Build rows for RecyclerView
                                rows.add("Past Events:");
                                rows.addAll(pastEvents);
                                rows.add("Current Events:");
                                rows.addAll(currentEvents);

                                // Update RecyclerView
                                rv.setAdapter(new OrganizerEventAdapter(rows, this));
                            });
                }
            } else {
                // Entrant view: Show Pre-Registration and Past Registration sections
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String uid = null;
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                }

                if (uid != null) {
                    // Query events where this user is an attendee (requires 'attendees' array field in documents)
                    db.collection("events")
                            .whereArrayContains("attendees", uid)
                            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                                if (e != null) {
                                    Log.e("Firestore", "Listen failed", e);
                                    return;
                                }
                                rows.clear();
                                List<Event> pastEvents = new ArrayList<>();
                                List<Event> preregEvents = new ArrayList<>();

                                if (queryDocumentSnapshots != null) {
                                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                        Event event = doc.toObject(Event.class);
                                        if (event != null && event.getEventDate() != null) {
                                            try {
                                                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
                                                Date eventDate = sdf.parse(event.getEventDate());
                                                Date today = new Date();

                                                if (eventDate != null) {
                                                    if (eventDate.before(today)) {
                                                        pastEvents.add(event);
                                                    } else {
                                                        preregEvents.add(event);
                                                    }
                                                }
                                            } catch (ParseException ex) {
                                                Log.e("Firestore", "Date parse error for event: " + event.getEventDate(), ex);
                                                preregEvents.add(event);
                                            }
                                        }
                                    }
                                }

                                rows.add("Pre-Registration Deadline");
                                rows.addAll(preregEvents);
                                rows.add("Past Registration Deadline");
                                rows.addAll(pastEvents);

                                rv.setAdapter(new SectionedEventAdapter(rows));
                            });
                } else {
                    // Not signed in: show empty sections
                    rows.add("Pre-Registration Deadline");
                    rows.add("Past Registration Deadline");
                    rv.setAdapter(new SectionedEventAdapter(rows));
                }
            }
        }
    }

    /* ----------------- Simple demo adapters ----------------- */
//
//    static class Event {
//        String title, date, open, deadline, cost, spots;
//        Event(String t, String d, String o, String dl, String c, String s){
//            title=t; date=d; open=o; deadline=dl; cost=c; spots=s;
//        }
//    }

    /** Event list on the main "Events" screen */
    static class SimpleEventAdapter extends RecyclerView.Adapter<EventVH> {
        private final List<String> data;
        SimpleEventAdapter(List<String> d){ data = d; }

        @NonNull @Override
        public EventVH onCreateViewHolder(@NonNull android.view.ViewGroup p, int vType) {
            View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_event_card, p, false);
            return new EventVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EventVH h, int i) {
            String titleStr = data.get(i);

            // Fill card UI
            h.title.setText(titleStr);
            h.date.setText("TBD");
            h.open.setText("Registration Opens: TBD");
            h.deadline.setText("Registration Deadline: TBD");
            h.cost.setText("Cost: —");
            h.spots.setText("Spots: —");

            // Click -> open detail with distinct state per item
            h.itemView.setOnClickListener(v -> {
                android.content.Context c = v.getContext();
                android.content.Intent intent =
                        new android.content.Intent(c, EventDetailActivity.class);

                // Common extras
                intent.putExtra("title",    titleStr);
                intent.putExtra("dateText", "TBD");
                intent.putExtra("open",     0L);
                intent.putExtra("deadline", 0L);
                intent.putExtra("cost",     "—");
                intent.putExtra("spots",    "—");
                intent.putExtra("posterRes", R.drawable.poolphoto);

                // 0: UNDECIDED, 1: NOT_IN_CIRCLE, 2: LEAVE_CIRCLE, 3: DUCK, 4: GOOSE
                int pos = h.getAdapterPosition();
                int state;
                switch (pos) {
                    case 0:  state = 0; break; // undecided
                    case 1:  state = 1; break; // enter circle
                    case 2:  state = 2; break; // leave circle
                    case 3:  state = 3; break; // duck
                    default: state = 4; break; // goose
                }
                intent.putExtra("state", state);

                c.startActivity(intent);
            });
        }

        @Override public int getItemCount(){ return data.size(); }
    }

        /** Adapter that binds Event objects into event cards */
        static class EventObjectAdapter extends RecyclerView.Adapter<EventVH> {
            private final List<Event> data;
            EventObjectAdapter(List<Event> d){ data = d; }

            @NonNull @Override
            public EventVH onCreateViewHolder(@NonNull android.view.ViewGroup p, int vType) {
                View v = android.view.LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_event_card, p, false);
                return new EventVH(v);
            }

            @Override
            public void onBindViewHolder(@NonNull EventVH h, int i) {
                Event e = data.get(i);
                h.bind(e);

                h.itemView.setOnClickListener(v -> {
                    android.content.Context c = v.getContext();
                    android.content.Intent intent = new android.content.Intent(c, EventDetailActivity.class);
                    intent.putExtra("title", e.getName());
                    intent.putExtra("dateText", e.getEventDate());
                    intent.putExtra("open", 0L);
                    intent.putExtra("deadline", 0L);
                    intent.putExtra("cost", e.getCost());
                    intent.putExtra("spots", e.getMaxSpots());
                    intent.putExtra("posterRes", R.drawable.poolphoto);
                    intent.putExtra("state", 0);
                    c.startActivity(intent);
                });
            }

            @Override public int getItemCount(){ return data.size(); }
        }

    /** Shared ViewHolder for an event card */
    static class EventVH extends RecyclerView.ViewHolder {
        TextView title, date, open, deadline, cost, spots;
        EventVH(View v){
            super(v);
            title   = v.findViewById(R.id.txtTitle);
            date    = v.findViewById(R.id.txtDate);
            open    = v.findViewById(R.id.txtOpen);
            deadline= v.findViewById(R.id.txtDeadline);
            cost    = v.findViewById(R.id.txtCost);
            spots   = v.findViewById(R.id.txtSpots);
        }

        void bind(Event e) {
            title.setText(e.getName());
            date.setText(e.getEventDate());
            open.setText("Registration Opens: " + e.getRegistrationOpens());
            deadline.setText("Registration Deadline: " + e.getRegistrationCloses());
            cost.setText("Cost: " + e.getCost());
            spots.setText("Spots: " + e.getMaxSpots());
        }
    }

    /** Sectioned list used on "My Events" (with headers) */
    static class SectionedEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0, TYPE_EVENT = 1;
        private final List<Object> rows;
        SectionedEventAdapter(List<Object> r){ rows = r; }

        @Override
        public int getItemViewType(int pos) {
            return (rows.get(pos) instanceof String) ? TYPE_HEADER : TYPE_EVENT;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup p, int vType) {
            if (vType == TYPE_HEADER) {
                TextView tv = (TextView) android.view.LayoutInflater.from(p.getContext())
                        .inflate(android.R.layout.simple_list_item_1, p, false);
                tv.setTextSize(18);
                tv.setPadding(24, 24, 24, 8);
                tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                return new RecyclerView.ViewHolder(tv) {};
            } else {
                View v = android.view.LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_event_card, p, false);
                return new EventVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int i) {
            if (getItemViewType(i) == TYPE_HEADER) {
                // Header row (no click)
                ((TextView) h.itemView).setText((String) rows.get(i));
                h.itemView.setOnClickListener(null);
            } else {
                // Event row
                Event e = (Event) rows.get(i);
                EventVH vh = (EventVH) h;

                vh.title.setText(e.getName());
                vh.date.setText(e.getEventDate());
                vh.open.setText("Registration Opens: " + e.getRegistrationOpens());
                vh.deadline.setText("Registration Deadline: " + e.getRegistrationCloses());
                vh.cost.setText("Cost: " + e.getCost());
                vh.spots.setText("Spots: " + e.getMaxSpots());

                vh.itemView.setOnClickListener(v -> {
                    android.content.Context c = v.getContext();
                    android.content.Intent intent =
                            new android.content.Intent(c, EventDetailActivity.class);

                    // Common extras
                    intent.putExtra("title",    e.getName());
                    intent.putExtra("dateText", e.getEventDate());
                    intent.putExtra("open",     0L);
                    intent.putExtra("deadline", 0L);
                    intent.putExtra("cost",     e.getCost());
                    intent.putExtra("spots",    e.getMaxSpots());
                    intent.putExtra("posterRes", R.drawable.poolphoto);

                    // Map each event in this sectioned list to a different state:
                    // positions: 0(H),1(E),2(E),3(H),4(E),5(E)
                    int pos = h.getAdapterPosition();
                    int state;
                    // 1: undecided, 2: not in circle, 4: leave circle, 5: duck, others: goose
                    switch (pos) {
                        case 1:  state = 0; break; // first event after header -> UNDECIDED
                        case 2:  state = 1; break; // NOT_IN_CIRCLE
                        case 4:  state = 2; break; // LEAVE_CIRCLE
                        case 5:  state = 3; break; // DUCK
                        default: state = 4; break; // GOOSE
                    }
                    intent.putExtra("state", state);

                    c.startActivity(intent);
                });
            }
        }

        @Override public int getItemCount(){ return rows.size(); }
    }

    /** Organizer-specific event adapter with click to edit */
    static class OrganizerEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0, TYPE_EVENT = 1;
        private final List<Object> rows;
        private final MainActivity context;

        OrganizerEventAdapter(List<Object> r, MainActivity ctx){
            rows = r;
            context = ctx;
        }

        @Override
        public int getItemViewType(int pos) {
            return (rows.get(pos) instanceof String) ? TYPE_HEADER : TYPE_EVENT;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup p, int vType) {
            if (vType == TYPE_HEADER) {
                TextView tv = (TextView) android.view.LayoutInflater.from(p.getContext())
                        .inflate(android.R.layout.simple_list_item_1, p, false);
                tv.setTextSize(18);
                tv.setPadding(24, 24, 24, 8);
                tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                return new RecyclerView.ViewHolder(tv) {};
            } else {
                View v = android.view.LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_event_card, p, false);
                return new EventVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int i) {
            if (getItemViewType(i) == TYPE_HEADER) {
                ((TextView) h.itemView).setText((String) rows.get(i));
                h.itemView.setOnClickListener(null);
            } else {
                Event e = (Event) rows.get(i);
                EventVH vh = (EventVH) h;

                vh.title.setText(e.getName());
                vh.date.setText("Maybe Recurring: " + e.getEventDate());
                vh.open.setText("Registration Opens: " + e.getRegistrationOpens());
                vh.deadline.setText("Registration Deadline: " + e.getRegistrationCloses());
                vh.cost.setText("Cost: " + e.getCost());
                vh.spots.setText("Spots: " + e.getMaxSpots());

                // Click opens EventDetailsOrganizerActivity
                vh.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, EventDetailsOrganizerActivity.class);
                    // Pass the document id — details activity will fetch the rest from Firestore
                    intent.putExtra("eventId", e.getEventId());
                    context.startActivity(intent);
                });
            }
        }

        @Override public int getItemCount(){ return rows.size(); }
    }
}
//dhruv