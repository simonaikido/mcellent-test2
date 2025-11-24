package com.bim.seif.services;

import com.bim.seif.clients.FideicomisoClient;
import com.bim.seif.dto.FideicomisoDto;
import com.bim.seif.models.Fideicomiso;
import com.bim.seif.repositories.FideicomisoRepository;
import com.bim.seif.repositories.RegionRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FideicomisoService {

    @Value("${gestor.api.url}")
    private String RUTA_CARGAR_FIDEICOMISO;

    private final FideicomisoRepository fideicomisoRepository;

    private final FideicomisoClient fideicomisoClient;

    // WebClient.Builder webClientBuilder,
    public FideicomisoService(FideicomisoRepository fideicomisoRepository, FideicomisoClient fideicomisoClient,
            RegionRepository regionRepository) {
        this.fideicomisoRepository = fideicomisoRepository;
        this.fideicomisoClient = fideicomisoClient;
        new RestTemplate();
        log.info("Fideicomiso inicializado. URL gestor API: {}", RUTA_CARGAR_FIDEICOMISO);
    }

    /**
     * 
     * @param cliente
     * @param folioFideicomiso
     * @return Optional<FideicomisoDto> con el fideicomiso encontrado
     * @throws Exception
     */
    public Optional<FideicomisoDto> obtenerFideicomisoPorFolio(String cliente, String folioFideicomiso)
            throws Exception {
        log.info("Buscando fideicomiso {} para el cliente {}.", folioFideicomiso, cliente);
        try {
            List<FideicomisoDto> listaFideicomiso = obtenerFideicomisos(cliente);
            log.debug("Recuperados {} fideicomisos del cliente {} para filtrar.", listaFideicomiso.size(), cliente);
            Optional<FideicomisoDto> fideicomisoEncontrado = listaFideicomiso.stream()
                    .filter(f -> f.getFolio().equals(folioFideicomiso))
                    .findFirst();
            if (fideicomisoEncontrado.isPresent()) {
                log.info("Fideicomiso {} encontrado.", folioFideicomiso);
            } else {
                log.warn("Fideicomiso {} No encontrado en la lista del cliente {}.", folioFideicomiso, cliente);
            }
            return fideicomisoEncontrado;
        } catch (Exception e) {
            log.error("Fallo al obtener la lista de fideicomisos para el cliente {} durante la busqueda por folio",
                    cliente, e);
            throw e;
        }
    }

    /**
     * 
     * @param clienteEmail
     * @return Lista de FideicomisoDto
     */

    public List<FideicomisoDto> obtenerFideicomisos(String clienteEmail) {
        log.info("Consultando listas de fideicomisos al servicio externo para cliente: {}", clienteEmail);
        try {
            List<FideicomisoDto> fideicomisos = fideicomisoClient.obtenerFideicomisos(clienteEmail);
            log.info("servicio externo retorno {} fideicomisos para {}.", fideicomisos.size(), clienteEmail);
            return fideicomisos;
        } catch (Exception e) {
            log.error("Fallo en la comunicacion con el servicio externo para {}.", clienteEmail, e);
            throw e;
        }

    }

    /**
     * 
     * @param fideicomisos
     */
    public void actualizarFideicomisos(List<Fideicomiso> fideicomisos) {
        log.info("iniciando actualizacion/guardado de {} fideicomisos en la base de datos local", fideicomisos.size());
        if (fideicomisos.isEmpty()) {
            log.warn("DB: La lista de fideicomisos a actualizar esta vacia. Operacion omitida ");
            return;
        }
        try {
            fideicomisoRepository.saveAllAndFlush(fideicomisos);
            log.info("Actualizacion/guardado de {} fideicomisos completado exitosamente", fideicomisos.size());
        } catch (Exception e) {
            log.error("fallo al guardar/actualizar la lista de fideicomisos en la basde de datos local", e);
            throw e;
        }
    }
}
