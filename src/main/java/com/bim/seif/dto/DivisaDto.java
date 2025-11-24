package com.bim.seif.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DivisaDto implements Serializable {
//    private final String cve;
    private final String abreviatura;
    private final String descripcion;
    private final boolean monedaNacional;
}
