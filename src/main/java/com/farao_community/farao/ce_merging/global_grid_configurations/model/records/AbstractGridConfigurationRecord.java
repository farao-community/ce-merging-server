package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class AbstractGridConfigurationRecord {
    @Id
    protected String id;
    protected LocalDateTime validFrom;
    protected LocalDateTime validTo;
    protected LocalDateTime publishedOn;

    protected AbstractGridConfigurationRecord(final String id,
                                              final LocalDateTime validFrom,
                                              final LocalDateTime validTo,
                                              final LocalDateTime publishedOn) {
        this.id = id;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.publishedOn = publishedOn;
    }

    protected AbstractGridConfigurationRecord() {}

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(final LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getPublishedOn() {
        return publishedOn;
    }

    public void setPublishedOn(final LocalDateTime publishedOn) {
        this.publishedOn = publishedOn;
    }
}
