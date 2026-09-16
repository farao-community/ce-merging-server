/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.merging_request;

import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MergingRequestServiceTest {

    private static final long DAILY_TASK_ID = 1L;
    private static final String MERGING_REQUEST_FILE = "merging_request.xml";

    @Autowired
    private MergingRequestService mergingRequestService;

    private DailyMergingTask dailyTask;

    @BeforeEach
    void setUp() {
        final Path mergingRequestPath = Path.of(
                "src",
                "test",
                "resources",
                "merging_request",
                MERGING_REQUEST_FILE);
        final SavedFile mergingRequest = new SavedFile(MERGING_REQUEST_FILE, mergingRequestPath.toAbsolutePath().toString(), "mock");
        final DailyInputs dailyInputs = new DailyInputs();
        dailyInputs.setMergingRequest(mergingRequest);

        dailyTask = new DailyMergingTask();
        dailyTask.setId(DAILY_TASK_ID);
        dailyTask.setDailyInputs(dailyInputs);
    }

    @Test
    void shouldGetMergingRequestInformation() {
        final RequestInformation requestInformation = mergingRequestService.getMergingRequestInformation(dailyTask);
        assertEquals("PRODUCTION", requestInformation.context());
        assertEquals("NOUN", requestInformation.noun());
        assertEquals("CORESO", requestInformation.replyAddress());
        assertEquals("correlation_id_123", requestInformation.correlationID());
        assertEquals("2020-02-16T23:00Z/2020-02-17T23:00Z", requestInformation.requestTimeInterval());
        assertEquals("2020-02-17T23:00Z", requestInformation.getMergingDay().toString());
        assertEquals("2020-02-16T23:00Z", requestInformation.getStartDateTime().toString());
        assertEquals("2020-02-17T23:00Z", requestInformation.getEndDateTime().toString());
    }
}
