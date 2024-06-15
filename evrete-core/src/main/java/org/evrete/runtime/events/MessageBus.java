package org.evrete.runtime.events;

import java.util.EnumMap;
import java.util.Map;

public class MessageBus {
    private final Map<Topic, IntermediaryPublisher<String>> topics = new EnumMap<>(Topic.class);

    public void registerPublisher(Topic topic, CustomSubmissionPublisher<String> publisher) {
        IntermediaryPublisher<String> intermediaryPublisher = topics.computeIfAbsent(topic, k -> new IntermediaryPublisher<>(publisher));
        intermediaryPublisher.addPublisher(publisher);
    }

    public void broadcast(Topic topic, String message) {
        IntermediaryPublisher<String> intermediaryPublisher = topics.get(topic);
        if (intermediaryPublisher != null) {
            intermediaryPublisher.publish(message);
        }
    }

    public void subscribe(Topic topic, Subscriber<? super String> subscriber) {
        IntermediaryPublisher<String> intermediaryPublisher = topics.get(topic);
        if (intermediaryPublisher == null) {
            throw new IllegalArgumentException("No event publisher found for events of type " + topic);
        } else {
            intermediaryPublisher.subscribe(subscriber);
        }
    }

    public void close() {
        topics.values().forEach(CustomSubmissionPublisher::close);
    }

}
