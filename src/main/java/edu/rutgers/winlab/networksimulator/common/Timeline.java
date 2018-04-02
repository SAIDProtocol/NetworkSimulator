package edu.rutgers.winlab.networksimulator.common;

import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * Basic time line implementation. Using Priority queue as inner data structure.
 * Keeps the adding order.
 *
 * @author Jiachen Chen
 */
public class Timeline {

    public static long MS_IN_SECOND = 1000;
    public static long US_IN_MS = 1000;

    private static final Timeline TIMELINE = new Timeline();

    public static Timeline getDefault() {
        return TIMELINE;
    }

    public static void addEvent(long timeInUs, Consumer<Object[]> e, Object... params) {
        getDefault().innerAddEvent(timeInUs, e, params);
    }

    public static long run() {
        return getDefault().innerRun();
    }

    public static long nowInUs() {
        return getDefault().innerNowInUs();
    }

    public static int getSize() {
        return getDefault().innerGetSize();
    }

    private Timeline() {
    }

    private final PriorityQueue<TimelineEvent> events = new PriorityQueue<>();
    private long now = Long.MIN_VALUE;
    private long serial = 0;

    private void innerAddEvent(long timeInUs, Consumer<Object[]> e, Object... params) {

        if (timeInUs < now) {
            throw new IllegalArgumentException(String.format("Cannot add an event in the past, now=%d, add=%d", now, timeInUs));
        }
        events.add(new TimelineEvent(serial++, timeInUs, e, params));
    }

    private long innerRun() {
        TimelineEvent e;
        while ((e = events.poll()) != null) {
            now = e.timeInUs;
            e.consumer.accept((Object[]) e.params);
        }
        long time = now;
        now = Long.MIN_VALUE;
        return time;
    }

    private long innerNowInUs() {
        return now;
    }

    private int innerGetSize() {
        return events.size();
    }

    private class TimelineEvent implements Comparable<TimelineEvent> {

        public final long serial;
        public final long timeInUs;
        public final Consumer<Object[]> consumer;
        public final Object[] params;

        public TimelineEvent(long serial, long timeInUs, Consumer<Object[]> consumer, Object... params) {
            this.serial = serial;
            this.timeInUs = timeInUs;
            this.consumer = consumer;
            this.params = params;
        }

        @Override
        public int compareTo(TimelineEvent o) {
            int ret = Long.compare(timeInUs, o.timeInUs);
            return ret == 0 ? (Long.compare(serial, o.serial)) : ret;
        }

    }
}
