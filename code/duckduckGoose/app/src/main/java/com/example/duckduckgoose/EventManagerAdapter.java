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

    private final List<MainActivity.Event> events;

    public EventManagerAdapter(List<MainActivity.Event> events) {
        this.events = events;
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
        MainActivity.Event event = events.get(position);
        holder.txtTitle.setText(event.title);
        String details = "Date: " + event.date + "\n" +
                "Registration Opens: " + event.open + "\n" +
                "Registration Deadline: " + event.deadline + "\n" +
                "Cost: " + event.cost + "\n" +
                "Spots: " + event.spots;
        holder.txtDetails.setText(details);

        holder.btnDelete.setOnClickListener(v -> {
            // TODO: Implement delete functionality
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        TextView txtDetails;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtEventTitle);
            txtDetails = itemView.findViewById(R.id.txtEventDetails);
            btnDelete = itemView.findViewById(R.id.btnDeleteEvent);
        }
    }
}
