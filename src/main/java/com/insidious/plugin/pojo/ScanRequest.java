package com.insidious.plugin.pojo;

import com.insidious.common.weaver.EventType;
import com.insidious.plugin.extension.model.DirectionType;

import java.util.*;

public class ScanRequest {

    private final Integer callStack;
    private final Integer startIndex;
    private final DirectionType direction;
    private final Set<EventType> matchUntilEvent = new HashSet<>();
    // whatever we are searching for, if we match any event which has listeners, the listner gets
    // a callback with the index of the matched probe
    private final Map<EventType, List<EventTypeMatchListener>> eventListeners = new HashMap<>();

    public void matchUntil(EventType eventType) {
        matchUntilEvent.add(eventType);
    }

    public ScanRequest(
            // where the search begins
            Integer startIndex,
            // this defines which call stack level should the value be matched in
            // call stack is defined by method_entry and method_exit event types
            Integer callStack,
            // we can either go forward from a particular index or backward, until the search
            // hits 0 or .size
            DirectionType direction
    ) {
        this.callStack = callStack;
        this.startIndex = startIndex;
        this.direction = direction;
    }
    public void addListener(
            EventType eventType,
            EventTypeMatchListener eventTypeMatchListener
    ) {
        List<EventTypeMatchListener> listenerList = this.eventListeners.get(eventType);
        if (listenerList == null) {
            synchronized (this) {
                listenerList = this.eventListeners.computeIfAbsent(eventType, k -> new LinkedList<>());
            }
        }
        listenerList.add(eventTypeMatchListener);
    }

    public void removeListener(
            EventType eventType,
            EventTypeMatchListener eventTypeMatchListener
    ) {
        List<EventTypeMatchListener> listenerList = this.eventListeners.get(eventType);
        if (listenerList == null) {
            synchronized (this) {
                listenerList = this.eventListeners.computeIfAbsent(eventType, k -> new LinkedList<>());
            }
        }
        listenerList.add(eventTypeMatchListener);
    }


    public Integer getCallStack() {
        return callStack;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public DirectionType getDirection() {
        return direction;
    }

    public Set<EventType> getMatchUntilEvent() {
        return matchUntilEvent;
    }

    public void onEvent(int callStack, EventType eventType, int callReturnIndex) {
        if (callStack == this.callStack && eventListeners.containsKey(eventType)) {
            eventListeners.get(eventType).forEach(e -> e.eventMatched(callReturnIndex));
        }
    }
}
