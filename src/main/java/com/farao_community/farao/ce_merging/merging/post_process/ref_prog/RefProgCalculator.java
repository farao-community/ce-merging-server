/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecCoefficients;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceExchangeData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.final_cgm_result.FinalCgmResult;
import com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.InternalBiddingZonesManager;
import com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import com.powsybl.loadflow.LoadFlow;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.util.CountryCodeUtils.mapDk1ToDk;
import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap.toFlowByAreaMap;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.isItalianBiddingZone;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public final class RefProgCalculator {
    private final MergingTask task;
    private final Map<Border, Double> virtualHubsExchanges = new HashMap<>();
    private final Map<Border, Double> acExchanges = new HashMap<>();
    private final ReferenceProgram referenceProgram;
    private final FinalCgmResult finalCgmResult;
    private final List<BorderDirectionRecord> borderDirectionRecords;
    private final RegionConfiguration regionConfiguration;
    private final List<VirtualHubRecord> virtualHubRecords;
    private final List<BecByBoundary> sharingKeysBEC;
    private final FlowByAreaMap acNetPositionOutCountry;
    private final Map<String, NetPositions> npByCountry;
    private final InternalBiddingZonesManager biddingZonesManager;

    public static RefProgCalculator with(final MergingTask task, final Supplier<LoadFlow.Runner> runnerSupplier) throws FileNotFoundException {
        return new RefProgCalculator(task, runnerSupplier);
    }

    private RefProgCalculator(final MergingTask task, final Supplier<LoadFlow.Runner> runnerSupplier) throws FileNotFoundException {
        this.task = task;
        final Configurations configurations = task.getConfigurations();
        referenceProgram = task.getArtifact(REFERENCE_PROGRAM_FORECAST_FILE, ReferenceProgram.class);
        finalCgmResult = task.getArtifact(CGM_NET_POSITIONS_FILE, FinalCgmResult.class);
        borderDirectionRecords = configurations.getBorderDirectionRecords();
        regionConfiguration = configurations.getRegionConfiguration();
        virtualHubRecords = configurations.getVirtualHubList();
        sharingKeysBEC = configurations.getBecMatrixConfig();
        acNetPositionOutCountry = regionConfiguration.getAreasIn().values().stream().collect(toFlowByAreaMap(identity(), this::computeAcNetPositionOutCountry));
        npByCountry = finalCgmResult.getNetPositionsResults().getNetPositionsByCountryMap();
        this.biddingZonesManager = new InternalBiddingZonesManager(task, runnerSupplier);
    }

    public PublicationDocument computeRefProg() {
        borderDirectionRecords.forEach(this::handleBorderDirection);
        final RefProgResult refProgResult = new RefProgResult(referenceProgram.getDailyTimeInterval(), acExchanges, virtualHubsExchanges);
        return FinalRefProgBuilder.buildFinalRefProgResult(refProgResult, task);
    }

    private void handleBorderDirection(final BorderDirectionRecord borderDirection) {
        final String borderFrom = mapDk1ToDk(borderDirection.getBorderFrom());
        final String borderTo = mapDk1ToDk(borderDirection.getBorderTo());
        findVirtualHub(borderFrom, borderTo).ifPresentOrElse(this::addVirtualHubExchange,
                                                             () -> handleAcExchange(borderFrom, borderTo));
    }

    private void handleAcExchange(final String borderFrom,
                                  final String borderTo) {
        if (isInternalItalianExchange(borderFrom, borderTo)) {
            addInternalItalianExchange(borderFrom, borderTo);
        } else if (isNonCoreExchange(borderFrom, borderTo)) {
            addNonCoreExchange(toEicCode(borderFrom), toEicCode(borderTo));
        } else if (isCoreExchange(borderFrom, borderTo)) {
            addCoreExchange(toEicCode(borderFrom), toEicCode(borderTo));
        } else {
            throw new CeMergingException("Border direction from " + borderFrom + " to " + borderTo + " defined in VH config is not supported");
        }
    }

    private String toEicCode(final String country) {
        final String eicCode = regionConfiguration.getAreasAll().get(country);
        if (eicCode == null) {
            throw new CeMergingException("EIC code cannot be found for country: " + country);
        }
        return eicCode;
    }

    private boolean isInternalItalianExchange(final String borderFrom,
                                              final String borderTo) {
        return isItalianBiddingZone(borderFrom) && isItalianBiddingZone(borderTo);
    }

    private boolean isNonCoreExchange(final String borderFrom,
                                      final String borderTo) {
        return regionConfiguration.getAreasOut().keySet().stream()
                .anyMatch(country -> borderFrom.equals(country) || borderTo.equals(country));
    }

    private boolean isCoreExchange(final String borderFrom,
                                   final String borderTo) {
        final Map<String, String> areasIn = regionConfiguration.getAreasIn();
        return areasIn.containsKey(borderFrom) && areasIn.containsKey(borderTo);
    }

    private void addVirtualHubExchange(final VirtualHubRecord virtualHub) {
        virtualHubsExchanges.put(new Border(virtualHub.getRelatedMaEic(), virtualHub.getEic()),
                                 getVirtualHubsExchangesFromCgm().getOrDefault(virtualHub.getNodeName(), 0.));
    }

    private void addNonCoreExchange(final String eicFrom,
                                    final String eicTo) {
        acExchanges.put(new Border(eicFrom, eicTo),
                        getExchangeFromRefProg(eicFrom, eicTo).getFlow());
    }

    private void addInternalItalianExchange(final String borderFrom,
                                            final String borderTo) {
        final ItalianBiddingZone from = ItalianBiddingZone.valueOf(borderFrom);
        final ItalianBiddingZone to = ItalianBiddingZone.valueOf(borderTo);

        acExchanges.put(new Border(from.getEic(), to.getEic()),
                        biddingZonesManager.getTotalFlow(from, to));
    }

    private void addCoreExchange(final String eicFrom,
                                 final String eicTo) {
        final Border border = new Border(eicFrom, eicTo);

        final double flow = getBexFactors(border)
                .getCoefficientByCountry()
                .stream()
                .mapToDouble(this::getBorderExchange)
                .sum();

        acExchanges.put(border, flow);
    }

    private double getBorderExchange(final BecCoefficients becCoefficients) {
        final String country = becCoefficients.getCountryCode();

        final NetPositions netPositions = npByCountry.get(country);
        final double globalWithoutVirtualHubs = netPositions.getGlobalNetPosition().getWithoutVirtualHubs();
        final double acNetPositionOutsideCountry = acNetPositionOutCountry.getOrZero(toEicCode(country));

        return becCoefficients.getCoefficient() * (globalWithoutVirtualHubs - acNetPositionOutsideCountry);
    }

    private Optional<VirtualHubRecord> findVirtualHub(final String borderFrom,
                                                      final String borderTo) {
        return virtualHubRecords.stream()
                .filter(vh -> vh.getCode().equals(borderFrom) || vh.getCode().equals(borderTo))
                .findAny();
    }

    private Map<String, Double> getVirtualHubsExchangesFromCgm() {
        return finalCgmResult.getNetPositionsResults()
                .getNetPositionsByCountryMap()
                .values()
                .stream()
                .map(NetPositions::getVirtualHubsExchanges)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(toMap(Map.Entry::getKey,
                                         Map.Entry::getValue,
                                         (a, b) -> b));
    }

    private ReferenceExchangeData getExchangeFromRefProg(final String eicFrom,
                                                         final String eicTo) {
        return referenceProgram.getReferenceExchangeDataList()
                .stream()
                .filter(exc -> exc.flowsBetween(eicFrom, eicTo))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find the exchange from " + eicFrom + " to " + eicTo + " in the reference program"));
    }

    private BecByBoundary getBexFactors(final Border border) {
        return sharingKeysBEC.stream()
                .filter(becByBoundary -> border.equals(becByBoundary.getBorder()))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find border between " + border.getOutArea() + " and " + border.getInArea() + " in Border Exchanges Sharing Keys"));
    }

    private double computeAcNetPositionOutCountry(final String countryEic) {
        return referenceProgram.getReferenceExchangeDataList()
                .stream()
                .filter(isAcExchangeWith(countryEic))
                .mapToDouble(exc -> exc.getAreaOutId().equals(countryEic) ? exc.getFlow() : -exc.getFlow())
                .sum();
    }

    private Predicate<ReferenceExchangeData> isAcExchangeWith(final String countryEic) {
        return r -> r.getAreaOutId().equals(countryEic) && isOutsideRegion(r.getAreaInId()) ||
                    r.getAreaInId().equals(countryEic) && isOutsideRegion(r.getAreaOutId());
    }

    private boolean isOutsideRegion(final String area) {
        final String regionId = regionConfiguration.getId();
        final Map<String, String> areasAll = regionConfiguration.getAreasAll();

        return !regionId.equals(area) && areasAll.containsValue(area);
    }
}
