/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class DummyJson {
    String stringValue;
    Boolean boolValue;
    Integer numValue;
    List<String> listValue;

    public DummyJson(@JsonProperty("stringValue") final String stringValue,
                     @JsonProperty("boolValue") final Boolean boolValue,
                     @JsonProperty("numValue") final Integer numValue,
                     @JsonProperty("listValue") final List<String> listValue) {
        this.stringValue = stringValue;
        this.boolValue = boolValue;
        this.numValue = numValue;
        this.listValue = listValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof final DummyJson dummyJson)) {
            return false;
        }
        return Objects.equals(stringValue, dummyJson.stringValue) && Objects.equals(boolValue, dummyJson.boolValue) && Objects.equals(numValue, dummyJson.numValue) && Objects.equals(listValue, dummyJson.listValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringValue, boolValue, numValue, listValue);
    }

    public Integer getNumValue() {
        return numValue;
    }

    public void setNumValue(final Integer numValue) {
        this.numValue = numValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(final String stringValue) {
        this.stringValue = stringValue;
    }

    public Boolean getBoolValue() {
        return boolValue;
    }

    public void setBoolValue(final Boolean boolValue) {
        this.boolValue = boolValue;
    }

    public List<String> getListValue() {
        return listValue;
    }

    public void setListValue(final List<String> listValue) {
        this.listValue = listValue;
    }
}
