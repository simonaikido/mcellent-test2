package com.bim.seif.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RegionDto implements Serializable {
    private final String cve;
    private final String descripcion;
}
