/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

public enum XnodeStatus {
    OPEN(0),
    CLOSE(1);

    private final int status;

    XnodeStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
