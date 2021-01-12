package org.evrete.showcase.abs.town.types;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WorldTime {
    private final Calendar calendar;
    private final int initialTimeSeconds;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("d MMM, K:mm:ss a");

    public WorldTime() {
        this.calendar = Calendar.getInstance();
        // reset hour, minutes, seconds and millis
        calendar.set(Calendar.HOUR_OF_DAY, 5);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // next day
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        this.initialTimeSeconds = absoluteTimeSeconds();
    }

    public static int timeInMinutes(int hour, int minute) {
        return 60 * hour + minute;
    }

    public WorldTime increment(int deltaSeconds) {
        this.calendar.add(Calendar.SECOND, deltaSeconds);
        return this;
    }

    public int minutesSinceMidnight() {
        return 60 * calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE);
    }

    public int seconds() {
        return 3600 * calendar.get(Calendar.HOUR_OF_DAY) + 60 * calendar.get(Calendar.MINUTE) + calendar.get(Calendar.SECOND);
    }

    @SuppressWarnings("unused")
    public int hour() {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    @SuppressWarnings("unused")
    public int minute() {
        return calendar.get(Calendar.MINUTE);
    }

    public int absoluteTimeSeconds() {
        return (int) (calendar.getTimeInMillis() / 1000);
    }

    public int getInitialTimeSeconds() {
        return initialTimeSeconds;
    }

    @Override
    public String toString() {
        return dateTimeFormat.format(calendar.getTime());
    }
}
