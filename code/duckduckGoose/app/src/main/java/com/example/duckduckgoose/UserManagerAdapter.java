/**
 * RecyclerView adapter for displaying and interacting with {@link User} items.
 *
 * <p>Binds a list of users into row views for the user manager screen. Supports
 * optional checkboxes (e.g., for attendee selection) and click callbacks.</p>
 *
 * <p><b>Author:</b> DuckDuckGoose Development Team</p>
 */
package com.example.duckduckgoose;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duckduckgoose.user.User;

import java.util.List;

/**
 * Adapter for rendering a list of {@link User} items in a RecyclerView.
 *
 * <p>Provides a click listener callback and an option to show/hide checkboxes
 * in each row (e.g., for attendee selection flows).</p>
 */
public class UserManagerAdapter extends RecyclerView.Adapter<UserManagerAdapter.ViewHolder> {

    /** Immutable backing list of users to render. */
    private final List<? extends User> users;

    /** Optional item click listener for row taps. */
    private OnItemClickListener onItemClickListener;

    /** Whether to display attendee checkboxes in each row. */
    private final boolean showCheckboxes;

    /**
     * Constructs an adapter with checkboxes visible by default.
     *
     * @param users non-null list of users to display
     * @throws IllegalArgumentException if {@code users} is null
     */
    public UserManagerAdapter(List<? extends User> users) {
        if (users == null) {
            throw new IllegalArgumentException("users cannot be null");
        }
        this.users = users;
        this.showCheckboxes = true;
    }

    /**
     * Constructs an adapter with explicit checkbox visibility.
     *
     * @param users non-null list of users to display
     * @param showCheckboxes {@code true} to show the attendee checkbox for each item
     * @throws IllegalArgumentException if {@code users} is null
     */
    public UserManagerAdapter(List<? extends User> users, boolean showCheckboxes) {
        if (users == null) {
            throw new IllegalArgumentException("users cannot be null");
        }
        this.users = users;
        this.showCheckboxes = showCheckboxes;
    }

    /**
     * Registers a row click callback.
     *
     * @param listener listener invoked with the clicked {@link User}
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * Inflates the item view and wraps it in a {@link ViewHolder}.
     *
     * @param parent RecyclerView parent
     * @param viewType unused view type for this adapter
     * @return newly created {@link ViewHolder}
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_manager, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the {@link User} at the given position to the provided holder.
     *
     * <p>Safely handles missing/null user fields and toggles optional views.</p>
     *
     * @param holder target {@link ViewHolder} for binding
     * @param position adapter position of the item to bind
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // verify index is in range (adapter invariants should guarantee this).
        if (position < 0 || position >= users.size()) {
            return;
        }

        User user = users.get(position);
        if (user == null) {
            return;
        }

        if (holder.checkboxAttendee != null) {
            holder.checkboxAttendee.setVisibility(showCheckboxes ? View.VISIBLE : View.GONE);
        }

        holder.txtFullName.setText(user.getFullName() != null ? user.getFullName() : "(no name)");
        holder.txtUserId.setText(user.getUserId() != null ? user.getUserId() : "(no id)");

        if (holder.txtAccountType != null) {
            if (user.getAccountType() != null && !user.getAccountType().isEmpty()) {
                holder.txtAccountType.setVisibility(View.VISIBLE);
                holder.txtAccountType.setText(user.getAccountType());
            } else {
                holder.txtAccountType.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION && currentPosition < users.size()) {
                    onItemClickListener.onItemClick(users.get(currentPosition));
                }
            }
        });
    }

    /**
     * Returns the number of items in the adapter.
     *
     * @return size of the backing user list (0 if list is empty)
     */
    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    /**
     * Caches row view references for faster binding.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /** Full name text field. */
        TextView txtFullName;
        /** User ID text field. */
        TextView txtUserId;
        /** Account type text field. */
        TextView txtAccountType;
        /** Optional row delete button (may be unused in this adapter). */
        ImageButton btnDelete;
        /** Optional attendee checkbox. */
        android.widget.CheckBox checkboxAttendee;

        /**
         * Binds layout child views to fields.
         *
         * @param itemView the inflated row view
         */
        ViewHolder(View itemView) {
            super(itemView);
            txtFullName = itemView.findViewById(R.id.txtUserName);
            txtUserId = itemView.findViewById(R.id.txtUserId);
            txtAccountType = itemView.findViewById(R.id.txtAccountType);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            checkboxAttendee = itemView.findViewById(R.id.checkboxAttendee);
        }
    }

    /**
     * Callback for row tap events.
     */
    public interface OnItemClickListener {
        /**
         * Invoked when a user row is tapped.
         *
         * @param user the {@link User} represented by the tapped row
         */
        void onItemClick(User user);
    }
}
