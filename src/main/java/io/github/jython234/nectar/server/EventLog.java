package io.github.jython234.nectar.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Represents the server's internal event log that records
 * all actions performed by management sessions and clients.
 *
 * @author jython234
 */
public class EventLog {
    @Getter private final int maxEntryCount;

    @Getter private final Deque<Entry> entries = new ArrayDeque<>();
    @Getter private final Logger eventLogLogger;

    public EventLog(int maxEntryCount) {
        this.maxEntryCount = maxEntryCount;
        this.eventLogLogger = LoggerFactory.getLogger("Nectar-EventLog");
    }

    public void addEntry(Entry entry) {
        synchronized (entries) {
            if(entries.size() >= maxEntryCount) { // If we reach the max size, remove the furthest entry
                entries.removeFirst();
            }

            this.entries.addLast(entry); // Add the entry to the end of the Deque
        }
    }

    public void addEntry(EntryLevel level, String message) {
        addEntry(new Entry(LocalDateTime.now(), level, message));
    }

    public void logEntry(EntryLevel level, String message) {
        switch (level) {
            case DEBUG:
                this.eventLogLogger.debug("!|! " + message);
                break;
            case INFO:
                this.eventLogLogger.info("!|! " + message);
                break;
            case NOTICE:
                this.eventLogLogger.info("!|! (NOTICE): " + message);
                break;
            case WARNING:
                this.eventLogLogger.warn("!|! " + message);
                break;
            case ERROR:
                this.eventLogLogger.error("!|! " + message);
                break;
        }

        this.addEntry(new Entry(LocalDateTime.now(), level, message));
    }

    /**
     * Represents an Entry inside the event log.
     * An entry contains a time, date, level, and message.
     *
     * @author jython234
     */
    @RequiredArgsConstructor
    public static class Entry {
        @Getter private final LocalDateTime datetime;
        @Getter private final EntryLevel level;
        @Getter private final String message;
    }

    /**
     * Represents the level of an Entry
     *
     * @author jython234
     */
    public enum EntryLevel {
        DEBUG("DEBUG"),
        INFO("INFO"),
        NOTICE("NOTICE"),
        WARNING("WARNING"),
        ERROR("ERROR");

        private String level;

        EntryLevel(String level) {
            this.level = level;
        }

        @Override
        public String toString() {
            return this.level;
        }
    }
}
