/**
 * Helper for wiring top bar actions (e.g., profile sheet).
 *
 * Provides a static method to attach the profile bottom sheet to the
 * top-bar profile button in any activity.
 *
 * @author DuckDuckGoose Development Team
 */

package com.example.duckduckgoose;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

/**
 * Utility class for attaching top-bar interactions.
 *
 * Contains static helpers only; not intended to be instantiated.
 */
public final class TopBarWiring {

    /** Prevents instantiation. */
    private TopBarWiring() {}

    /**
     * Attaches a click listener to open the profile sheet from the top bar.
     *
     * Looks up {@code R.id.btnProfile} and, if present, shows {@link ProfileSheet}
     * using the activity's {@link androidx.fragment.app.FragmentManager}.
     *
     * @param activity Host activity containing a view with id {@code R.id.btnProfile}.
     */
    public static void attachProfileSheet(Activity activity) {
        View avatar = activity.findViewById(R.id.btnProfile);
        if (avatar != null) {
            avatar.setOnClickListener(v ->
                    ProfileSheet.newInstance().show(
                            ((androidx.fragment.app.FragmentActivity) activity)
                                    .getSupportFragmentManager(),
                            "ProfileSheet"));
        }

        View notifBtn = activity.findViewById(R.id.btnNotifications);
        if (notifBtn != null) {
            notifBtn.setOnClickListener(v -> {
                if (activity instanceof NotificationActivity) {
                    return;
                }
                Intent intent = new Intent(activity, NotificationActivity.class);
                // no multiple copies
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            });
        }
    }
}
