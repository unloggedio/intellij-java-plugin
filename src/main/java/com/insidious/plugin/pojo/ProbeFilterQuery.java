package com.insidious.plugin.pojo;

import com.insidious.common.weaver.EventType;

import java.util.Collection;

public class ProbeFilterQuery {

    private final Collection<EventType> eventType;

    public ProbeFilterQuery(Collection<EventType> eventType) {
        this.eventType = eventType;
    }

    public Collection<EventType> getEventType() {
        return eventType;
    }
}
