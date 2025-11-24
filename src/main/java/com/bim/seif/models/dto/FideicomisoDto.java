package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FideicomisoDto implements Serializable {
    private final String folio;
    private final String alias;
    private final boolean bloqueado;

}