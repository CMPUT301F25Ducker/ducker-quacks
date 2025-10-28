package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    enum Screen { LOGIN, EVENT_LIST, MY_EVENTS, EVENT_DETAIL, NOTIFICATIONS }
    private Screen current = Screen.LOGIN;

    // Login-only views (valid only when LOGIN layout is set)
    private View btnSignIn, btnCreateAccount;
    private CardView sheetSignIn, sheetCreate;
    private TextView btnSheetCancel1, btnSheetCancel2;
    private MaterialButton btnSheetSignIn, btnCreateSubmit;

    private void wireTopBarNav() {
        View btnMyEvents = findViewById(R.id.btnMyEvents);
        if (btnMyEvents != null) btnMyEvents.setOnClickListener(v -> showMyEvents());
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
        showLogin();
        handleStartOnIntent();

        // Intercept system back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                switch (current) {
                    case LOGIN:
                        // If a sheet is open, close it instead of exiting
                        if (sheetSignIn != null && sheetSignIn.getVisibility() == View.VISIBLE) {
                            sheetSignIn.setVisibility(View.GONE);
                            if (btnSignIn != null) btnSignIn.setVisibility(View.VISIBLE);
                            if (btnCreateAccount != null) btnCreateAccount.setVisibility(View.VISIBLE);
                            return;
                        }
                        if (sheetCreate != null && sheetCreate.getVisibility() == View.VISIBLE) {
                            sheetCreate.setVisibility(View.GONE);
                            if (btnSignIn != null) btnSignIn.setVisibility(View.VISIBLE);
                            if (btnCreateAccount != null) btnCreateAccount.setVisibility(View.VISIBLE);
                            return;
                        }
                        // Nothing to collapse → allow default (exit)
                        setEnabled(false);
                        onBackPressed(); // let system finish activity
                        break;

                    case EVENT_LIST:
                        showLogin();
                        break;

                    case MY_EVENTS:
                        showEventList();
                        break;

                    case EVENT_DETAIL:
                    case NOTIFICATIONS:
                        // go back to event list by default
                        showEventList();
                        break;
                }
            }
        });
    }

    /* ----------------- Screens ----------------- */

    private void showLogin() {
        setContentView(R.layout.activity_login);
        current = Screen.LOGIN;

        btnSignIn = findViewById(R.id.btnSignIn);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        sheetSignIn = findViewById(R.id.sheetSignIn);
        sheetCreate = findViewById(R.id.sheetCreate);
        btnSheetCancel1 = findViewById(R.id.btnSheetCancel1);
        btnSheetCancel2 = findViewById(R.id.btnSheetCancel2);
        btnSheetSignIn = findViewById(R.id.btnSheetSignIn);
        btnCreateSubmit = findViewById(R.id.btnCreateSubmit);

        // 🔽 Account Type dropdown for the Create Account sheet
        MaterialAutoCompleteTextView acct = findViewById(R.id.edtAccountType);
        if (acct != null) {
            ArrayAdapter<String> acctAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    getResources().getStringArray(R.array.account_types)
            );
            acct.setAdapter(acctAdapter);
            // Optional: preselect the first item
            // acct.setText(acctAdapter.getItem(0), false);
        }

        // open sheets
        btnSignIn.setOnClickListener(v -> {
            sheetSignIn.setVisibility(View.VISIBLE);
            sheetCreate.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });
        btnCreateAccount.setOnClickListener(v -> {
            sheetCreate.setVisibility(View.VISIBLE);
            sheetSignIn.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            btnCreateAccount.setVisibility(View.GONE);
        });

        // close sheets
        btnSheetCancel1.setOnClickListener(v -> {
            sheetSignIn.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.VISIBLE);
            btnCreateAccount.setVisibility(View.VISIBLE);
        });
        btnSheetCancel2.setOnClickListener(v -> {
            sheetCreate.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.VISIBLE);
            btnCreateAccount.setVisibility(View.VISIBLE);
        });

        // continue to app
        btnSheetSignIn.setOnClickListener(v -> showEventList());
        btnCreateSubmit.setOnClickListener(v -> showEventList());
    }

    private void showEventList() {
        setContentView(R.layout.activity_event_list);
        current = Screen.EVENT_LIST;
        wireTopBarNav();

        // top bar nav example (optional):
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


        // Sort dropdown
        MaterialAutoCompleteTextView drop = findViewById(R.id.dropSortMy);
        if (drop != null) {
            String[] sorts = {"Date (Soonest)", "Date (Latest)", "Registration Opens", "Registration Deadline", "Cost"};
            drop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sorts));
        }

        // One list with headers + cards
        RecyclerView rv = findViewById(R.id.rvMyEvents);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
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
}
