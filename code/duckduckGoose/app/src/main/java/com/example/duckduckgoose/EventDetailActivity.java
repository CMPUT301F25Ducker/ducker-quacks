package com.example.duckduckgoose;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class EventDetailActivity extends AppCompatActivity {

    enum State { UNDECIDED, NOT_IN_CIRCLE, LEAVE_CIRCLE, DUCK, GOOSE }

    private State currentState = State.UNDECIDED;
    private boolean isOrganizerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MUST inflate layout first so views exist
        setContentView(R.layout.activity_event_detail);

        // Check if organizer mode
        isOrganizerMode = AppConfig.LOGIN_MODE.equals("ORGANIZER");

        TopBarWiring.attachProfileSheet(this);

        // --- Top bar nav (My Events / Events) ---
        View tbMy = findViewById(R.id.btnMyEvents);
        if (tbMy != null) {
            tbMy.setOnClickListener(v -> {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra("startOn", "MY_EVENTS");
                startActivity(i);
            });
        }

        // ---- read extras ----
        Intent i = getIntent();
        String title    = i.getStringExtra("title");
        String dateText = i.getStringExtra("dateText");
        long   open     = i.getLongExtra("open", 0L);
        long   deadline = i.getLongExtra("deadline", 0L);
        String cost     = i.getStringExtra("cost");
        String spots    = i.getStringExtra("spots");
        int    poster   = i.getIntExtra("posterRes", R.drawable.poolphoto);
        int    stateInt = i.getIntExtra("state", -1);

        // ---- bind text views ----
        TextView tvTitle     = findViewById(R.id.txtEventTitle);
        TextView tvDesc      = findViewById(R.id.txtDescription);
        TextView tvDates     = findViewById(R.id.txtDates);
        TextView tvOpen      = findViewById(R.id.txtOpen);
        TextView tvDeadline  = findViewById(R.id.txtDeadline);
        TextView tvCost      = findViewById(R.id.txtCost);
        TextView tvSpots     = findViewById(R.id.txtSpots);

        if (tvTitle != null)    tvTitle.setText(title != null ? title : "Event");
        if (tvDesc != null)     tvDesc.setText("Event details will be loaded from backend later.");
        if (tvDates != null)    tvDates.setText(dateText != null ? dateText : "TBD");
        if (tvOpen != null)     tvOpen.setText("Registration Opens: " + (open == 0 ? "TBD" : new java.util.Date(open)));
        if (tvDeadline != null) tvDeadline.setText("Registration Deadline: " + (deadline == 0 ? "TBD" : new java.util.Date(deadline)));
        if (tvCost != null)     tvCost.setText("Cost: " + (cost == null ? "—" : cost));
        if (tvSpots != null)    tvSpots.setText("Spots: " + (spots == null ? "—" : spots));

        // ---- optional image gallery ----
        LinearLayout gallery = findViewById(R.id.imageGallery);
        if (gallery != null) {
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int heightPx = (int) (280 * getResources().getDisplayMetrics().density); // ~280dp

            int[] photos = { poster, R.drawable.poolphoto, R.drawable.poolphoto };
            for (int res : photos) {
                ImageView img = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(screenW, heightPx);
                lp.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
                img.setLayoutParams(lp);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setImageResource(res);
                gallery.addView(img);
            }
        }

        // ---- determine initial state & apply ----
        currentState = (stateInt >= 0 && stateInt < State.values().length)
                ? State.values()[stateInt]
                : pickStateFromTitle(title);
        
        if (isOrganizerMode) {
            setupOrganizerButtons(title, dateText, open, deadline, cost, spots);
        } else {
            applyState(currentState);
            setupEntrantButtons();
        }
    }

    private void setupOrganizerButtons(String title, String dateText, long open, long deadline, String cost, String spots) {
        // Hide entrant buttons
        View areaButtons = findViewById(R.id.areaButtons);
        View singleArea = findViewById(R.id.singleCtaArea);
        View organizerArea = findViewById(R.id.organizerButtonsArea);
        
        if (areaButtons != null) areaButtons.setVisibility(View.GONE);
        if (singleArea != null) singleArea.setVisibility(View.GONE);
        if (organizerArea != null) organizerArea.setVisibility(View.VISIBLE);

        // Setup organizer buttons
        com.google.android.material.button.MaterialButton btnEdit = findViewById(R.id.btnEditEventDetail);
        com.google.android.material.button.MaterialButton btnAttendeeManager = findViewById(R.id.btnAttendeeManagerDetail);
        com.google.android.material.button.MaterialButton btnDelete = findViewById(R.id.btnDeleteEventDetail);

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, EventEditActivity.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("title", title);
                intent.putExtra("date", dateText);
                intent.putExtra("cost", cost);
                intent.putExtra("spots", spots);
                startActivity(intent);
            });
        }

        if (btnAttendeeManager != null) {
            btnAttendeeManager.setOnClickListener(v -> {
                Intent intent = new Intent(this, AttendeeManagerActivity.class);
                intent.putExtra("eventTitle", title);
                startActivity(intent);
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                // In production, show confirmation dialog
                finish();
            });
        }
    }

    private void setupEntrantButtons() {
        // Hide organizer buttons
        View organizerArea = findViewById(R.id.organizerButtonsArea);
        if (organizerArea != null) organizerArea.setVisibility(View.GONE);

        // ---- wire Accept/Decline with navigation for DUCK -> Events tab ----
        View areaButtons = findViewById(R.id.areaButtons);
        if (areaButtons != null && areaButtons.getVisibility() == View.VISIBLE) {
            com.google.android.material.button.MaterialButton btnDecline = findViewById(R.id.btnLeft);
            com.google.android.material.button.MaterialButton btnAccept  = findViewById(R.id.btnRight);

            if (btnDecline != null) {
                btnDecline.setOnClickListener(v ->
                        animateTap(v, () -> {
                            currentState = State.DUCK;      // Decline -> DUCK
                            applyState(currentState);
                        })
                );
            }
            if (btnAccept != null) {
                btnAccept.setOnClickListener(v ->
                        animateTap(v, () -> {
                            currentState = State.GOOSE;     // Accept -> GOOSE
                            applyState(currentState);
                        })
                );
            }
        }

        // Single CTA (optional toggle)
        com.google.android.material.button.MaterialButton btnSingle = findViewById(R.id.btnSingleCta);
        View singleArea = findViewById(R.id.singleCtaArea);
        if (btnSingle != null && singleArea != null && singleArea.getVisibility() == View.VISIBLE) {
            btnSingle.setOnClickListener(v ->
                    animateTap(v, () -> {
                        if (currentState == State.NOT_IN_CIRCLE) currentState = State.LEAVE_CIRCLE;
                        else if (currentState == State.LEAVE_CIRCLE) currentState = State.NOT_IN_CIRCLE;
                        applyState(currentState);
                    })
            );
        }
    }

    private void animateTap(View v, Runnable after) {
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).withEndAction(after).start()
        ).start();
    }

    private State pickStateFromTitle(String title) {
        if (title == null) return State.UNDECIDED;
        String t = title.toLowerCase(java.util.Locale.ROOT);
        if (t.contains("duck"))  return State.DUCK;
        if (t.contains("goose")) return State.GOOSE;
        if (t.contains("enter")) return State.NOT_IN_CIRCLE;
        if (t.contains("leave")) return State.LEAVE_CIRCLE;
        return State.UNDECIDED;
    }

    private void applyState(State s) {
        View twoBtns = findViewById(R.id.areaButtons);
        View single  = findViewById(R.id.singleCtaArea);
        com.google.android.material.button.MaterialButton btn = findViewById(R.id.btnSingleCta);
        if (twoBtns == null || single == null || btn == null) return;

        int charcoal = ContextCompat.getColor(this, R.color.fg_charcoal);
        int white    = ContextCompat.getColor(this, R.color.fg_white);

        switch (s) {
            case UNDECIDED:
                twoBtns.setVisibility(View.VISIBLE);
                single.setVisibility(View.GONE);
                break;

            case NOT_IN_CIRCLE:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Enter Registration Circle");
                btn.setBackgroundResource(R.drawable.btn_primary_green);
                btn.setTextColor(charcoal);
                break;

            case LEAVE_CIRCLE:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Leave Registration Circle");
                btn.setBackgroundResource(R.drawable.btn_secondary_silver);
                btn.setTextColor(charcoal);
                break;

            case DUCK:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Duck");
                btn.setBackgroundResource(R.drawable.btn_filled_brick);
                btn.setTextColor(white);
                break;

            case GOOSE:
                twoBtns.setVisibility(View.GONE);
                single.setVisibility(View.VISIBLE);
                btn.setText("Goose");
                btn.setBackgroundResource(R.drawable.btn_primary_green);
                btn.setTextColor(charcoal);
                break;
        }
    }
}
