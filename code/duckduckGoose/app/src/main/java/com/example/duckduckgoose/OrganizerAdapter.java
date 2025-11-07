package com.example.duckduckgoose;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class OrganizerAdapter extends RecyclerView.Adapter<OrganizerAdapter.ViewHolder> {

    private List<User> organizers;

    public OrganizerAdapter(List<User> organizers) {
        this.organizers = organizers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_organizer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User organizer = organizers.get(position);
        holder.organizerName.setText(organizer.getFullName());
        holder.organizerEmail.setText(organizer.getEmail());

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OrganizerProfileActivity.class);
            intent.putExtra("organizerName", organizer.getFullName());
            intent.putExtra("organizerEmail", organizer.getEmail());
            context.startActivity(intent);
        });

        holder.deleteButton.setOnClickListener(v -> {
            organizers.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        });
    }

    @Override
    public int getItemCount() {
        return organizers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView organizerName;
        TextView organizerEmail;
        MaterialButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            organizerName = itemView.findViewById(R.id.organizer_name);
            organizerEmail = itemView.findViewById(R.id.organizer_email);
            deleteButton = itemView.findViewById(R.id.delete_organizer_button);
        }
    }
}
