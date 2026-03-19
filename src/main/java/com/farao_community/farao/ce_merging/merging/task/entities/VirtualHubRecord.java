/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class VirtualHubRecord implements Serializable {
    private String code;
    private String eic;
    private String nodeName;
    private String relatedMaCode;
    private String relatedMaEic;

    public String getEic() {
        return eic;
    }

    public void setEic(final String eic) {
        this.eic = eic;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public String getRelatedMaCode() {
        return relatedMaCode;
    }

    public void setRelatedMaCode(final String relatedMaCode) {
        this.relatedMaCode = relatedMaCode;
    }

    public String getRelatedMaEic() {
        return relatedMaEic;
    }

    public void setRelatedMaEic(final String relatedMaEic) {
        this.relatedMaEic = relatedMaEic;
    }
}
