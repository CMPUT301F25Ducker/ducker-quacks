/**
 * RecyclerView adapter for previewing and removing image resources.
 *
 * Binds a list of image items (URL + Event ID) into simple preview rows with a
 * delete action for each item. Uses Glide for image loading.
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

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adapter that shows image previews and supports per-item deletion.
 */
public class ImageManagerAdapter extends RecyclerView.Adapter<ImageManagerAdapter.ViewHolder> {

    /**
     * Data class representing an image item with its associated event.
     */
    public static class ImageItem {
        /** The event ID this image belongs to. */
        public String eventId;

        /** The URL of the image. */
        public String imageUrl;

        /**
         * Constructs an ImageItem with the given event ID and image URL.
         *
         * @param eventId - The ID of the event this image belongs to
         * @param imageUrl - The URL of the image
         */
        public ImageItem(String eventId, String imageUrl) {
            this.eventId = eventId;
            this.imageUrl = imageUrl;
        }
    }

    /**
     * Callback interface for handling image deletion events.
     */
    public interface OnImageDeleteListener {
        /**
         * Called when an image should be deleted.
         *
         * @param item - The image item to delete
         * @param position - The position of the item in the list
         */
        void onDelete(ImageItem item, int position);
    }

    /** List of image items to display. */
    private final List<ImageItem> imageItems;

    /** Listener for handling delete actions. */
    private final OnImageDeleteListener deleteListener;

    /**
     * Constructs an adapter with the given image list and delete listener.
     *
     * @param imageItems - List of ImageItem objects to display
     * @param deleteListener - Listener for handling delete actions
     */
    public ImageManagerAdapter(List<ImageItem> imageItems, OnImageDeleteListener deleteListener) {
        this.imageItems = imageItems;
        this.deleteListener = deleteListener;
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
     * Binds image data to a view holder and sets up the delete listener.
     *
     * @param holder - The ViewHolder to bind data to
     * @param position - The position in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageItem item = imageItems.get(position);

        holder.txtImageLabel.setText("Image " + (position + 1));

        Glide.with(holder.itemView.getContext())
                .load(item.imageUrl)
                .placeholder(R.drawable.poolphoto)
                .centerCrop()
                .into(holder.imgPreview);

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(item, holder.getAdapterPosition());
            }
        });
    }

    /**
     * Returns the total number of image items.
     *
     * @return The number of images in the list
     */
    @Override
    public int getItemCount() {
        return imageItems.size();
    }

    /**
     * Holds references to views for an image row.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /** ImageView for displaying the image preview. */
        ImageView imgPreview;

        /** TextView for displaying the image label. */
        TextView txtImageLabel;

        /** Button for deleting the image. */
        View btnDelete;

        /**
         * Initializes view references for an image row.
         *
         * @param itemView - The root view of the image item
         */
        ViewHolder(View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            txtImageLabel = itemView.findViewById(R.id.txtImageLabel);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}