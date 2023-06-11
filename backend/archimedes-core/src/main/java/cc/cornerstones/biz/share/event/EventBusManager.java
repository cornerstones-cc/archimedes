package cc.cornerstones.biz.share.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class EventBusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusManager.class);

    private EventBus syncEventBus = new EventBus("sync-event-bus");

    private EventBus asyncEventBus = new AsyncEventBus("async-event-bus", Executors.newFixedThreadPool(3));

    public void registerSubscriber(Object subscriber) {
        this.syncEventBus.register(subscriber);
        this.asyncEventBus.register(subscriber);
    }

    public void send(Object event) {
        this.syncEventBus.post(event);
    }

    public void post(Object event) {
        this.asyncEventBus.post(event);
    }
}
