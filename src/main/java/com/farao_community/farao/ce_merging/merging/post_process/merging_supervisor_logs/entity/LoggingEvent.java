package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "logging_event")
public class LoggingEvent {

    @Id
    @GeneratedValue
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false, columnDefinition = "BIGINT")
    private long timestmp;

    @Column(name = "formatted_message", nullable = false, columnDefinition = "LONGVARCHAR")
    private String formattedMessage;

    @Column(name = "logger_name", nullable = false, columnDefinition = "VARCHAR(256)")
    private String loggerName;

    @Column(name = "level_string", nullable = false, columnDefinition = "VARCHAR(256)")
    private String levelString;

    @Column(name = "thread_name", columnDefinition = "VARCHAR(256)")
    private String threadName;

    @Column(name = "reference_flag", columnDefinition = "SMALLINT")
    private int referenceFlag;

    @Column(columnDefinition = "VARCHAR(256)")
    private String arg0;

    @Column(columnDefinition = "VARCHAR(256)")
    private String arg1;

    @Column(columnDefinition = "VARCHAR(256)")
    private String arg2;

    @Column(columnDefinition = "VARCHAR(256)")
    private String arg3;

    @Column(name = "caller_filename", columnDefinition = "VARCHAR(256)")
    private String callerFilename;

    @Column(name = "caller_class", columnDefinition = "VARCHAR(256)")
    private String callerClass;

    @Column(name = "caller_method", columnDefinition = "VARCHAR(256)")
    private String callerMethod;

    @Column(name = "caller_line", columnDefinition = "CHAR(4)")
    private String callerLine;

    @OneToMany(
            mappedBy = "loggingEvent",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<LoggingEventProperty> properties;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(final Long eventId) {
        this.eventId = eventId;
    }

    public long getTimestmp() {
        return timestmp;
    }

    public void setTimestmp(final long timestmp) {
        this.timestmp = timestmp;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public void setFormattedMessage(final String formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLevelString() {
        return levelString;
    }

    public void setLevelString(final String levelString) {
        this.levelString = levelString;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(final String threadName) {
        this.threadName = threadName;
    }

    public int getReferenceFlag() {
        return referenceFlag;
    }

    public void setReferenceFlag(final int referenceFlag) {
        this.referenceFlag = referenceFlag;
    }

    public String getArg0() {
        return arg0;
    }

    public void setArg0(final String arg0) {
        this.arg0 = arg0;
    }

    public String getArg1() {
        return arg1;
    }

    public void setArg1(final String arg1) {
        this.arg1 = arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setArg2(final String arg2) {
        this.arg2 = arg2;
    }

    public String getArg3() {
        return arg3;
    }

    public void setArg3(final String arg3) {
        this.arg3 = arg3;
    }

    public String getCallerFilename() {
        return callerFilename;
    }

    public void setCallerFilename(final String callerFilename) {
        this.callerFilename = callerFilename;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public void setCallerClass(final String callerClass) {
        this.callerClass = callerClass;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public void setCallerMethod(final String callerMethod) {
        this.callerMethod = callerMethod;
    }

    public String getCallerLine() {
        return callerLine;
    }

    public void setCallerLine(final String callerLine) {
        this.callerLine = callerLine;
    }

    public Set<LoggingEventProperty> getProperties() {
        return properties;
    }

    public void setProperties(final Set<LoggingEventProperty> properties) {
        this.properties = properties;
    }
}
