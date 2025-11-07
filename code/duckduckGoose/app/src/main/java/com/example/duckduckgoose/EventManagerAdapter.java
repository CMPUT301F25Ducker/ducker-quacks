/**
 * @file EventManagerAdapter.java
 * @brief RecyclerView adapter for displaying and managing event details.
 *
 * Used in the admin interface to list, preview, and interact with event entries.
 * Supports click handling via a listener for viewing or editing event details.
 *
 * @author
 *      DuckDuckGoose Development Team
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
 * @class EventManagerAdapter
 * @brief Adapter class that binds Event objects into a scrollable list for management.
 */
public class EventManagerAdapter extends RecyclerView.Adapter<EventManagerAdapter.ViewHolder> {

    /** List of events to display. */
    private final List<Event> events;

    /** Optional callback for handling item clicks. */
    private OnItemClickListener onItemClickListener;

    /**
     * @brief Constructs the adapter with a provided list of events.
     * @param events List of Event objects to display.
     */
    public EventManagerAdapter(List<Event> events) {
        this.events = events;
    }

    /**
     * @brief Sets a listener for event item click actions.
     * @param listener Implementation of OnItemClickListener to handle clicks.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * @brief Inflates the view holder for a single event row.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_manager, parent, false);
        return new ViewHolder(view);
    }

    /**
     * @brief Binds event data (title and details) to a row view.
     * @param holder The row’s view holder.
     * @param position The adapter position.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.txtTitle.setText(event.getName());
        String details = event.getEventDate() + "\n" +
                "Registration Opens: " + event.getRegistrationOpens() + "\n" +
                "Registration Deadline: " + event.getRegistrationCloses() + "\n" +
                "Cost: " + event.getCost() + "\n" +
                "Spots: " + event.getMaxSpots();
        holder.txtDetails.setText(details);

        /** @brief Handles item click and forwards the selected event. */
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(events.get(currentPosition));
                }
            }
        });
    }

    /**
     * @brief Returns total number of event items.
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * @class ViewHolder
     * @brief Holds references to the TextViews for each event item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        TextView txtDetails;

        /**
         * @brief Binds UI references for an event row.
         * @param itemView The inflated event item layout.
         */
        ViewHolder(View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtEventTitle);
            txtDetails = itemView.findViewById(R.id.txtEventDetails);
        }
    }

    /**
     * @interface OnItemClickListener
     * @brief Callback interface for event item click handling.
     */
    public interface OnItemClickListener {
        /**
         * @brief Called when an event item is clicked.
         * @param event The Event object corresponding to the clicked row.
         */
        void onItemClick(Event event);
    }
}
