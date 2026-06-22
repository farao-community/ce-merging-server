/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
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
