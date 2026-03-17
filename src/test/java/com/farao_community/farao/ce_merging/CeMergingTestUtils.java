/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.ArgumentMatchers.any;

public final class CeMergingTestUtils {

    private CeMergingTestUtils() {
        // utility class
    }

    public static ServiceIOException testServiceEx = new ServiceIOException("Test");

    /**
     *
     * @param fileName must be in testFiles (or children if specified in fileName) in order for this to work
     * @return ...../testFiles/fileName
     */
    public static Path pathOfTestFile(final String fileName) {
        return Paths.get(Optional.ofNullable(CeMergingTestUtils.class
                                                 .getResource("/testFiles/%s".formatted(fileName)))
                             .orElse(CeMergingTestUtils.class
                                         .getResource("/testFiles/blank.file"))
                             .getPath());
    }

    public static String stringPathOfTestFile(final String fileName) {
        return pathOfTestFile(fileName).toString();
    }

    /**
     * for the sake of readability
     *
     * @param object to serialize
     * @return serialized json object
     * @throws JsonProcessingException should never happen
     */
    public static String stringify(final Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

    public static byte[] byteContentOfTestFile(final String fileName) {
        try {
            return readFileToByteArray(new File(stringPathOfTestFile(fileName)));
        } catch (final Exception e) {
            return new byte[0];
        }
    }

    public static String stringContentOfTestFile(final String fileName) {
        try {
            return readFileToString(new File(stringPathOfTestFile(fileName)), UTF_8);
        } catch (final Exception e) {
            return "";
        }
    }

    public static MergingTask anyTask() {
        return any(MergingTask.class);
    }
}
