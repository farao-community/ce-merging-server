/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies;

import com.farao_community.farao.ce_merging.merging.process.xnode.AreaInformation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeInformation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus;

import java.util.List;

import static com.farao_community.farao.ce_merging.merging.process.xnode.AreaInformation.empty;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.CLOSE;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.OPEN;
import static java.util.Optional.ofNullable;

public class XnodeIncorrect {
    private String name;
    private String country1;
    private XnodeStatus status1;
    private boolean recessive1;
    private String country2;
    private XnodeStatus status2;
    private boolean recessive2;
    private XnodeStatus finalStatus;

    public XnodeIncorrect() {
    }

    public static XnodeIncorrect buildFrom(final String name,
                                           final XnodeInformation info,
                                           final List<String> recessiveCountries) {
        return new XnodeIncorrect(name,
                                  ofNullable(info.getArea1Information()).orElse(empty()),
                                  ofNullable(info.getArea2Information()).orElse(empty()),
                                  recessiveCountries
                                  );
    }

    public XnodeIncorrect(final String name,
                          final AreaInformation infos1,
                          final AreaInformation infos2,
                          final List<String> recessiveCountries) {
        this.name = name;
        this.country1 = infos1.getCountry();
        this.status1 = infos1.getStatus();
        this.recessive1 = recessiveCountries.contains(infos1.getCountry());
        this.country2 = infos2.getCountry();
        this.status2 = infos2.getStatus();
        this.recessive2 = recessiveCountries.contains(infos2.getCountry());
        this.finalStatus = computeFinalStatus();
    }

    private XnodeStatus computeFinalStatus() {
        if (recessive1 == recessive2) {
            return OPEN.equals(status1) || OPEN.equals(status2) ? OPEN : CLOSE;
        } else if (!recessive1) {
            return status1;
        } else {
            return status2;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCountry1() {
        return country1;
    }

    public void setCountry1(final String country1) {
        this.country1 = country1;
    }

    public XnodeStatus getStatus1() {
        return status1;
    }

    public void setStatus1(final XnodeStatus status1) {
        this.status1 = status1;
    }

    public String getCountry2() {
        return country2;
    }

    public void setCountry2(final String country2) {
        this.country2 = country2;
    }

    public XnodeStatus getStatus2() {
        return status2;
    }

    public void setStatus2(final XnodeStatus status2) {
        this.status2 = status2;
    }

    public XnodeStatus getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(final XnodeStatus finalStatus) {
        this.finalStatus = finalStatus;
    }

    public boolean isRecessive1() {
        return recessive1;
    }

    public void setRecessive1(final boolean recessive1) {
        this.recessive1 = recessive1;
    }

    public boolean isRecessive2() {
        return recessive2;
    }

    public void setRecessive2(final boolean recessive2) {
        this.recessive2 = recessive2;
    }
}
