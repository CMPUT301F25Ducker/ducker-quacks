package com.example.duckduckgoose;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EventManagerAdapter extends RecyclerView.Adapter<EventManagerAdapter.ViewHolder> {

    private final List<Event> events;
    private OnItemClickListener onItemClickListener;

    public EventManagerAdapter(List<Event> events) {
        this.events = events;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_manager, parent, false);
        return new ViewHolder(view);
    }

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

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(events.get(currentPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        TextView txtDetails;

        ViewHolder(View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtEventTitle);
            txtDetails = itemView.findViewById(R.id.txtEventDetails);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Event event);
    }
}
