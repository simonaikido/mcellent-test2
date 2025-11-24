package com.bim.seif.models.dto;

import lombok.Data;

import java.util.Date;

@Data
public class SolicitudArchivoJuridicaDto {

    private Long id;
    private Long idOperacion;
    private Date fechaSolicitud;
    private Date fechaCarga;
    private String nombreArchivo;
    private String rutaArchivo;
    private String descripcionDelActo;
    private String nombreFormato;
    private String rutaFormato;
    private String nota;

}
