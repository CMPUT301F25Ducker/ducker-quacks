package com.example.duckduckgoose;

/**
 * Application-wide configuration settings.
 * Change LOGIN_MODE here to switch between user types across the entire app.
 */
public class AppConfig {
    
    /**
     * CHANGE THIS TO SWITCH BETWEEN "ADMIN", "ENTRANT", AND "ORGANIZER" MODE
     * This controls the behavior across all activities in the app.
     */
    public static final String LOGIN_MODE = "ORGANIZER";

    public static void setLoginMode(String mode) {
        LOGIN_MODE = mode.toUpperCase();
    }
}
