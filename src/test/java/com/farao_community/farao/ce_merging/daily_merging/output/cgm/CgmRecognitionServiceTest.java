/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging.output.cgm;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.ResponseItem;
import com.farao_community.farao.ce_merging.xsd.daily.response.payload.ResponseItems;
import com.farao_community.farao.ce_merging.xsd.merging_request.EventMessageType;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.JaxbUtils.readNode;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CgmRecognitionServiceTest {
    @Autowired
    private CgmRecognitionService cgmRecognitionService;
    @Autowired
    private CeMergingConfiguration configuration;

    private List<MergingTask> tasks;
    private RequestInformation requestInformation;
    private MergingTask task3;

    @BeforeEach
    void setUp() throws IOException {
        final SavedFile cgm1 = new SavedFile("mock_cgm_1.uct", getClass().getResource("/cgmResult/mock_cgm_1.uct").getPath(), "mock");
        final MergingTask task1 = new MergingTask();
        final Outputs outputs1 = new Outputs();
        outputs1.setCgm(cgm1);
        task1.setOutputs(outputs1);
        final Inputs inputs1 = new Inputs();
        inputs1.setTargetDate(OffsetDateTime.parse("2020-01-06T01:30Z"));
        task1.setInputs(inputs1);
        task1.setStatus(SUCCESS);

        final SavedFile cgm2 = new SavedFile("mock_cgm_2.uct", getClass().getResource("/cgmResult/mock_cgm_2.uct").getPath(), "mock");
        final MergingTask task2 = new MergingTask();
        final Outputs outputs2 = new Outputs();
        outputs2.setCgm(cgm2);
        task2.setOutputs(outputs2);
        final Inputs inputs2 = new Inputs();
        inputs2.setTargetDate(OffsetDateTime.parse("2020-01-06T02:30Z"));
        task2.setInputs(inputs2);
        task2.setStatus(SUCCESS);

        task3 = new MergingTask();
        final Inputs inputs3 = new Inputs();
        inputs3.setTargetDate(OffsetDateTime.parse("2020-01-06T04:30Z"));
        task3.setInputs(inputs3);
        task3.setStatusDetail(String.format("Merging failed for task %d with target date %s", task3.getId(), task3.getInputs().getTargetDate()));
        task3.setStatus(ERROR);

        tasks = List.of(task1, task2, task3);

        requestInformation = new RequestInformation("2020-01-05T23:00Z/2020-01-06T23:00Z",
                                                    "CommonGridModel",
                                                    "PRODUCTION",
                                                    "22XCORESO------S",
                                                    "62613970-472b-405a-b8c9-9e1be581f519");
    }

    @Test
    void shouldComputeCgmRecognition() throws JAXBException, ParserConfigurationException {
        final int version = 5;
        final byte[] cgmRecognitionBytes = cgmRecognitionService.computeCgmRecognition(requestInformation, tasks, version);
        final EventMessageType cgmRecognition = JaxbUtils.readFromBytes(EventMessageType.class, cgmRecognitionBytes);

        assertEquals("PRODUCTION", cgmRecognition.getHeader().getContext());
        assertEquals("22XCORESO------S", cgmRecognition.getHeader().getSource());
        assertEquals("5", cgmRecognition.getHeader().getRevision());
        assertEquals("CommonGridModel", cgmRecognition.getHeader().getNoun());
        assertEquals("20200106-F100-05", cgmRecognition.getHeader().getMessageID());

        assertEquals("62613970-472b-405a-b8c9-9e1be581f519", cgmRecognition.getHeader().getCorrelationID());
        final String timeInterval = cgmRecognition.getPayload().getAny().getFirst().getAttribute("timeInterval");
        assertEquals("2020-01-05T23:00Z/2020-01-06T23:00Z", timeInterval);

        final ResponseItems responseItems = readNode(cgmRecognition.getPayload().getAny().getFirst(), ResponseItems.class);
        final List<ResponseItem> responseItemList = responseItems.getResponseItem();
        assertEquals(24, responseItemList.size());

        assertEquals("2020-01-06T01:00Z/2020-01-06T02:00Z", responseItemList.get(2).getTimeInterval());
        assertEquals("2020-01-06T02:00Z/2020-01-06T03:00Z", responseItemList.get(3).getTimeInterval());
        assertEquals("2020-01-06T04:00Z/2020-01-06T05:00Z", responseItemList.get(5).getTimeInterval());

        assertEquals(1, responseItemList.get(2).getFiles().getFile().size());
        assertEquals("CGM", responseItemList.get(2).getFiles().getFile().getFirst().getCode());
        assertEquals("fileName://mock_cgm_1.uct", responseItemList.get(2).getFiles().getFile().getFirst().getUrl());

        final String expectedErrorReason = String.format("Merging failed for task %d with target date %s", task3.getId(), task3.getInputs().getTargetDate());
        assertEquals(expectedErrorReason, responseItemList.get(5).getError().getReason());
    }
}
