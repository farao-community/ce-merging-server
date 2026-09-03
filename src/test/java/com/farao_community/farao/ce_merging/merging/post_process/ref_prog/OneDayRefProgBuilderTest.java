///*
// * Copyright (c) 2026, RTE (http://www.rte-france.com)
// * This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/.
// */
//package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;
//
//import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
//import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
//import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
//import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
//import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
//import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
//import com.rte_france.gridcapa.core_merging.DailyCoreMergingRepository;
//import com.rte_france.gridcapa.core_merging.entities.DailyCoreMergingEntity;
//import com.rte_france.gridcapa.core_merging.entities.Outputs;
//import com.rte_france.gridcapa.core_merging.exceptions.CoreMergingException;
//import com.rte_france.gridcapa.core_merging.post_process.merging_request.RequestInformation;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
///**
// * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
// * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
// */
//@SpringBootTest
//public class OneDayRefProgBuilderTest {
//    private MergingTask task1;
//    private MergingTask task2;
//    private MergingTask task3;
//    private MergingTask task4;
//    private MergingTask task5;
//    private Path resourceDirectory = Paths.get("src", "test", "resources", "refProg");
//    private String absolutePath = resourceDirectory.toFile().getAbsolutePath();
//
//    @Autowired
//    private DailyCoreMergingRepository dailyCoreMergingRepository;
//
//    @Autowired
//    private CeMergingConfiguration configuration;
//
//    @Autowired
//    private OneDayRefProgBuilder oneDayRefProgBuilder;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        SavedFile refProg1 = new SavedFile("refProg1.xml", absolutePath.concat("/refProg1.xml"), "mock");
//        task1 = new MergingTask();
//        Outputs outputs1 = new Outputs();
//        outputs1.setRefProg(refProg1);
//        task1.setOutputs(outputs1);
//        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task1)));
//
//        SavedFile refProg2 = new SavedFile("refProg2.xml", absolutePath.concat("/refProg2.xml"), "mock");
//        task2 = new MergingTask();
//        Outputs outputs2 = new Outputs();
//        outputs2.setRefProg(refProg2);
//        task2.setOutputs(outputs2);
//        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task2)));
//
//        SavedFile refProg3 = new SavedFile("20200106_2230_CORESO_RefProg.xml", absolutePath.concat("/20200106_2230_CORESO_RefProg.xml"), "mock");
//        task3 = new MergingTask();
//        Outputs outputs3 = new Outputs();
//        outputs3.setRefProg(refProg3);
//        task3.setOutputs(outputs3);
//        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task3)));
//
//        SavedFile refProg4 = new SavedFile("20200106_2330_CORESO_RefProg.xml", absolutePath.concat("/20200106_2330_CORESO_RefProg.xml"), "mock");
//        task4 = new MergingTask();
//        Outputs outputs4 = new Outputs();
//        outputs4.setRefProg(refProg4);
//        task4.setOutputs(outputs4);
//        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task4)));
//
//        SavedFile refProg5 = new SavedFile("20190618_0030_CORESO_RefProg.xml", absolutePath.concat("/20190618_0030_CORESO_RefProg.xml"), "mock");
//        task5 = new MergingTask();
//        Outputs outputs5 = new Outputs();
//        outputs5.setRefProg(refProg5);
//        task5.setOutputs(outputs5);
//        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task5)));
//    }
//
//    @Test
//    public void getOneDayRefProgTest() {
//
//        List<MergingTask> tasksList = Arrays.asList(task1, task2);
//        PublicationDocument publicationDocument = oneDayRefProgBuilder.buildOneDayRefProg(1, tasksList, 24);
//        assertEquals(1, publicationDocument.getDtdVersion().intValue());
//        assertEquals(0, publicationDocument.getDtdRelease().intValue());
//
//        assertEquals(68, publicationDocument.getPublicationTimeSeries().size());
//        assertEquals(24, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().size());
//        assertEquals(-230, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().get(21).getQty().getV().intValue());
//        assertEquals(10, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().get(23).getQty().getV().intValue());
//        assertEquals(24, publicationDocument.getPublicationTimeSeries().get(1).getPeriod().getInterval().size());
//
//        assertEquals("RS-ME", publicationDocument.getPublicationTimeSeries().get(0).getTimeSeriesIdentification().getV());
//        assertEquals("10YCS-SERBIATSOV", publicationDocument.getPublicationTimeSeries().get(0).getOutArea().getV());
//        assertEquals("10YCS-SERBIATSOV", publicationDocument.getPublicationTimeSeries().get(0).getOutArea().getV());
//        assertEquals("10YCS-CG-TSO---S", publicationDocument.getPublicationTimeSeries().get(0).getInArea().getV());
//        assertEquals(24, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().size());
//        assertEquals(1, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().get(0).getPos().getV().intValue());
//        assertEquals(-230, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().get(21).getQty().getV().intValue());
//        assertEquals(2, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().get(1).getPos().getV().intValue());
//        assertEquals(10, publicationDocument.getPublicationTimeSeries().get(0).getPeriod().getInterval().get(23).getQty().getV().intValue());
//
//        assertEquals("GR-MK", publicationDocument.getPublicationTimeSeries().get(1).getTimeSeriesIdentification().getV());
//        assertEquals("10YCB-GREECE---2", publicationDocument.getPublicationTimeSeries().get(1).getOutArea().getV());
//        assertEquals("10YMK-MEPSO----8", publicationDocument.getPublicationTimeSeries().get(1).getInArea().getV());
//        assertEquals(24, publicationDocument.getPublicationTimeSeries().get(1).getPeriod().getInterval().size());
//        assertEquals(22, publicationDocument.getPublicationTimeSeries().get(1).getPeriod().getInterval().get(21).getPos().getV().intValue());
//        assertEquals(-327, publicationDocument.getPublicationTimeSeries().get(1).getPeriod().getInterval().get(21).getQty().getV().intValue());
//        assertEquals(24, publicationDocument.getPublicationTimeSeries().get(1).getPeriod().getInterval().get(23).getPos().getV().intValue());
//        assertEquals(-309, publicationDocument.getPublicationTimeSeries().get(1).getPeriod().getInterval().get(23).getQty().getV().intValue());
//
//        assertEquals("NL-DK1_Cobra", publicationDocument.getPublicationTimeSeries().get(65).getTimeSeriesIdentification().getV());
//        assertEquals("10YNL----------L", publicationDocument.getPublicationTimeSeries().get(65).getOutArea().getV());
//        assertEquals("17YXXXXXXAAAAAAB", publicationDocument.getPublicationTimeSeries().get(65).getInArea().getV());
//        assertEquals(24, publicationDocument.getPublicationTimeSeries().get(65).getPeriod().getInterval().size());
//        assertEquals(22, publicationDocument.getPublicationTimeSeries().get(65).getPeriod().getInterval().get(21).getPos().getV().intValue());
//        assertEquals(-683, publicationDocument.getPublicationTimeSeries().get(65).getPeriod().getInterval().get(21).getQty().getV().intValue());
//        assertEquals(24, publicationDocument.getPublicationTimeSeries().get(65).getPeriod().getInterval().get(23).getPos().getV().intValue());
//        assertEquals(-683, publicationDocument.getPublicationTimeSeries().get(65).getPeriod().getInterval().get(23).getQty().getV().intValue());
//    }
//
//    @Test
//    public void shouldExportBorderWhenBorderPresentForTaskButNotForAnother() {
//        List<MergingTask> tasksList = Arrays.asList(task3, task4);
//        PublicationDocument publicationDocument = oneDayRefProgBuilder.buildOneDayRefProg(1, tasksList, 24);
//        List<String> timeSeriesIdentificationList = publicationDocument.getPublicationTimeSeries().stream().map(publication -> publication.getTimeSeriesIdentification().getV()).collect(Collectors.toList());
//        assertTrue(timeSeriesIdentificationList.contains("RS-ME"));
//    }
//
//    @Test
//    public void shouldThrowExceptionWhenTasksHaveNotTheSameInterval() {
//        List<MergingTask> tasksList = Arrays.asList(task4, task5);
//        List<PublicationDocument> refProgResultsList = oneDayRefProgBuilder.getAllRefProgResult(tasksList);
//        assertThrows(CeMergingException.class, () -> {
//            oneDayRefProgBuilder.checkThatAllTasksAreInTheSameInterval(refProgResultsList);
//        });
//    }
//
//    @Test
//    public void shouldNotThrowExceptionWhenTasksHaveSameInterval() {
//        List<MergingTask> tasksList = Arrays.asList(task1, task2, task3, task4);
//        List<PublicationDocument> refProgResultsList = oneDayRefProgBuilder.getAllRefProgResult(tasksList);
//        oneDayRefProgBuilder.checkThatAllTasksAreInTheSameInterval(refProgResultsList);
//    }
//
//    @Test
//    public void testSaveRefProgFile() throws IOException {
//        DailyCoreMergingEntity dailyCoreMergingEntity = new DailyCoreMergingEntity();
//        int version = 4;
//        dailyCoreMergingEntity.setVersion(version);
//        dailyCoreMergingEntity = dailyCoreMergingRepository.save(dailyCoreMergingEntity);
//        Files.createDirectories(Paths.get(configuration.getDailyOutputsDirectoryPath(dailyCoreMergingEntity)));
//        List<MergingTask> tasksList = Arrays.asList(task1, task2);
//        RequestInformation requestInformation = new RequestInformation();
//        requestInformation.setTimeInterval("2020-01-05T23:00Z/2020-01-06T23:00Z");
//        oneDayRefProgBuilder.computeOneDayRefProg(dailyCoreMergingEntity, tasksList, requestInformation);
//        assertTrue(new File(dailyCoreMergingEntity.getDailyOutputs().getRefProg().getPath()).exists());
//        String fileName = "22XCORESO------S_10V1001C--00236Y_CORE-FB-A45-101_20200106-F101-04.xml";
//        assertEquals(fileName, dailyCoreMergingEntity.getDailyOutputs().getRefProg().getOriginalName());
//    }
//
//}
