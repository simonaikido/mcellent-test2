package com.bim.seif.services;

import com.bim.seif.dto.EventoDto;
import com.bim.seif.models.Evento;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.repositories.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;

    /**
     * @param id El identificador del tipo de evento a buscar.
     * @return EventoDto con el asunto y cuerpo del mensaje.
     * @throws NoSuchElementException si el evento no existe en la base de datos.
     * @throws IllegalStateException  si el evento existe pero está marcado como
     */
    public EventoDto obtenerEvento(TipoEvento id) {
        log.info("Solicitud para obtener evento con ID: {}", id);

        // 1. Buscar el evento por ID
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ERROR: No se encontro la definicion del evento con ID {}.", id);
                    return new NoSuchElementException("No existe evento: " + id);
                });

        log.debug("Evento encontrado: Asunto='{}'", evento.getAsunto());

        // 2. Validar si el evento está desactivado
        if (evento.isDesactivado()) {
            log.warn("El evento {} esta marcado como desactivado y no puede ser utilizado.", id);
            throw new IllegalStateException("El evento " + id + " está desactivado");
        }

        // 3. Mapear la entidad al DTO
        EventoDto dto = new EventoDto();
        dto.setAsunto(evento.getAsunto());
        dto.setCuerpoCorreo(evento.getCuerpoCorreo());
        dto.setRutaFirma(evento.getRutaFirma());

        log.info("Evento con ID {} obtenido y validado exitosamente.", id);

        return dto;
    }
}
