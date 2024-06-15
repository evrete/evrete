package org.evrete.runtime.events;

public class SimpleSubscriber<T> implements Subscriber<T> {
    private final String name;

    public SimpleSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void onNext(T item) {
        System.out.println(name + " received: " + item);
    }
}
