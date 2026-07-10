/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.process.FileStorageService;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.*;

import com.powsybl.commons.report.ReportNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlskFixServiceTest {

    public static final String INVALID_TARGET_DATE = "2016-07-30T10:00:00Z";
    @Mock
    private CeMergingConfiguration configuration;

    @Mock
    private FileStorageService storage;

    private GlskFixService glskFixService;

    private static final String TARGET_DATE = "2016-07-29T00:00:00Z";
    private static final String CREATION_DATE = "2026-01-01T00:00:00Z";
    private static final String GLSK_FIX_RESOURCE_PATH = "glskFix";

    @BeforeEach
    void setUp() {
        glskFixService = new GlskFixService(configuration, storage);
    }

    @Test
    void shouldUpdateCreationDateWhenProcessingGlskFile() throws Exception {
        final byte[] glskBytes = readGlskFile(GLSK_FIX_RESOURCE_PATH + "/20160728_GLSK.xml");
        final GSKDocument resultDocument = fixGlsk(glskBytes, createEmptyReportNode());
        assertNotNull(resultDocument);
        assertEquals(createCreationDate(), resultDocument.getCreationDateTime().getV());
        assertEquals(3, resultDocument.getGSKSeries().size());
    }

    @Test
    void shouldRemoveEmptyGlskSeries() throws Exception {
        final byte[] glskBytes = readGlskFile(GLSK_FIX_RESOURCE_PATH + "/20160728_GLSK_with_one_empty_glskSeries.xml");
        final ReportNode reportNode = createEmptyReportNode();
        final GSKDocument resultDocument = fixGlsk(glskBytes, reportNode);
        assertEquals(2, resultDocument.getGSKSeries().size());
    }

    @Test
    void shouldThrowExceptionWhenTargetDateIsOutsideGlskTimeInterval() throws Exception {
        final byte[] glskBytes = readGlskFile(GLSK_FIX_RESOURCE_PATH + "/20160728_GLSK.xml");
        final OffsetDateTime targetDate = OffsetDateTime.parse(INVALID_TARGET_DATE);
        final CeMergingException exception = assertThrows(
                CeMergingException.class,
                () -> glskFixService.checkTimeInterval(glskBytes, targetDate)
        );
        assertThat(exception.getMessage()).contains("The time interval of Glsk document does not correspond");
    }

    private GSKDocument fixGlsk(final byte[] glskBytes, final ReportNode reportNode) {
        final byte[] result = glskFixService.fixGlsk(glskBytes, reportNode, Instant.parse(TARGET_DATE), createCreationDate());
        return JaxbUtils.readFromBytes(GSKDocument.class, result);
    }

    private XMLGregorianCalendar createCreationDate() {
        try {
            return DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(CREATION_DATE);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private ReportNode createEmptyReportNode() {
        final ReportNode reportNode = mock(ReportNode.class);
        when(reportNode.getChildren()).thenReturn(List.of());
        return reportNode;
    }

    private byte[] readGlskFile(final String filePath) throws IOException {
        try (final InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + filePath);
            }
            return inputStream.readAllBytes();
        }
    }
}
