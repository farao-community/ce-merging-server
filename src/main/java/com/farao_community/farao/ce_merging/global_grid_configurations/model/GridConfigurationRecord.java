package com.farao_community.farao.ce_merging.global_grid_configurations.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public abstract class GridConfigurationRecord {
    @Id
    protected String id;
    protected LocalDateTime validFrom;
    protected LocalDateTime validTo;
    protected LocalDateTime publishedOn;

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
