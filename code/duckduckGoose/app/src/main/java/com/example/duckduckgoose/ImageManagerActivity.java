/**
 * Activity for displaying and managing a grid of stored images.
 *
 * Provides a simple interface to preview and remove image items in a grid layout.
 * Fetches all images from Firestore events and allows permanent deletion.
 *
 * @author DuckDuckGoose Development Team
 */
package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays an editable image grid and attaches top bar profile actions.
 */
public class ImageManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    /** RecyclerView for displaying the image grid. */
    private RecyclerView rvImages;

    /** Adapter for managing image items in the RecyclerView. */
    private ImageManagerAdapter adapter;

    /** List of all image items loaded from Firestore. */
    private List<ImageManagerAdapter.ImageItem> allImageItems;

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /**
     * Initializes the activity, sets up the image grid, and loads images.
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
            controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_manager);

        // Attach top bar profile sheet
        TopBarWiring.attachProfileSheet(this);

        // Back button handler: returns to the previous screen
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvImages = findViewById(R.id.rvImages);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2));

        allImageItems = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        // Bind adapter
        adapter = new ImageManagerAdapter(allImageItems, (item, position) -> {
            deleteImageFromFirestore(item, position);
        });

        rvImages.setAdapter(adapter);

        fetchAllImages();
    }

    /**
     * Fetches all events, extracts their image URLs, and populates the list.
     */
    private void fetchAllImages() {
        db.collection("events").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allImageItems.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> eventImages = (List<String>) doc.get("imagePaths");
                        if (eventImages != null) {
                            // Pair the Event ID with the URL so we can delete it later
                            for (String url : eventImages) {
                                allImageItems.add(new ImageManagerAdapter.ImageItem(doc.getId(), url));
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (allImageItems.isEmpty()) {
                        Toast.makeText(this, "No images found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load images: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Deletes an image from Firestore and removes it from the UI.
     *
     * @param item - The image item to delete
     * @param position - The position of the item in the list
     */
    private void deleteImageFromFirestore(ImageManagerAdapter.ImageItem item, int position) {
        if (item.eventId == null || item.imageUrl == null) return;

        // 1. Remove from Firestore
        db.collection("events").document(item.eventId)
                .update("imagePaths", FieldValue.arrayRemove(item.imageUrl))
                .addOnSuccessListener(aVoid -> {
                    // 2. Only remove from UI if DB update succeeded
                    if (position >= 0 && position < allImageItems.size()) {
                        allImageItems.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, allImageItems.size());
                    }
                    Toast.makeText(this, "Image deleted permanently", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Not applicable to Image Manager - profile deletion not used here.
     *
     * @param userId - ID of the user profile to delete
     */
    @Override
    public void onProfileDeleted(String userId) {
        // Not applicable to Image Manager
    }

    /**
     * Not applicable to Image Manager - events button not used here.
     *
     * @param userId - ID of the user whose events button was clicked
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        // Not applicable to Image Manager
    }
}