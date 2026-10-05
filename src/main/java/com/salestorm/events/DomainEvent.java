package com.salestorm.events;

public interface DomainEvent {
    String getEventId();
    String getEventType();
    Object getPayload();
}
