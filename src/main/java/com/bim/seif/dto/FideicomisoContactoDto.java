package com.bim.seif.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FideicomisoContactoDto implements Serializable {
//    private final String folio;
    private final boolean bloqueado;
    private final String nombreCliente;
    private final String telefono;
    private final String email;
}
