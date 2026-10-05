package com.salestorm.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryEventPublisher implements EventPublisher {
    
    private final ConcurrentHashMap<String, List<Consumer<DomainEvent>>> listeners = new ConcurrentHashMap<>();

    @Override
    public void publish(DomainEvent event) {
        List<Consumer<DomainEvent>> eventListeners = listeners.getOrDefault(event.getEventType(), new ArrayList<>());
        for (Consumer<DomainEvent> listener : eventListeners) {
            listener.accept(event);
        }
    }

    public void subscribe(String eventType, Consumer<DomainEvent> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
}
