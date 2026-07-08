/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.Interval;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.xsd.FlowBasedExternalConstraintDocument;
import com.farao_community.farao.ce_merging.xsd.NetPositionConstraint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test_utils.CeTestUtils;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static java.lang.Double.MAX_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.assertions.IntervalAssert.assertThat;

class ExternalConstraintsImporterTest {

    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2019-06-18T06:00Z", DateTimeFormatter.ISO_DATE_TIME);
    private String correctAlegroFilePath;
    private byte[] externalConstraints;
    private byte[] externalConstraintsAlegro;
    private RegionConfiguration regionConfiguration;

    @BeforeEach
    void setUp() throws IOException {
        externalConstraints = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/20190618_MergedEC.xml"));
        correctAlegroFilePath = CeTestUtils.stringPathOf("bci/F229-MergedECs_v01_Alegro.xml");
        externalConstraintsAlegro = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/F229-MergedECs_v01_Alegro.xml"));

        ObjectMapper objectMapper = new ObjectMapper();
        File resourceRegion = CeTestUtils.pathOf("bci/region_configuration.json").toFile();
        String jsonConfig = new String(java.nio.file.Files.readAllBytes(resourceRegion.toPath()));
        regionConfiguration = objectMapper.readValue(jsonConfig, RegionConfiguration.class);
    }

    @Test
    void calculateConstraints() {
        final Map<String, Interval> ecs = ExternalConstraintsImporter.calculateConstraints(externalConstraints, regionConfiguration, TARGET_DATE);
        assertEquals(12, ecs.size());

        assertThat(ecs.get("AREA_NUMBER12_EIC")).rangeIs(-6500, MAX_VALUE);
        assertThat(ecs.get("AREA_NUMBER25_EIC")).rangeIs(-6500, 6500);
        assertThat(ecs.get("AREA_NUMBER26_EIC")).rangeIs(-6875, 0);
        assertThat(ecs.get("AREA_NUMBER1_EIC")).rangeIs(-MAX_VALUE, MAX_VALUE);
    }

    @Test
    void checkExternalConstraintValues() throws JAXBException {
        final FlowBasedExternalConstraintDocument ecFile = getECFile(correctAlegroFilePath);
        List<NetPositionConstraint> cts = ecFile.getConstraints().getNetPositionConstraint();
        assertEquals(10, cts.size());

        assertEquals("BE_ALEGrO", cts.get(6).getHub());
        assertEquals(11285, cts.get(6).getValue().intValue());
        assertEquals("ABSOLUTE", cts.get(6).getType());
        assertEquals("1", cts.get(6).getId());
        assertEquals("IMPORT", cts.get(6).getDirection());

        assertEquals("BE_ALEGrO", cts.get(7).getHub());
        assertEquals(11285, cts.get(7).getValue().intValue());
        assertEquals("ABSOLUTE", cts.get(7).getType());
        assertEquals("25", cts.get(7).getId());
        assertEquals("EXPORT", cts.get(7).getDirection());

        assertEquals("DE_ALEGrO", cts.get(8).getHub());
        assertEquals(11285, cts.get(8).getValue().intValue());
        assertEquals("ABSOLUTE", cts.get(8).getType());
        assertEquals("1", cts.get(8).getId());
        assertEquals("IMPORT", cts.get(8).getDirection());

        assertEquals("DE_ALEGrO", cts.get(9).getHub());
        assertEquals(11285, cts.get(9).getValue().intValue());
        assertEquals("ABSOLUTE", cts.get(9).getType());
        assertEquals("25", cts.get(9).getId());
        assertEquals("EXPORT", cts.get(9).getDirection());
    }

    private FlowBasedExternalConstraintDocument getECFile(String path) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FlowBasedExternalConstraintDocument.class);
        Unmarshaller jaxbMarshaller = jaxbContext.createUnmarshaller();
        FlowBasedExternalConstraintDocument document = (FlowBasedExternalConstraintDocument) jaxbMarshaller.unmarshal(new File(path));
        return document;
    }
}
