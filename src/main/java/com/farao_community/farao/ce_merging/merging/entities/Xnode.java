package com.farao_community.farao.ce_merging.merging.entities;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
public class Xnode implements Serializable {

    private String name;
    private String area1;
    private String area2;
    private String subarea1;
    private String subarea2;
}
