/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;

import java.util.Map;


public record RefProgResult(String dailyTimeInterval, Map<Border, Double> acExchanges, Map<Border, Double> virtualHubsExchanges) { }
