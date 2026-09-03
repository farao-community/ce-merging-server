/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;

import java.util.Map;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class RefProgResult {
    private String dailyTimeInterval;
    private Map<Border, Double> acExchanges;
    private Map<Border, Double> virtualHubsExchanges;

    public RefProgResult(String dailyTimeInterval, Map<Border, Double> acExchanges, Map<Border, Double> virtualHubsExchanges) {
        this.dailyTimeInterval = dailyTimeInterval;
        this.acExchanges = acExchanges;
        this.virtualHubsExchanges = virtualHubsExchanges;
    }

    public String getDailyTimeInterval() {
        return dailyTimeInterval;
    }

    public void setDailyTimeInterval(String dailyTimeInterval) {
        this.dailyTimeInterval = dailyTimeInterval;
    }

    public Map<Border, Double> getAcExchanges() {
        return acExchanges;
    }

    public void setAcExchanges(Map<Border, Double> acExchanges) {
        this.acExchanges = acExchanges;
    }

    public Map<Border, Double> getVirtualHubsExchanges() {
        return virtualHubsExchanges;
    }

    public void setVirtualHubsExchanges(Map<Border, Double> virtualHubsExchanges) {
        this.virtualHubsExchanges = virtualHubsExchanges;
    }
}
