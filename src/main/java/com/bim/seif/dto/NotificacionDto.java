package com.bim.seif.dto;

import com.bim.seif.models.TipoNotificacion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NotificacionDto {

    private TipoNotificacion cve;
    private String nombre;
    private String titulo;
    private String mensaje;
    private boolean desactivada;
}
