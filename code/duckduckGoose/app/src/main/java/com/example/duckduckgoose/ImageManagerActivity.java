package com.example.duckduckgoose;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImageManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_manager);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rvImages = findViewById(R.id.rvImages);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2));

        List<Integer> images = new ArrayList<>();
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);
        images.add(android.R.drawable.ic_menu_gallery);

        ImageManagerAdapter adapter = new ImageManagerAdapter(images);
        rvImages.setAdapter(adapter);
    }
}
