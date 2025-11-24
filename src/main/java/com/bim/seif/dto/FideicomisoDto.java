package com.bim.seif.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FideicomisoDto implements Serializable {
    private final String folio;
    private final String alias;
    private final RegionDto region;
    private final FideicomisoContactoDto fideicomisoContacto;
    private final EstadoFideicomisoDto estado;
}
