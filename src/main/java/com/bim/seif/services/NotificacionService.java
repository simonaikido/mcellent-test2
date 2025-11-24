package com.bim.seif.services;

import com.bim.seif.dto.NotificacionDto;
import com.bim.seif.models.Notificacion;
import com.bim.seif.models.TipoNotificacion;
import com.bim.seif.repositories.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionDto obtenerNotificacion(TipoNotificacion id) {
        try {
            Notificacion notificacion = notificacionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("No se encontró la notificación con id: " + id));

            NotificacionDto dto = new NotificacionDto();
            dto.setTitulo(notificacion.getTitulo());
            dto.setMensaje(notificacion.getMensaje());
            dto.setDesactivada(notificacion.isDesactivada());

            return dto;

        } catch (Exception e) {
            // Log detallado del error
            log.error("Error al obtener la notificación con id {}: {}", id, e.getMessage(), e);

            // Puedes devolver un objeto vacío o relanzar la excepción
            throw new RuntimeException("Error al obtener la notificación: " + e.getMessage(), e);
        }
    }
}