package org.evrete.showcase.abs.town.types;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WorldTime {
    private final Calendar calendar;
    private final int initialTimeSeconds;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("K:mm a");

    public WorldTime() {
        this.calendar = Calendar.getInstance();
        // reset hour, minutes, seconds and millis
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // next day
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        this.initialTimeSeconds = absoluteTimeSeconds();
    }

    public WorldTime increment(int deltaSeconds) {
        this.calendar.add(Calendar.SECOND, deltaSeconds);
        return this;
    }

    @SuppressWarnings("unused")
    public int hour() {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    @SuppressWarnings("unused")
    public int minute() {
        return calendar.get(Calendar.MINUTE);
    }

    private int absoluteTimeSeconds() {
        return (int) (calendar.getTimeInMillis() / 1000);
    }

    public int secondsSinceStart() {
        return absoluteTimeSeconds() - initialTimeSeconds;
    }

    @Override
    public String toString() {
        String time = dateTimeFormat.format(calendar.getTime());
        if (time.length() < 8) {
            time = "0" + time;
        }
        int day = secondsSinceStart() / (24 * 3600);
        return String.format("Day %02d %s", day, time);
    }


}
