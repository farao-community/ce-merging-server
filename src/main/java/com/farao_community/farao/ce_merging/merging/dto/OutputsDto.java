/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@JsonInclude(NON_NULL)
@Data
public class OutputsDto implements Serializable {
    private String refProgLocation;
    private String cgmLocation;
    private String bciReportLocation;
}

