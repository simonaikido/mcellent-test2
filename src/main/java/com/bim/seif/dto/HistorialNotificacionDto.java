package com.bim.seif.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HistorialNotificacionDto {
    private Long id;
    private String nombre;
    private String titulo;
    private String mensaje;
    private String modificadoPor;
    private String comentario;
    private boolean desactivada;
    private LocalDateTime fechaModificacion;
}
