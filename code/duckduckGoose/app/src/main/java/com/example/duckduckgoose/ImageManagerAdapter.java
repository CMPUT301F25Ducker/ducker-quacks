package com.example.duckduckgoose;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ImageManagerAdapter extends RecyclerView.Adapter<ImageManagerAdapter.ViewHolder> {

    private final List<Integer> images;

    public ImageManagerAdapter(List<Integer> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageResource(images.get(position));
        holder.imageLabel.setText("Image " + (position + 1));
        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                images.remove(currentPosition);
                notifyItemRemoved(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView imageLabel;
        View btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            imageLabel = itemView.findViewById(R.id.imageLabel);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
