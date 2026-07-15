/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pst_special_process;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.iidm.network.Country.AT;
import static com.powsybl.iidm.network.Country.IT;
import static com.powsybl.iidm.network.Country.SI;
import static java.util.function.Function.identity;

public enum SpecialPst {
    PADRICIANO("Padriciano", "IPDRV12[0-9A-Z] IPDRV12[0-9A-Z] 1", IT),
    DIVACA("Divača", "LDIVAC1[0-9A-Z] LDIVAC1[0-9A-Z] [1-2]", SI),
    LIENZ("Lienz", "OLIENN2[0-9A-Z] OLIENN2[0-9A-Z] [2]", AT),
    NRPST21("Nauders 1", "ONAUDE2[0-9A-Z] ONAUDE2[0-9A-Z] 1", AT),
    NRPST22("Nauders 2", "ONAUDE2[0-9A-Z] ONAUDE2[0-9A-Z] 2", AT);

    private final String fullName;
    private final String idRegex;
    private final Country country;

    SpecialPst(final String fullName, final String idRegex, final Country country) {
        this.fullName = fullName;
        this.idRegex = idRegex;
        this.country = country;
    }

    public String getFullName() {
        return fullName;
    }

    public String getIdRegex() {
        return idRegex;
    }

    public Country getCountry() {
        return country;
    }

    public boolean matches(final Identifiable<?> identifiable) {
        return Pattern.compile(idRegex).matcher(identifiable.getId()).matches();
    }

    public static Stream<SpecialPst> stream() {
        return Arrays.stream(SpecialPst.values());
    }

    public static <T> Map<SpecialPst, T> toPstMap(final Function<SpecialPst, T> valueMapper) {
        return Arrays.stream(SpecialPst.values()).collect(Collectors.toMap(identity(), valueMapper));
    }

    public static void forAllSpecialPst(final Consumer<SpecialPst> action) {
        Arrays.stream(SpecialPst.values()).forEach(action);
    }
}
