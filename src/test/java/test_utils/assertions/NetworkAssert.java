/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package test_utils.assertions;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.Map;

public class NetworkAssert extends AbstractAssert<NetworkAssert, Network> {

    protected NetworkAssert(final Network network) {
        super(network, NetworkAssert.class);
    }

    public static NetworkAssert assertThat(final Network network) {
        return new NetworkAssert(network);
    }

    @CanIgnoreReturnValue
    public NetworkAssert hasLine(final String lineId) {
        return hasLineWithProperties(lineId, Map.of());
    }

    @CanIgnoreReturnValue
    public NetworkAssert hasLineWithProperties(final String lineId,
                                               final Map<String, String> properties) {
        final Line line = actual.getLine(lineId);
        if (line == null) {
            failWithMessage("line %s doesn't exist in network %s".formatted(lineId, actual.getId()));
        } else {
            properties.forEach((key, expectedValue) -> {
                final String linePropValue = line.getProperty(key);
                if (linePropValue == null) {
                    failWithMessage("line %s doesn't have property %s".formatted(lineId, key));
                } else if (!linePropValue.equals(expectedValue)) {
                    failWithMessage("line %s has %s:%s, but expected %s".formatted(lineId, key, linePropValue, expectedValue));
                }
            });
        }
        return this;
    }

    @CanIgnoreReturnValue
    public NetworkAssert hasBus(final String busId) {
        if (actual.getBusBreakerView().getBus(busId) == null) {
            failWithMessage("bus %s doesn't exist in network %s".formatted(busId, actual.getId()));
        }
        return this;
    }

    @CanIgnoreReturnValue
    public NetworkAssert doesNotHaveLine(final String lineId) {
        if (actual.getLine(lineId) != null) {
            failWithMessage("line %s exists in network %s".formatted(lineId, actual.getId()));
        }
        return this;
    }

    @CanIgnoreReturnValue
    public NetworkAssert doesNotHaveBus(final String busId) {
        if (actual.getBusBreakerView().getBus(busId) != null) {
            failWithMessage("bus %s exists in network %s".formatted(busId, actual.getId()));
        }
        return this;
    }

}
