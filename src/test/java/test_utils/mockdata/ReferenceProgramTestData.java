/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils.mockdata;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceExchangeData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;

import java.util.ArrayList;
import java.util.List;

public final class ReferenceProgramTestData {

    private ReferenceProgramTestData() {
        throw new AssertionError("No default constructor in utility class");
    }

    public static ReferenceProgram createReferenceProgram() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", 1500.));
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "NINE", -1000.));
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "ELEVEN", 900.));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", -800.));
        referenceExchangeDataList.add(new ReferenceExchangeData("ZERO", "CE", 300.));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", -1800.));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", 800.));

        return new ReferenceProgram(referenceExchangeDataList);
    }

    public static ReferenceProgram createReferenceProgramCase1() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", 1000.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", -1800.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", 1000.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", -200.0));
        return new ReferenceProgram(referenceExchangeDataList);
    }

    public static ReferenceProgram createReferenceProgramCase2() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", 500.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", 500.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", -500.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", -500.0));

        return new ReferenceProgram(referenceExchangeDataList);
    }

    public static ReferenceProgram createReferenceProgramCase3() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", -1000.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", 0.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", 1000.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", 0.0));

        return new ReferenceProgram(referenceExchangeDataList);
    }

    public static ReferenceProgram createReferenceProgramCase4() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", -1500.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", 0.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", 1000.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", 500.0));

        return new ReferenceProgram(referenceExchangeDataList);
    }

    public static ReferenceProgram createReferenceProgramCase5() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", -3000.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", -2200.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", 1500.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", 3700.0));

        return new ReferenceProgram(referenceExchangeDataList);
    }

    public static ReferenceProgram createReferenceProgramCase6() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", 400.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", -400.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", 0.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", 0.0));

        return new ReferenceProgram(referenceExchangeDataList);
    }

    public static ReferenceProgram createReferenceProgramCase7() {
        List<ReferenceExchangeData> referenceExchangeDataList = new ArrayList<>();
        referenceExchangeDataList.add(new ReferenceExchangeData("ONE", "CE", 800.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWELVE", "CE", 200.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("EIGHT", "CE", 200.0));
        referenceExchangeDataList.add(new ReferenceExchangeData("TWENTYFIVE", "CE", -1200.0));

        return new ReferenceProgram(referenceExchangeDataList);
    }
}
