/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ThrowableAssert;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.ArgumentMatchers.any;

public final class CeTestUtils {

    private CeTestUtils() {
        // utility class
    }

    private static final Class<CeTestUtils> THIS = CeTestUtils.class;
    private static final String DEFAULT_FILE = "blank.file";
    private static final String ZIP_NAME = "inputs.zip";

    public static final ServiceIOException S_IO_EXCEPTION = new ServiceIOException("Test");

    public static Path pathOf(final String fileName) {
        return Paths.get(Optional.ofNullable(THIS.getResource("/" + fileName))
                             .orElse(THIS.getResource("/" + DEFAULT_FILE))
                             .getPath());
    }

    public static String stringPathOf(final String fileName) {
        return pathOf(fileName).toString();
    }

    public static String stringify(final Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

    public static byte[] byteContentOf(final String fileName) {
        try {
            return readFileToByteArray(new File(stringPathOf(fileName)));
        } catch (final Exception e) {
            return new byte[0];
        }
    }

    public static String stringContentOf(final String fileName) {
        try {
            return readFileToString(new File(stringPathOf(fileName)), UTF_8);
        } catch (final Exception e) {
            return "";
        }
    }

    public static MergingTask anyTask() {
        return any(MergingTask.class);
    }

    public static MergingTask withIdAndStatus(final long id, final TaskStatus status) {
        final MergingTask task = new MergingTask();
        task.setTaskId(id);
        task.setArchiveFileOriginalName(ZIP_NAME);
        task.setTaskStatus(status);

        return task;
    }

    public static Stream<ThrowableAssert.ThrowingCallable> calls(final ThrowableAssert.ThrowingCallable... calls) {
        return Stream.of(calls);
    }
}
