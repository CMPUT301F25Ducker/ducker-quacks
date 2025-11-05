package com.example.duckduckgoose;

import java.util.List;

public class Event {
    private String eventId;
    private String name;
    private String eventDate;
    private String registrationOpens;
    private String registrationCloses;
    private String maxSpots;
    private String cost;
    private boolean geolocationEnabled;
    private List<String> imagePaths;

    public Event() {}

    public Event(String eventId, String name, String eventDate, String registrationOpens,
                 String registrationCloses, String maxSpots, String cost,
                 boolean geolocationEnabled, List<String> imagePaths) {
        this.eventId = eventId;
        this.name = name;
        this.eventDate = eventDate;
        this.registrationOpens = registrationOpens;
        this.registrationCloses = registrationCloses;
        this.maxSpots = maxSpots;
        this.cost = cost;
        this.geolocationEnabled = geolocationEnabled;
        this.imagePaths = imagePaths;
    }

    public String getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getRegistrationOpens() {
        return registrationOpens;
    }

    public String getRegistrationCloses() {
        return registrationCloses;
    }

    public String getMaxSpots() {
        return maxSpots;
    }

    public String getCost() {
        return cost;
    }

    public boolean isGeolocationEnabled() {
        return geolocationEnabled;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }
}