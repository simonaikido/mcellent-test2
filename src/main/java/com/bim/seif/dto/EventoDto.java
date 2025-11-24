package com.bim.seif.dto;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class EventoDto {

    private String cve;
    private String asunto;
    private String cuerpoCorreo;
    private String rutaFirma;
    private String modificadoPor;
    private String comentario;
    private boolean desactivado;
    private LocalDateTime fechaModificacion;
    private String nombre;
}
