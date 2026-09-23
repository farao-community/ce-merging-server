/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.outputs.merging_response;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import com.farao_community.farao.ce_merging.xsd.merging_request.EventMessageType;
import com.farao_community.farao.ce_merging.xsd.merging_response.File;
import com.farao_community.farao.ce_merging.xsd.merging_response.ResponseItem;
import com.farao_community.farao.ce_merging.xsd.merging_response.ResponseItems;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.w3c.dom.Element;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MergingResponseServiceTest {

    private static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources", "merging_response");
    private static final String REF_PROG = "REFPROG";
    private static final String CGM = "CGM";
    public static final String QCHECK_GLSK = "QCHECK_GLSK";
    public static final String MERGINGLOG = "MERGINGLOG";
    private static final String REF_PROG_URL = "documentIdentification://20200106-F101-03";
    private static final String CGM_URL = "documentIdentification://20200106-F100-03";
    private static final String GLSK_QUALITY_URL = "documentIdentification://20200106-F117-03";
    private static final String MERGING_LOG_URL = "documentIdentification://20200106-F123-03";

    private DailyMergingTask dailyTask;
    private List<MergingTask> mergingTasks;
    private RequestInformation requestInformation;

    @Autowired
    private DailyMergingRepository dailyMergingRepository;

    @Autowired
    private CeMergingConfiguration configuration;

    @Autowired
    private MergingResponseService mergingResponseService;

    @BeforeEach
    void setUp() throws IOException {
        final SavedFile mergingRequestFile = new SavedFile("merging_request.xml", RESOURCE_DIRECTORY.resolve("merging_request.xml").toString(), "mock");
        final MergingTask task1 = createMergingTask(1L, "2020-01-06T01:30Z", TaskStatus.SUCCESS);
        final MergingTask task2 = createMergingTask(2L, "2020-01-06T02:30Z", TaskStatus.SUCCESS);
        final MergingTask task3 = createMergingTask(3L, "2020-01-06T04:30Z", TaskStatus.ERROR);
        task3.setStatusDetail("error");
        mergingTasks = Arrays.asList(task1, task2, task3);
        dailyTask = new DailyMergingTask();
        final DailyInputs dailyInputs = new DailyInputs();
        dailyInputs.setMergingRequest(mergingRequestFile);
        dailyTask.setDailyInputs(dailyInputs);
        dailyTask.setVersion(3);
        dailyTask = dailyMergingRepository.save(dailyTask);
        Files.createDirectories(Paths.get(configuration.getDailyOutputsDirectoryPath(dailyTask)));
        requestInformation = new RequestInformation(
                "2020-01-05T23:00Z/2020-01-06T23:00Z",
                "MergingCommonGridModel",
                "PRODUCTION",
                "22XCORESO------S",
                "62613970-472b-405a-b8c9-9e1be581f519"
        );
    }

    @Test
    void shouldCreateMergingResponse() throws JAXBException, IOException {
        mergingResponseService.computeMergingResponse(dailyTask, mergingTasks, requestInformation);
        final SavedFile mergingResponseFile = dailyTask.getDailyOutputs().getMergingResponse();
        final EventMessageType response = readMergingResponseFromPath(mergingResponseFile.getPath());
        assertHeader(response);
        final Element payload = response.getPayload().getAny().get(0);
        assertEquals("2020-01-05T23:00Z/2020-01-06T23:00Z", payload.getAttribute("timeInterval"));
        final ResponseItems responseItems = readResponseItems(payload);
        final List<ResponseItem> responseItemList = responseItems.getResponseItem();
        assertEquals(24, responseItemList.size());
        final Map<String, ResponseItem> responseItemsByInterval = responseItemList.stream()
                .collect(Collectors.toMap(
                        ResponseItem::getTimeInterval,
                        Function.identity()
                ));
        final ResponseItem firstSuccessfulTask = responseItemsByInterval.get("2020-01-06T01:00Z/2020-01-06T02:00Z");
        assertSuccessfulResponseItem(firstSuccessfulTask);
        final ResponseItem secondSuccessfulTask = responseItemsByInterval.get("2020-01-06T02:00Z/2020-01-06T03:00Z");
        assertSuccessfulResponseItem(secondSuccessfulTask);
        final ResponseItem failedTask = responseItemsByInterval.get("2020-01-06T04:00Z/2020-01-06T05:00Z");
        assertEquals(1, failedTask.getFiles().getFile().size());
        assertFile(failedTask.getFiles().getFile().get(0), REF_PROG, REF_PROG_URL);
        final ResponseItem missingTask = responseItemsByInterval.get("2020-01-06T03:00Z/2020-01-06T04:00Z");
        assertEquals(1, missingTask.getFiles().getFile().size());
        assertFile(missingTask.getFiles().getFile().get(0), REF_PROG, REF_PROG_URL);
    }

    private void assertHeader(final EventMessageType response) {
        assertEquals("PRODUCTION", response.getHeader().getContext());
        assertEquals("22XCORESO------S", response.getHeader().getSource());
        assertEquals("62613970-472b-405a-b8c9-9e1be581f519", response.getHeader().getCorrelationID());
    }

    private void assertSuccessfulResponseItem(final ResponseItem responseItem) {
        assertEquals(4, responseItem.getFiles().getFile().size());
        assertFile(responseItem.getFiles().getFile().get(0), CGM, CGM_URL);
        assertFile(responseItem.getFiles().getFile().get(1), REF_PROG, REF_PROG_URL);
        assertFile(responseItem.getFiles().getFile().get(2), QCHECK_GLSK, GLSK_QUALITY_URL);
        assertFile(responseItem.getFiles().getFile().get(3), MERGINGLOG, MERGING_LOG_URL);
    }

    private void assertFile(final File file, final String expectedCode, final String expectedUrl) {
        assertEquals(expectedCode, file.getCode());
        assertEquals(expectedUrl, file.getUrl());
    }

    private MergingTask createMergingTask(final long id, final String targetDate, final TaskStatus status) throws IOException {
        final MergingTask task = new MergingTask();
        task.setId(id);
        final Inputs inputs = new Inputs();
        inputs.setTargetDate(OffsetDateTime.parse(targetDate));
        task.setInputs(inputs);
        task.setStatus(status);
        Files.createDirectories(Paths.get(configuration.getInputsDirectoryPath(task)));
        return task;
    }

    private EventMessageType readMergingResponseFromPath(final String path) throws IOException, JAXBException {
        try (final InputStream fileContent = Files.newInputStream(Paths.get(path))) {
            final JAXBContext jaxbContext = JAXBContext.newInstance(EventMessageType.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            final JAXBElement<EventMessageType> eventMessage = unmarshaller.unmarshal(new StreamSource(fileContent), EventMessageType.class);
            return eventMessage.getValue();
        }
    }

    private ResponseItems readResponseItems(final Element element) throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(ResponseItems.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final JAXBElement<ResponseItems> responseItems = unmarshaller.unmarshal(element, ResponseItems.class);
        return responseItems.getValue();
    }
}
