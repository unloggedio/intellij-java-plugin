package com.insidious.plugin.pojo;

import com.insidious.common.weaver.EventType;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousDebuggerTreeNodeImpl;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ScanResult;

import java.util.*;

public class ScanRequest {

    public static final int ANY_STACK = 9999;

    // match true when the current probe class matches the first probe class, this is needed so
    // we can track all calls from a particular class
    public static final int CURRENT_CLASS = 8888;

    // match true when the current probe caller owner  value matches the first probes caller
    // owner value, so we can track all calls on a particular instance (or object)
    // impossible to track for now, since we are not tracking actual object ownership thru the
    // event sequence (call stack count itself is much easier)
    // todo: maintain stack of actual ids of object ownership to track calls to current owner
//    public static final int CURRENT_OWNER = 7777;
    private final Integer callStack;
    private final ScanResult startIndex;
    private final DirectionType direction;
    private final Set<EventType> matchUntilEvent = new HashSet<>();
    // whatever we are searching for, if we match any event which has listeners, the listner gets
    // a callback with the index of the matched probe
    private final Map<EventType, List<EventMatchListener>> eventListeners = new HashMap<>();
    private final Map<Long, List<EventMatchListener>> valueEventListeners = new HashMap<>();
    private int startStack = 0;

    public void matchUntil(EventType eventType) {
        matchUntilEvent.add(eventType);
    }

    public ScanRequest(
            // where the search begins
            ScanResult startIndex,
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
            EventMatchListener eventTypeMatchListener
    ) {
        List<EventMatchListener> listenerList = this.eventListeners.get(eventType);
        if (listenerList == null) {
            synchronized (this) {
                listenerList = this.eventListeners.computeIfAbsent(eventType, k -> new LinkedList<>());
            }
        }
        listenerList.add(eventTypeMatchListener);
    }

    public void removeListener(
            EventType eventType,
            EventMatchListener eventTypeMatchListener
    ) {
        List<EventMatchListener> listenerList = this.eventListeners.get(eventType);
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

    public ScanResult getStartIndex() {
        return startIndex;
    }

    public DirectionType getDirection() {
        return direction;
    }

    public Set<EventType> getMatchUntilEvent() {
        return matchUntilEvent;
    }

    public void onEvent(boolean stackMatch, EventType eventType, int callReturnIndex) {
        if (stackMatch && eventListeners.containsKey(eventType)) {
            eventListeners.get(eventType).forEach(e -> e.eventMatched(callReturnIndex));
        }
    }
    public void onValue(boolean stackMatch, Long valueId, int callReturnIndex) {
        if (stackMatch && valueEventListeners.containsKey(valueId)) {
            valueEventListeners.get(valueId).forEach(e -> e.eventMatched(callReturnIndex));
        }
    }

    public int getStartStack() {
        return startStack;
    }

    public void setStartStack(int startStack) {
        this.startStack = startStack;
    }

    public void addListener(long value, EventMatchListener eventTypeMatchListener) {
        List<EventMatchListener> existingList = valueEventListeners
                .computeIfAbsent(value, k -> new LinkedList<>());
        existingList.add(eventTypeMatchListener);
    }
}
