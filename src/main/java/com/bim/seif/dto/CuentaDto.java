package com.bim.seif.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CuentaDto implements Serializable {
    private final String cuenta;
//    private final EstadoCuenta estado;
    private final EntidadFinancieraDto entidadFinanciera;
    private final FideicomisoDto fideicomiso;
    private final DivisaDto divisa;
}
