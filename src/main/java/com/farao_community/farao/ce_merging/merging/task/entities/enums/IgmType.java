/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities.enums;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;

import static java.util.Arrays.stream;

public enum IgmType {

    SNAPSHOT("SN"),
    DACF("FO"),
    D2CF("2D"),
    REFERENCE("RE"),
    LONG_TERM_REFERENCE("LR");

    private final String typeCode;

    IgmType(final String typeCode) {
        this.typeCode = typeCode;
    }

    public static IgmType fromTypeCode(final String typeCode) {
        return stream(values())
            .filter(type -> type.typeCode.equals(typeCode))
            .findAny()
            .orElseThrow(() -> new CeMergingException(String.format("Type code '%s' not recognized",
                                                                    typeCode)));
    }

}
