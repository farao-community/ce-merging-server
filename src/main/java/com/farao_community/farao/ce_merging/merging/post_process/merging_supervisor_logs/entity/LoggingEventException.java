package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "logging_event_exception")
public class LoggingEventException {

    @Id
    @ManyToOne()
    @JoinColumn(name = "event_id")
    private LoggingEvent loggingEvent;

    @Id
    @Column(nullable = false)
    private int i;

    @Column(name = "trace_line", nullable = false, columnDefinition = "VARCHAR(254)")
    private String traceLine;

    public LoggingEvent getLoggingEvent() {
        return loggingEvent;
    }

    public void setLoggingEvent(final LoggingEvent loggingEvent) {
        this.loggingEvent = loggingEvent;
    }

    public int getI() {
        return i;
    }

    public void setI(final int i) {
        this.i = i;
    }

    public String getTraceLine() {
        return traceLine;
    }

    public void setTraceLine(final String traceLine) {
        this.traceLine = traceLine;
    }
}
