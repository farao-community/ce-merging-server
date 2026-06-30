/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.initial_netpositions;

import com.farao_community.farao.ce_merging.base_case_improvement.RegionConfiguration;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InitialNetPositionsImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialNetPositionsImporter.class);

    private InitialNetPositionsImporter() {
    }

    /**
     * get initial net positions in region without hvdc lines from file
     *
     * @return Map<String, Double> key correspond to area EICode and value correspond to the net position
     */
    static Map<String, Double> getInRegionNetPositions(final InputStream initialNetPositionsFile,
                                                       final RegionConfiguration region) {

        final InitialNetPositions initialNetPositions = read(initialNetPositionsFile);
        final Map<String, String> areasIdByCountry = Stream.concat(region.getAreasIn().entrySet().stream(), region.getAreasOut().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Map<String, InitialNetPositions.CountryNetPositions> countriesNetPositions = initialNetPositions.getCountryNetPositionsMap();

        return countriesNetPositions.entrySet().stream()
                .filter(entry -> areasIdByCountry.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> areasIdByCountry.get(entry.getKey()), entry -> entry.getValue().getInRegionNetPosition().getWithoutVirtualHubs()));
    }

    public static Map<String, Double> getGlobalNetPosition(final InputStream initialNetPositionsFile,
                                                           final RegionConfiguration region) {

        final InitialNetPositions initialNetPositions = read(initialNetPositionsFile);
        final Map<String, String> areasIdByCountry = Stream.concat(region.getAreasIn().entrySet().stream(), region.getAreasOut().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Map<String, InitialNetPositions.CountryNetPositions> countriesNetPositions = initialNetPositions.getCountryNetPositionsMap();

        return countriesNetPositions.entrySet().stream()
                .filter(entry -> areasIdByCountry.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> areasIdByCountry.get(entry.getKey()), entry -> entry.getValue().getGlobalNetPosition().getWithoutVirtualHubs()));
    }

    private static InitialNetPositions read(final InputStream is) {
        try {
            final ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readValue(is, InitialNetPositions.class);
        } catch (IOException e) {
            LOGGER.error("Error while reading initial net positions in BCI process", e);
            throw new ServiceIOException("Error while reading initial net positions in BCI process", e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper();
    }
}


