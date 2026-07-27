package com.farao_community.farao.ce_merging.merging.task.enums;

import java.util.Arrays;

public enum GermanTso {
    D2,
    D4,
    D6,
    D7,
    D8;

    public static boolean includes(final String tsoCode) {
        return Arrays.stream(values()).map(GermanTso::name).anyMatch(tsoCode::equals);
    }
}
