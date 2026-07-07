/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.common.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_UP;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public class DoubleSerializer extends JsonSerializer<Double> {

    @Override
    public void serialize(Double value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(BigDecimal.valueOf(value).setScale(2, HALF_UP).doubleValue());
    }
}
