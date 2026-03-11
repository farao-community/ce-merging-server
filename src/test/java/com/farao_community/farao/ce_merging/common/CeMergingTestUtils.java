/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

@NoArgsConstructor(access = AccessLevel.NONE)
public class CeMergingTestUtils {

    public static ServiceIOException testServiceEx = new ServiceIOException("Test");

    /**
     *
     * @param fileName must be in testFiles (or children if specified in fileName) in order for this to work
     * @return ...../testFiles/fileName
     */
    public static Path pathOfTestFile(final String fileName) {
        return Paths.get(requireNonNull(CeMergingTestUtils.class
                                            .getResource("/testFiles/%s".formatted(fileName)))
                             .getPath());
    }

    public static String stringPathOfTestFile(final String fileName) {
        return pathOfTestFile(fileName).toString();
    }

    /**
     * for the sake of readability
     * @param object to serialize
     * @return serialized json object
     * @throws JsonProcessingException should never happen
     */
    public static String stringify(final Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }
}
