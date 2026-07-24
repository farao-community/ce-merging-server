/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies;

import org.junit.jupiter.api.Test;
import test_utils.GetterSetterVerifier;

class XnodeInconsistenciesTest {

    @Test
    void entitiesShouldHaveStandardGettersAndSetters() {
        GetterSetterVerifier.forClass(XnodesInconsistencies.class).verify();
        GetterSetterVerifier.forClass(XnodeIncomplete.class).verify();
        GetterSetterVerifier.forClass(XnodeIncorrect.class).verify();
    }

}
