/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process;

import com.farao_community.farao.ce_merging.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.base_case_improvement.data.netpositions.CountryNetPositions;
import com.farao_community.farao.ce_merging.base_case_improvement.data.netpositions.InitialNetPositions;
import com.farao_community.farao.ce_merging.base_case_improvement.data.netpositions.NetPosition;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.farao_community.farao.ce_merging.base_case_improvement.data.FlowByAreaMap.toFlowByAreaMap;
import static com.powsybl.commons.json.JsonUtil.createObjectMapper;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class InitialNetPositionsImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialNetPositionsImporter.class);

    private InitialNetPositionsImporter() {
    }

    static FlowByAreaMap getInRegionNetPositions(final InputStream initialNpFile,
                                                 final RegionConfiguration region) {
        return getNetPositionsWithoutVirtualHubs(initialNpFile, region,
                                                 CountryNetPositions::getInRegionNetPosition);
    }

    public static FlowByAreaMap getGlobalNetPosition(final String netPosFilePath,
                                                     final RegionConfiguration region) throws FileNotFoundException {

        if (isBlank(netPosFilePath)) {
            return new FlowByAreaMap();
        }

        return getNetPositionsWithoutVirtualHubs(new FileInputStream(netPosFilePath), region,
                                                 CountryNetPositions::getGlobalNetPosition);
    }

    private static FlowByAreaMap getNetPositionsWithoutVirtualHubs(final InputStream initialNpFile,
                                                                   final RegionConfiguration region,
                                                                   final Function<CountryNetPositions, NetPosition> npGetter) {
        final Map<String, String> areasIdByCountry = getAreasIdByCountry(region);

        return netPositionsMapFromContent(initialNpFile)
            .entrySet()
            .stream()
            .filter(entry -> areasIdByCountry.containsKey(entry.getKey()))
            .collect(toFlowByAreaMap(entry -> areasIdByCountry.get(entry.getKey()),
                                       entry -> npGetter.apply(entry.getValue()).getWithoutVirtualHubs()));
    }

    private static Map<String, String> getAreasIdByCountry(final RegionConfiguration region) {
        final Map<String, String> allAreas = new HashMap<>();
        allAreas.putAll(region.getAreasIn());
        allAreas.putAll(region.getAreasOut());
        return allAreas;
    }

    private static Map<String, CountryNetPositions> netPositionsMapFromContent(final InputStream is) {
        try {
            final InitialNetPositions netPositions = createObjectMapper().readValue(is, InitialNetPositions.class);
            return netPositions.getCountryNetPositionsMap();
        } catch (IOException e) {
            LOGGER.error("Error while reading initial net positions in BCI process", e);
            throw new ServiceIOException("Error while reading initial net positions in BCI process", e);
        }
    }

}


