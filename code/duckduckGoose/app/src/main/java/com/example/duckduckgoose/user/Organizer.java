package com.example.duckduckgoose.user;

import com.example.duckduckgoose.Event;

import java.util.ArrayList;
import java.util.List;

public class Organizer extends User {
    private List<Event> events;

    public Organizer() {
        super();
        events = new ArrayList<>();
    }

    public List<Event> getEvents() {
        return events;
    }

    public int getEventCount() {
        return events.size();
    }
}
