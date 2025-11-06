package com.example.duckduckgoose;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Activity for creating and editing events (Organizer mode)
 * Shows form with: Event Name, Spots, Cost, Event Date, Registration Opens/Closes,
 * Images/Posters, Geolocation checkbox, and action buttons
 */
public class EventEditActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    
    private String mode; // "create" or "edit"
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private String eventId; // when editing, this identifies the Firestore document

    // Form fields
    private EditText edtEventName;
    private EditText edtSpots;
    private EditText edtCost;
    private EditText txtEventDate;
    private EditText txtRegOpens;
    private EditText txtRegCloses;
    private CheckBox chkGeolocation;
    
    // Image management
    private LinearLayout imageContainer;
    private List<String> imagePaths = new ArrayList<>();
    private CardView sheetImageSelect;
    
    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    // Buttons
    private MaterialButton btnSaveChanges;
    private MaterialButton btnCreateEvent;
    private MaterialButton btnCancel;
    private MaterialButton btnDeleteEvent;
    private MaterialButton btnAttendeeManager;
    private MaterialButton btnAddImage;

    // Date selection
    private Calendar eventDate = Calendar.getInstance();
    private Calendar regOpensDate = Calendar.getInstance();
    private Calendar regClosesDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy", Locale.US);

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
        setContentView(R.layout.activity_event_edit);

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        addImageToContainer(imageUri.toString());
                        Toast.makeText(this, "Image added", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        // Get mode
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "create";

        initializeViews();
        setupClickListeners();
        
        // Update title based on mode
        TextView txtTopBarTitle = findViewById(R.id.txtTopBarTitle);
        
        if (mode.equals("edit")) {
            if (txtTopBarTitle != null) txtTopBarTitle.setText("Edit Event");
            // allow caller to pass the Firestore document id to edit
            String incomingId = getIntent().getStringExtra("eventId");
            if (incomingId != null) eventId = incomingId;
            loadEventData();
            btnCreateEvent.setVisibility(View.GONE);
            btnSaveChanges.setVisibility(View.VISIBLE);
            btnDeleteEvent.setVisibility(View.GONE);
            btnAttendeeManager.setVisibility(View.GONE);
        } else {
            if (txtTopBarTitle != null) txtTopBarTitle.setText("New Event");
            btnSaveChanges.setVisibility(View.GONE);
            btnCreateEvent.setVisibility(View.VISIBLE);
            btnDeleteEvent.setVisibility(View.GONE);
            btnAttendeeManager.setVisibility(View.GONE);
        }

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void initializeViews() {
        edtEventName = findViewById(R.id.edtEventName);
        edtSpots = findViewById(R.id.edtSpots);
        edtCost = findViewById(R.id.edtCost);
        txtEventDate = findViewById(R.id.txtEventDate);
        txtRegOpens = findViewById(R.id.txtRegOpens);
        txtRegCloses = findViewById(R.id.txtRegCloses);
        chkGeolocation = findViewById(R.id.chkGeolocation);
        
        imageContainer = findViewById(R.id.imageContainer);
        sheetImageSelect = findViewById(R.id.sheetImageSelect);
        
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnCancel = findViewById(R.id.btnCancel);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        btnAttendeeManager = findViewById(R.id.btnAttendeeManager);
        btnAddImage = findViewById(R.id.btnAddImage);
    }

    private void setupClickListeners() {
        // Date pickers
        txtEventDate.setOnClickListener(v -> showDatePicker(eventDate, txtEventDate, "Event Date"));
        txtRegOpens.setOnClickListener(v -> showDatePicker(regOpensDate, txtRegOpens, "Registration Opens"));
        txtRegCloses.setOnClickListener(v -> showDatePicker(regClosesDate, txtRegCloses, "Registration Closes"));

        // Image management - Open gallery directly
        btnAddImage.setOnClickListener(v -> openImagePicker());

        // Action buttons
        btnSaveChanges.setOnClickListener(v -> saveEvent());
        btnCreateEvent.setOnClickListener(v -> createEvent());
        btnCancel.setOnClickListener(v -> finish());
        btnDeleteEvent.setOnClickListener(v -> deleteEvent());
        btnAttendeeManager.setOnClickListener(v -> openAttendeeManager());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showDatePicker(Calendar calendar, EditText targetView, String title) {
        DatePickerDialog picker = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                targetView.setText(dateFormat.format(calendar.getTime()));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.setTitle(title);
        picker.show();
    }

    private void showImageSelectSheet() {
        if (sheetImageSelect != null) {
            sheetImageSelect.setVisibility(View.VISIBLE);
            
            // Close button
            View btnCloseSheet = findViewById(R.id.btnCloseImageSheet);
            if (btnCloseSheet != null) {
                btnCloseSheet.setOnClickListener(v -> sheetImageSelect.setVisibility(View.GONE));
            }

            // Image/poster select button
            MaterialButton btnSelectImage = findViewById(R.id.btnSelectImagePoster);
            if (btnSelectImage != null) {
                btnSelectImage.setOnClickListener(v -> {
                    // In a real app, this would open image picker
                    addImageToContainer("Sample Image " + (imagePaths.size() + 1));
                    sheetImageSelect.setVisibility(View.GONE);
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void addImageToContainer(String imagePath) {
        imagePaths.add(imagePath);
        updateImageDisplay();
    }

    private void updateImageDisplay() {
        imageContainer.removeAllViews();
        
        for (int i = 0; i < imagePaths.size() && i < 3; i++) {
            View imageItem = getLayoutInflater().inflate(R.layout.item_image, imageContainer, false);
            
            TextView txtImageLabel = imageItem.findViewById(R.id.txtImageLabel);
            MaterialButton btnDeleteImage = imageItem.findViewById(R.id.btnDeleteImage);
            
            final int index = i;
            txtImageLabel.setText("Image " + (i + 1));
            btnDeleteImage.setOnClickListener(v -> {
                imagePaths.remove(index);
                updateImageDisplay();
            });
            
            imageContainer.addView(imageItem);
        }
    }

    private void loadEventData() {
        Intent intent = getIntent();

        // If an eventId was provided (preferred), load the document from Firestore
        if (eventId != null) {
            eventsRef.document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                if (event.getName() != null) edtEventName.setText(event.getName());
                                if (event.getMaxSpots() != null) edtSpots.setText(event.getMaxSpots());
                                if (event.getCost() != null) {
                                    String cost = event.getCost().replace("$", "").trim();
                                    if (!cost.equals("Free") && !cost.equals("—")) edtCost.setText(cost);
                                }
                                if (event.getEventDate() != null) txtEventDate.setText(event.getEventDate());
                                if (event.getRegistrationOpens() != null) txtRegOpens.setText(event.getRegistrationOpens());
                                if (event.getRegistrationCloses() != null) txtRegCloses.setText(event.getRegistrationCloses());
                                chkGeolocation.setChecked(event.isGeolocationEnabled());

                                // load image paths if present
                                if (event.getImagePaths() != null) {
                                    imagePaths.clear();
                                    imagePaths.addAll(event.getImagePaths());
                                }
                                updateImageDisplay();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            return;
        }

        // Legacy fallback: populate from extras if no eventId provided
        String title = intent.getStringExtra("title");
        String spots = intent.getStringExtra("spots");
        String cost = intent.getStringExtra("cost");

        if (title != null) edtEventName.setText(title);
        if (spots != null) {
            // Extract number from "12/40" format
            String[] parts = spots.split("/");
            if (parts.length > 1) {
                edtSpots.setText(parts[1]);
            }
        }
        if (cost != null) {
            // Remove $ sign if present
            cost = cost.replace("$", "").trim();
            if (!cost.equals("Free") && !cost.equals("—")) {
                edtCost.setText(cost);
            }
        }

        // Load sample images
        imagePaths.add("Sample Image 1");
        imagePaths.add("Sample Image 2");
        imagePaths.add("Sample Image 3");
        updateImageDisplay();
    }


    private void createEvent() {
        if (validateForm()) {
            storeInDB();
        }
    }

    private void storeInDB() {
        String name = edtEventName.getText().toString().trim();
        String spots = edtSpots.getText().toString().trim();
        String cost = edtCost.getText().toString().trim();
        String eventDateStr = txtEventDate.getText().toString().trim();
        String regOpensStr = txtRegOpens.getText().toString().trim();
        String regClosesStr = txtRegCloses.getText().toString().trim();
        boolean geolocation = chkGeolocation.isChecked();

        String newEventId = eventsRef.document().getId();

        Event newEvent = new Event(
                newEventId,
                name,
                eventDateStr,
                regOpensStr,
                regClosesStr,
                spots,
                cost,
                geolocation,
                imagePaths
        );

        // Attach organizer id if available so we can filter organizer-specific events
        try {
            com.google.firebase.auth.FirebaseUser fu = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (fu != null) newEvent.setOrganizerId(fu.getUid());
        } catch (Exception ex) {
            // If auth not available, leave organizerId null
        }

        eventsRef.document(newEventId)
                .set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    // Notify caller (if any) and finish
                    Intent result = new Intent();
                    result.putExtra("eventId", newEventId);
                    setResult(RESULT_OK, result);
                    Toast.makeText(this, "Event \"" + name + "\" created successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: Failed to create event. " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("EventEditActivity", "Error writing document", e);
                });
    }

    private void saveEvent() {
        if (validateForm()) {
            // If we have an eventId, update the existing Firestore document
            if (eventId != null) {
                String name = edtEventName.getText().toString().trim();
                String spots = edtSpots.getText().toString().trim();
                String cost = edtCost.getText().toString().trim();
                String eventDateStr = txtEventDate.getText().toString().trim();
                String regOpensStr = txtRegOpens.getText().toString().trim();
                String regClosesStr = txtRegCloses.getText().toString().trim();
                boolean geolocation = chkGeolocation.isChecked();

                Event updated = new Event(
                        eventId,
                        name,
                        eventDateStr,
                        regOpensStr,
                        regClosesStr,
                        spots,
                        cost,
                        geolocation,
                        imagePaths
                );
                // Ensure organizerId stays consistent by re-attaching current user if available
                try {
                    com.google.firebase.auth.FirebaseUser fu = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (fu != null) updated.setOrganizerId(fu.getUid());
                } catch (Exception ex) { }

                eventsRef.document(eventId)
                        .set(updated)
                        .addOnSuccessListener(aVoid -> {
                            // Notify caller and finish so UI can refresh if caller used startActivityForResult
                            Intent result = new Intent();
                            result.putExtra("eventId", eventId);
                            setResult(RESULT_OK, result);
                            Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                // No eventId — behave like create
                Toast.makeText(this, "No event id provided; cannot save edits.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteEvent() {
        // Show confirmation dialog in production
        if (eventId != null) {
            eventsRef.document(eventId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Intent result = new Intent();
                        result.putExtra("eventId", eventId);
                        result.putExtra("deleted", true);
                        setResult(RESULT_OK, result);
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void openAttendeeManager() {
        Intent intent = new Intent(this, AttendeeManagerActivity.class);
        if (eventId != null) intent.putExtra("eventId", eventId);
        else intent.putExtra("eventTitle", edtEventName.getText().toString());
        startActivity(intent);
    }

    private boolean validateForm() {
        String eventName = edtEventName.getText().toString().trim();
        
        if (eventName.isEmpty()) {
            Toast.makeText(this, "Please enter an event name", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    @Override
    public void onBackPressed() {
        if (sheetImageSelect != null && sheetImageSelect.getVisibility() == View.VISIBLE) {
            sheetImageSelect.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
