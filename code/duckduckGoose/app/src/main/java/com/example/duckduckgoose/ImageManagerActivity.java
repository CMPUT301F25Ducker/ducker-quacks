/**
 * @file ImageManagerActivity.java
 * @brief Activity for displaying and managing a grid of stored images.
 *
 * Provides a simple interface to preview and remove image items in a grid layout.
 * Integrates with the app’s top bar and profile sheet for consistent navigation.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsetsController;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @class ImageManagerActivity
 * @brief Displays an editable image grid and attaches top bar profile actions.
 */
public class ImageManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

    /**
     * @brief Initializes the grid of images and top bar wiring.
     * @param savedInstanceState State bundle for recreation.
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
        setContentView(R.layout.activity_image_manager);

        // Attach top bar profile sheet
        TopBarWiring.attachProfileSheet(this);

        /** @brief Back button: return to previous screen. */
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // RecyclerView setup
        RecyclerView rvImages = findViewById(R.id.rvImages);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2)); // 2-column grid

        // Placeholder images for demonstration
        List<Integer> images = new ArrayList<>();
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);

        // Bind adapter
        ImageManagerAdapter adapter = new ImageManagerAdapter(images);
        rvImages.setAdapter(adapter);
    }

    /**
     * @brief Unused — profile deletion not applicable in this screen.
     */
    @Override
    public void onProfileDeleted(String userId) {
        // Not applicable to Image Manager
    }

    /**
     * @brief Unused — event navigation not applicable in this screen.
     */
    @Override
    public void onEventsButtonClicked(String userId) {
        // Not applicable to Image Manager
    }
}
