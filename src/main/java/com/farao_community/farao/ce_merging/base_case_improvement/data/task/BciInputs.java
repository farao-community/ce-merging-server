/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.task;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class BciInputs implements Serializable {

    private String forecastNetPositionsPath;
    private String externalConstraintsPath;
    private String feasibilityRangePath;
    private String initialNetPositionsPath;
    private String alegroNetPositionsPath;

    public BciInputs() {
    }

    public BciInputs(final String forecastNetPositionsPath,
                     final String externalConstraintsPath,
                     final String feasibilityRangePath,
                     final String initialNetPositionsPath) {
        this.forecastNetPositionsPath = forecastNetPositionsPath;
        this.externalConstraintsPath = externalConstraintsPath;
        this.feasibilityRangePath = feasibilityRangePath;
        this.initialNetPositionsPath = initialNetPositionsPath;

    }

    public BciInputs(final String forecastNetPositionFilePath,
                     final String externalConstraintsPath) {
        this(forecastNetPositionFilePath,
             externalConstraintsPath,
             null,
             null);
    }

    public String getForecastNetPositionsPath() {
        return forecastNetPositionsPath;
    }

    public void setForecastNetPositionsPath(final String forecastNetPositionsPath) {
        this.forecastNetPositionsPath = forecastNetPositionsPath;
    }

    public String getExternalConstraintsPath() {
        return externalConstraintsPath;
    }

    public String getFeasibilityRangePath() {
        return feasibilityRangePath;
    }

    public void setExternalConstraintsPath(final String externalConstraintsPath) {
        this.externalConstraintsPath = externalConstraintsPath;
    }

    public void setFeasibilityRangePath(final String feasibilityRangePath) {
        this.feasibilityRangePath = feasibilityRangePath;
    }

    public String getInitialNetPositionsPath() {
        return initialNetPositionsPath;
    }

    public void setInitialNetPositionsPath(final String initialNetPositionsPath) {
        this.initialNetPositionsPath = initialNetPositionsPath;
    }

    public String getAlegroNetPositionsPath() {
        return alegroNetPositionsPath;
    }

    public void setAlegroNetPositionsPath(final String alegroNetPositionsPath) {
        this.alegroNetPositionsPath = alegroNetPositionsPath;
    }
}
