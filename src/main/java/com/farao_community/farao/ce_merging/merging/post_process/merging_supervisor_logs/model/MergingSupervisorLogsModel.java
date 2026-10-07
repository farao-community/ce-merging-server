/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.model;

public class MergingSupervisorLogsModel {

    private String timestamp;
    private String level;
    private String message;
    private String context;
    private String subContextLevel1;

    public MergingSupervisorLogsModel(String timestamp, String level, String message, String context) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.context = context;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSubContextLevel1() {
        return subContextLevel1;
    }

    public void setSubContextLevel1(String subContextLevel1) {
        this.subContextLevel1 = subContextLevel1;
    }
}
