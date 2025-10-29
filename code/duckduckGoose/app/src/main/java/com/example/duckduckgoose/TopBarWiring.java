package com.example.duckduckgoose;

import android.app.Activity;
import android.view.View;

public final class TopBarWiring {
    private TopBarWiring() {}

    public static void attachProfileSheet(Activity activity) {
        View avatar = activity.findViewById(R.id.btnProfile);
        if (avatar != null) {
            avatar.setOnClickListener(v ->
                    ProfileSheet.newInstance().show(
                            ((androidx.fragment.app.FragmentActivity) activity)
                                    .getSupportFragmentManager(),
                            "ProfileSheet"));
        }
    }
}
