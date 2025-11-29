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
     * Constructs an adapter with the given image list.
     * 
     * @param images - List of drawable resource IDs to display
     */
    public static class ImageItem {
        public String eventId;
        public String imageUrl;

        public ImageItem(String eventId, String imageUrl) {
            this.eventId = eventId;
            this.imageUrl = imageUrl;
        }
    }

    public interface OnImageDeleteListener {
        void onDelete(ImageItem item, int position);
    }

    private final List<ImageItem> imageItems;

    private final OnImageDeleteListener deleteListener;

    /**
     * Inflates an image row view holder.
     *
     * @param parent - The parent ViewGroup
     * @param viewType - The view type to create
     * @return A new ViewHolder for the image row
     */
    public ImageManagerAdapter(List<ImageItem> imageItems, OnImageDeleteListener deleteListener) {
        this.imageItems = imageItems;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return imageItems.size();
    }

    /**
     * Holds references to views for an image row.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview;
        TextView txtImageLabel;
        View btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            txtImageLabel = itemView.findViewById(R.id.txtImageLabel);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
