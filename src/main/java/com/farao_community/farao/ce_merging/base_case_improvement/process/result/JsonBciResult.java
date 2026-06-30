/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process.result;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * Utility class for Base case improvement result file JSON
 *
 * @author Walha Ameni {@literal <ameni.walha at rte-france.com>}
 */
public final class JsonBciResult {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonBciResult.class);

    private JsonBciResult() {
    }

    public static void write(final BciProcessResult result, final OutputStream os) {
        try {
            createObjectMapper()
                .configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .writerWithDefaultPrettyPrinter()
                .writeValue(os, result);

        } catch (final IOException e) {
            LOGGER.error("Error during write result of '{}' region", result.getRegionName(), e);
            throw new ServiceIOException(String.format("Error during write result of '%s' region", result.getRegionName()), e);
        }
    }

    public static BciProcessResult read(final InputStream is) {
        try {
            return createObjectMapper().readValue(is, BciProcessResult.class);
        } catch (final IOException e) {
            LOGGER.error("Error during read result", e);
            throw new ServiceIOException("Error during read result", e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper();
    }
}
