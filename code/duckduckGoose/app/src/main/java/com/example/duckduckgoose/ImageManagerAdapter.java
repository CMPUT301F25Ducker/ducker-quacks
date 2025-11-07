/**
 * @file ImageManagerAdapter.java
 * @brief RecyclerView adapter for previewing and removing image resources.
 *
 * Binds a list of drawable resource IDs into simple preview rows with a
 * delete action for each item.
 *
 * @author
 *      DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * @class ImageManagerAdapter
 * @brief Adapter that shows image previews and supports per-item deletion.
 */
public class ImageManagerAdapter extends RecyclerView.Adapter<ImageManagerAdapter.ViewHolder> {

    /** Backing list of drawable resource IDs. */
    private final List<Integer> images;

    /**
     * @brief Constructs an adapter with the given image list.
     * @param images List of drawable resource IDs to display.
     */
    public ImageManagerAdapter(List<Integer> images) {
        this.images = images;
    }

    /**
     * @brief Inflates an image row view holder.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    /**
     * @brief Binds the image preview and wires the delete click handler.
     * @param holder Row view holder.
     * @param position Adapter position.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imgPreview.setImageResource(images.get(position));
        holder.txtImageLabel.setText("Image " + (position + 1));

        /** @brief Delete current image and notify item removal. */
        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                images.remove(currentPosition);
                notifyItemRemoved(currentPosition);
            }
        });
    }

    /**
     * @brief Returns the number of images in the list.
     */
    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * @class ViewHolder
     * @brief Holds references to views for an image row.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview;
        TextView txtImageLabel;
        View btnDelete;

        /**
         * @brief Binds view references for the row.
         * @param itemView Root row view.
         */
        ViewHolder(View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            txtImageLabel = itemView.findViewById(R.id.txtImageLabel);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
