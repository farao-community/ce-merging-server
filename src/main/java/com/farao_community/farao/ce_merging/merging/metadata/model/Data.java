/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.metadata.model;

import com.farao_community.farao.ce_merging.merging.entities.Configurations;

@lombok.Data
public class Data {
    private String type;
    private AttributesMetadata attributes;
    private Configurations configurations;
}
