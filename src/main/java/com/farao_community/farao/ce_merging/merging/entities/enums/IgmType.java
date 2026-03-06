/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.entities.enums;

import java.util.Arrays;

public enum IgmType {

    SNAPSHOT("SN"),
    DACF("FO"),
    D2CF("2D");

    private final String typeCode;

    IgmType(final String typeCode) {
        this.typeCode = typeCode;
    }

    public static IgmType fromTypeCode(final String typeCode) {
        return Arrays.stream(IgmType.values())
            .filter(type -> type.typeCode.equals(typeCode))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Type code '%s' not recognized",
                                                                          typeCode)));
    }

}
