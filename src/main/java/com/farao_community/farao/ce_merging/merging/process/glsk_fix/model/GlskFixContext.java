/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.glsk_fix.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlskFixContext {

    private final Map<String, List<GlskSerieShare>> incorrectBlockByGskName = new HashMap<>();
    private final Map<String, List<GlskSerieShare>> correctBlockByGskName = new HashMap<>();
    private final Map<String, List<GlskSerieShare>> correctSeriesByArea = new HashMap<>();

    public Map<String, List<GlskSerieShare>> getIncorrectBlockByGskName() {
        return incorrectBlockByGskName;
    }

    public Map<String, List<GlskSerieShare>> getCorrectBlockByGskName() {
        return correctBlockByGskName;
    }

    public Map<String, List<GlskSerieShare>> getCorrectSeriesByArea() {
        return correctSeriesByArea;
    }
}
