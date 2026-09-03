/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.common;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public enum SchemaLocationNamespace {

    GLSK_QUALITY_CHECK_XSD("quality-check-report-08.xsd"),
    REFPROG_XSD("publication-document-v2r0.xsd"),
    RESPONSE_XSD("common-message-specification-01.xsd"),
    RESPONSE_PAYLOAD_XSD("response-payload-01.xsd"),
    MERGING_LOGS_XSD("flowbased flowbasedmerginglog-01.xsd");

    private final String name;

    SchemaLocationNamespace(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
