package test_utils.assertions;

import com.powsybl.ucte.network.UcteCountryCode;
import com.powsybl.ucte.network.UcteNodeCode;
import com.powsybl.ucte.network.UcteVoltageLevelCode;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.Objects;

public class UcteNodeCodeAssert extends AbstractAssert<UcteNodeCodeAssert, UcteNodeCode> {
    protected UcteNodeCodeAssert(final UcteNodeCode ucteNodeCode) {
        super(ucteNodeCode, UcteNodeCodeAssert.class);
    }

    public static UcteNodeCodeAssert assertThat(final UcteNodeCode ucteNodeCode) {
        return new UcteNodeCodeAssert(ucteNodeCode);
    }

    @CanIgnoreReturnValue
    public UcteNodeCodeAssert isLocatedIn(final UcteCountryCode country,
                                          final String geographicalSpot) {
        if (actual.getUcteCountryCode() != country) {
            failWithActualExpectedAndMessage(actual.getUcteCountryCode(), country, "Unexpected UCTE country code");
        }
        if (!Objects.equals(actual.getGeographicalSpot(), geographicalSpot)) {
            failWithActualExpectedAndMessage(actual.getGeographicalSpot(), geographicalSpot, "Unexpected spot");
        }
        return this;
    }

    @CanIgnoreReturnValue
    public UcteNodeCodeAssert isBusBar(final UcteVoltageLevelCode voltageLevel,
                                       final char busbar) {
        if (actual.getVoltageLevelCode() != voltageLevel) {
            failWithActualExpectedAndMessage(actual.getVoltageLevelCode(), voltageLevel, "Unexpected voltage level");
        }
        if (actual.getBusbar() != busbar) {
            failWithActualExpectedAndMessage(actual.getBusbar(), busbar, "Unexpected bus bar");
        }
        return this;
    }

}
