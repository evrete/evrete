package org.evrete.showcase.stock.rule;

import org.evrete.showcase.stock.OHLC;

import java.util.HashMap;
import java.util.Map;

public class TimeSlot extends OHLC {
    private final Map<String, Object> data = new HashMap<>();
    public int id;

    public TimeSlot(int id, OHLC ohlc) {
        super(ohlc.open, ohlc.high, ohlc.low, ohlc.close);
        this.id = id;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object o = data.get(key);
        return o == null ? defaultValue : (T) o;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object o = this.data.get(key);
        if (o == null) {
            throw new IllegalStateException("No value associated with key '" + key + "' is found in " + this);
        } else {
            return (T) this.data.get(key);
        }
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", data=" + data +
                '}';
    }
}
