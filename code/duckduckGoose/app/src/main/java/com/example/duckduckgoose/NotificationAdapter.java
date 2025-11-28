/**
 * RecyclerView adapter for displaying notification items.
 *
 * Binds notification data (title and organizer) to card-based list items.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for binding NotificationItem objects to RecyclerView rows.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    /** List of notifications to display. */
    private final List<NotificationLogsActivity.NotificationItem> notifications;

    /**
     * Constructs the adapter with a notification list.
     *
     * @param notifications - List of NotificationItem objects
     */
    public NotificationAdapter(List<NotificationLogsActivity.NotificationItem> notifications) {
        this.notifications = notifications;
    }

    /**
     * Inflates the notification item layout.
     *
     * @param parent - Parent ViewGroup
     * @param viewType - View type (unused)
     * @return New ViewHolder for a notification row
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds notification data to a row's views.
     *
     * @param holder - ViewHolder for the row
     * @param position - Position in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationLogsActivity.NotificationItem notification = notifications.get(position);
        holder.txtNotifTitle.setText(notification.getTitle());
        holder.txtOrganizer.setText(notification.getOrganizer());
    }

    /**
     * Returns the total number of notifications.
     *
     * @return Number of items in the list
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * ViewHolder for notification item views.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNotifTitle;
        TextView txtOrganizer;

        /**
         * Initializes view references.
         *
         * @param itemView - Root view of the notification item
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNotifTitle = itemView.findViewById(R.id.txtNotifTitle);
            txtOrganizer = itemView.findViewById(R.id.txtOrganizer);
        }
    }
}
