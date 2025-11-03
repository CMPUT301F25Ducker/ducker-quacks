package com.example.duckduckgoose;

import java.util.List;

public class Event {
    private String newEventId;
    private String name;
    String eventDateStr;
    private String regOpensStr;
    private String regClosesStr;
    private String spots;
    private String cost;
    private boolean geolocation;
    private List<String>  imagePaths;

    Event(String newEventId,
          String name,
          String eventDateStr,
          String regOpensStr,
          String regClosesStr,
          String spots,
          String cost,
          boolean geolocation,
          List<String>  imagePaths) {
        this.newEventId = newEventId;
        this.name = name;
        this.eventDateStr = eventDateStr;
        this.regOpensStr = regOpensStr;
        this.regClosesStr = regClosesStr;
        this.spots = spots;
        this.cost = cost;
        this.geolocation = geolocation;
        this.imagePaths = imagePaths;
    };
}
