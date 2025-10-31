package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Default to event list, but check intent for specific start screen
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

        // Fake list using your card layout
        RecyclerView rv = findViewById(R.id.recyclerEvents);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            List<String> fake = Arrays.asList(
                    "City Swim Classic", "Downtown 5K Run", "Autumn Cycling Tour",
                    "Campus Fun Run", "Community Tri"
            );
            rv.setAdapter(new SimpleEventAdapter(fake));
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

            if (AppConfig.LOGIN_MODE.equals("ORGANIZER")) {
                // Organizer view: Show Past Events and Current Events sections
                List<Object> rows = new ArrayList<>();
                rows.add("Past Events:");
                rows.add(new Event("Example Event 1", "Nov 20–22", "Nov 1", "Nov 15", "$25", "12/40"));
                rows.add("Current Events:");
                rows.add(new Event("Example Event", "Dec 3", "Nov 10", "Dec 1", "Free", "80/100"));
                rv.setAdapter(new OrganizerEventAdapter(rows, this));
            } else {
                // Entrant view: Show Pre-Registration and Past Registration sections
                List<Object> rows = new ArrayList<>();
                rows.add("Pre-Registration Deadline");
                rows.add(new Event("City Swim Classic", "Nov 20–22", "Nov 1", "Nov 15", "$25", "12/40"));
                rows.add(new Event("Downtown 5K Run", "Dec 3", "Nov 10", "Dec 1", "Free", "80/100"));
                rows.add("Past Registration Deadline");
                rows.add(new Event("Autumn Cycling Tour", "Oct 12", "Sep 25", "Oct 5 (Closed)", "$15", "Filled"));
                rows.add(new Event("Campus Fun Run", "Sep 28", "Sep 1", "Sep 20 (Closed)", "$10", "Filled"));
                rv.setAdapter(new SectionedEventAdapter(rows));
            }
        }
    }

    /* ----------------- Simple demo adapters ----------------- */

    static class Event {
        String title, date, open, deadline, cost, spots;
        Event(String t, String d, String o, String dl, String c, String s){
            title=t; date=d; open=o; deadline=dl; cost=c; spots=s;
        }
    }

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
            h.date.setText("Date: TBD");
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

                vh.title.setText(e.title);
                vh.date.setText("Date: " + e.date);
                vh.open.setText("Registration Opens: " + e.open);
                vh.deadline.setText("Registration Deadline: " + e.deadline);
                vh.cost.setText("Cost: " + e.cost);
                vh.spots.setText("Spots: " + e.spots);

                vh.itemView.setOnClickListener(v -> {
                    android.content.Context c = v.getContext();
                    android.content.Intent intent =
                            new android.content.Intent(c, EventDetailActivity.class);

                    // Common extras
                    intent.putExtra("title",    e.title);
                    intent.putExtra("dateText", e.date);
                    intent.putExtra("open",     0L);
                    intent.putExtra("deadline", 0L);
                    intent.putExtra("cost",     e.cost);
                    intent.putExtra("spots",    e.spots);
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

                vh.title.setText(e.title);
                vh.date.setText("Date (Maybe Reoccurring): " + e.date);
                vh.open.setText("Registration Opens: " + e.open);
                vh.deadline.setText("Registration Deadline: " + e.deadline);
                vh.cost.setText("Cost: " + e.cost);
                vh.spots.setText("Spots: " + e.spots);

                // Click opens EventDetailsOrganizerActivity
                vh.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, EventDetailsOrganizerActivity.class);
                    intent.putExtra("title", e.title);
                    intent.putExtra("dateText", e.date);
                    intent.putExtra("open", e.open);
                    intent.putExtra("deadline", e.deadline);
                    intent.putExtra("cost", e.cost);
                    intent.putExtra("spots", e.spots);
                    context.startActivity(intent);
                });
            }
        }

        @Override public int getItemCount(){ return rows.size(); }
    }
}
