package com.salestorm.events;

public interface EventPublisher {
    void publish(DomainEvent event);
}
