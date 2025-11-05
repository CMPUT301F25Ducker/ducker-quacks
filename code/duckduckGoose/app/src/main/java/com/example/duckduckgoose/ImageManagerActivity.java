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

public class ImageManagerActivity extends AppCompatActivity implements ProfileSheet.OnProfileInteractionListener {

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

        TopBarWiring.attachProfileSheet(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rvImages = findViewById(R.id.rvImages);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns grid

        List<Integer> images = new ArrayList<>();
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);

        ImageManagerAdapter adapter = new ImageManagerAdapter(images);
        rvImages.setAdapter(adapter);
    }

    @Override
    public void onProfileDeleted(String userId) {
        // Not applicable to Image Manager
    }

    @Override
    public void onEventsButtonClicked(String userId) {
        // Not applicable to Image Manager
    }
}
