/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.process.glsk_fix.model.GlskSerieShare;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.QuantityType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GlskBlockRedispatcherTest {

    public static final String NODE_1 = "NODE1";
    public static final String GSK_001 = "GSK001";

    @Test
    void shouldThrowExceptionWhenCorrectSumIsZero() {
        final ManualGSKBlockType block = createManualBlock(GSK_001);
        block.getManualNodes().add(createManualNode());

        final Map<String, List<GlskSerieShare>> incorrectValues = new HashMap<>();
        incorrectValues.put(GSK_001, List.of(new GlskSerieShare(NODE_1, 10)));
        final Map<String, List<GlskSerieShare>> correctValues = new HashMap<>();
        correctValues.put(GSK_001, List.of(new GlskSerieShare(NODE_1, 0)));

        assertThrows(CeMergingException.class,
                () -> GlskBlockRedispatcher.redispatchFactorValue(
                        incorrectValues,
                        correctValues,
                        block
                )
        );
    }

    private ManualGSKBlockType createManualBlock(final String name, final ManualNodesType... nodes) {
        final ManualGSKBlockType block = new ManualGSKBlockType();
        final IdentificationType identification = new IdentificationType();
        identification.setV(name);
        block.setGSKName(identification);
        block.getManualNodes().addAll(Arrays.asList(nodes));
        return block;
    }

    private ManualNodesType createManualNode() {
        final ManualNodesType node = new ManualNodesType();
        final IdentificationType nodeId = new IdentificationType();
        nodeId.setV(GlskBlockRedispatcherTest.NODE_1);
        node.setNodeName(nodeId);
        final QuantityType factor = new QuantityType();
        factor.setV(BigDecimal.valueOf(1.0));
        node.setFactor(factor);
        return node;
    }
}
