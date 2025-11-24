package com.bim.seif.controllers;

import com.bim.seif.dto.NotificacionDto;
import com.bim.seif.models.TipoNotificacion;
import com.bim.seif.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerNotificacion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable TipoNotificacion id) {
        try {
            log.info("Usuario {} solicitó notificación {}",
                    jwt != null ? jwt.getSubject() : "desconocido", id);
            NotificacionDto dto = notificacionService.obtenerNotificacion(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error en /notificacion/{}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener la notificación", "detalle", e.getMessage()));
        }
    }
}
