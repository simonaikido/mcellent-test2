package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InstruccionDto implements Serializable {
    private String folio;
    private FideicomisoDto fideicomiso;
    private String rutaArchivo;
    private String comentario;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaAtencion;
    private String clienteCarga;
    private LocalDateTime fechaRechazo;}
