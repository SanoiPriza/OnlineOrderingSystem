package org.example.orderService.event;

import org.springframework.context.ApplicationEvent;

public class OutboxSavedEvent extends ApplicationEvent {

    public OutboxSavedEvent(Object source) {
        super(source);
    }
}
