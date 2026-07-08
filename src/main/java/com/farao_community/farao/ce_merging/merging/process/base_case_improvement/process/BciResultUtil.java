/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciProcessResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

public final class BciResultUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(BciResultUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = JsonUtil.createObjectMapper();

    private BciResultUtil() {
    }

    public static void write(final BciProcessResult result, final OutputStream os) {
        try {
            OBJECT_MAPPER.configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .writerWithDefaultPrettyPrinter()
                .writeValue(os, result);

        } catch (final IOException e) {
            LOGGER.error("Error while writing result of '{}' region", result.regionName(), e);
            throw new ServiceIOException(String.format("Error while writing result of '%s' region", result.regionName()), e);
        }
    }

    public static BciProcessResult read(final InputStream is) {
        try {
            return OBJECT_MAPPER.readValue(is, BciProcessResult.class);
        } catch (final IOException e) {
            LOGGER.error("Error while reading result", e);
            throw new ServiceIOException("Error while reading result", e);
        }
    }

}
