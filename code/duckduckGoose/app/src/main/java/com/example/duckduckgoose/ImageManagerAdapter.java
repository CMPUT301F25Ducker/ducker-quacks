/**
 * RecyclerView adapter for previewing and removing image resources.
 *
 * Binds a list of drawable resource IDs into simple preview rows with a
 * delete action for each item.
 *
 * @author DuckDuckGoose Development Team
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
 * Adapter that shows image previews and supports per-item deletion.
 */
public class ImageManagerAdapter extends RecyclerView.Adapter<ImageManagerAdapter.ViewHolder> {

    /** Backing list of drawable resource IDs. */
    private final List<Integer> images;

    /**
     * Constructs an adapter with the given image list.
     * 
     * @param images - List of drawable resource IDs to display
     */
    public ImageManagerAdapter(List<Integer> images) {
        this.images = images;
    }

    /**
     * Inflates an image row view holder.
     *
     * @param parent - The parent ViewGroup
     * @param viewType - The view type to create
     * @return A new ViewHolder for the image row
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the image preview and wires the delete click handler.
     * 
     * @param holder - Row view holder
     * @param position - Adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imgPreview.setImageResource(images.get(position));
        holder.txtImageLabel.setText("Image " + (position + 1));

        // Delete handler: Removes image and updates the adapter
        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                images.remove(currentPosition);
                notifyItemRemoved(currentPosition);
            }
        });
    }

    /**
     * Returns the number of images in the list.
     * 
     * @return The total number of images
     */
    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * Holds references to views for an image row.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview;
        TextView txtImageLabel;
        View btnDelete;

        /**
         * Binds view references for the row.
         * 
         * @param itemView - Root row view
         */
        ViewHolder(View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            txtImageLabel = itemView.findViewById(R.id.txtImageLabel);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
