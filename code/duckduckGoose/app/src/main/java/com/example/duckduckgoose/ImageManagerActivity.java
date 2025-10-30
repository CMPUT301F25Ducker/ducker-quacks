package com.example.duckduckgoose;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ImageManagerActivity extends AppCompatActivity {

    private ImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_manager);

        TopBarWiring.attachProfileSheet(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        RecyclerView rv = findViewById(R.id.rvImages);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ImageAdapter();
            rv.setAdapter(adapter);
        }
    }

    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

        private List<String> images = new ArrayList<>();

        ImageAdapter() {
            for (int i = 1; i <= 5; i++) {
                images.add("Image " + i);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_manager, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.txtImageName.setText(images.get(position));
            holder.btnDelete.setOnClickListener(v -> {
                images.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtImageName;
            ImageView imgPreview;
            MaterialButton btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                txtImageName = itemView.findViewById(R.id.txtImageName);
                imgPreview = itemView.findViewById(R.id.imgPreview);
                btnDelete = itemView.findViewById(R.id.btnDeleteImage);
            }
        }
    }
}