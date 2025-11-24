package com.bim.seif.services;

import com.bim.seif.dto.InstruccionResponse;
import com.bim.seif.dto.ProgramacionResponse;
import com.bim.seif.models.Instruccion;
import com.bim.seif.models.TipoOperacionJuridica;
import com.bim.seif.models.dto.InstruccionJuridicaDto;
import com.bim.seif.models.dto.OperacionJuridicaDto;
import com.bim.seif.models.dto.SolicitudArchivoJuridicaDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bim.seif.repositories.InstruccionRepository;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InstruccionService {

    private final InstruccionRepository instruccionRepository;
    private final FileServiceSSH fileServiceSSH;

    public InstruccionService(InstruccionRepository instruccionRepository, FileServiceSSH fileServiceSSH) {
        this.instruccionRepository = instruccionRepository;
        this.fileServiceSSH = fileServiceSSH;
        log.info("InstruccionService inicializado con InstruccionesRepository");
    }

    public List<Instruccion> obtenerInstruccionesPorCliente(String folio) {
        log.debug("Buscando Instrucciones para el cliente con folio: {}.", Instruccion.size(), folio);
        List<Instruccion> instrucciones = instruccionRepository.findAllByClienteCarga(folio);
        log.info("Encontradas {} intrucciones para el cliente {}.", instrucciones.size(), folio);
        return instrucciones;
    }

    public Integer obtenerTotalInstruccionesPorFideicomiso(String folioFideicomiso) {
        log.debug("Contando instrucciones para el fideicomiso: {}", folioFideicomiso);
        Integer total = instruccionRepository.countByFideicomisoFolio(folioFideicomiso);
        log.info("Total de instrucciones para {}: {}", folioFideicomiso, total);
        return total;
    }

    public Instruccion guardarInstruccion(Instruccion instruccion) {
        log.info("Guardando nueva instrucción para el folio: {}", instruccion.getFolio());
        try {
            Instruccion instruccionGuardada = instruccionRepository.save(instruccion);
            log.info("Instruccion {} guardada exitosamente.", instruccionGuardada.getFolio());
            return instruccionGuardada;
        } catch (Exception e) {
            log.error("Fallo al guardar la instrucción con folio {}.", instruccion.getFolio(), e);
            throw e;
        }
    }

    public List<Instruccion> guardarInstrucciones(List<Instruccion> instrucciones) {
        log.info("Guardando lista de {} instrucciones", instrucciones.size());
        try {
            List<Instruccion> instruccionesGuardadas = instruccionRepository.saveAll(instrucciones);
            log.info("Lista de instrucciones guardada exitosamente. Total {}", instruccionesGuardadas.size());
            return instruccionesGuardadas;
        } catch (Exception e) {
            log.error("Fallo al guardar lista de instrucciones", e);
            throw e;
        }
    }

    public List<InstruccionResponse> getInstruccionesCompletas(String clienteEmail) {
        log.info("Iniciando busqueda de instrucciones completas para el cliente: {}", clienteEmail);
        try {
            List<Object[]> results = instruccionRepository.findInstruccionesCompletasByCliente(clienteEmail);
            log.debug("Consulta a repositorio para {} devolvio {} resultados.", clienteEmail, results.size());

            if (results.isEmpty()) {
                log.info("No se encontraron instrucciones completas para el cliente {}.", clienteEmail);
                return Collections.emptyList();
            }

            List<InstruccionResponse> responseList = results.stream()
                    .map(this::mapToInstruccionResponse)
                    .collect(Collectors.toList());

            log.info("Mapeados {} resultados a InstruccionResponse para {}.", responseList.size(), clienteEmail);
            return responseList;
        } catch (Exception e) {
            log.error("Fallo al obtener instrucciones completas para {}.", clienteEmail, e);
            throw e;
        }
    }

    private InstruccionResponse mapToInstruccionResponse(Object[] result) {
        if (result == null || result.length < 18) {
            log.error("Arreglo de resultados de la DB incompleto. Esperado >= 18, Recibido: {}",
                    (result == null ? null : result.length));
            return null;
        }
        log.debug("Mapeando resultado de DB: Folio Instruccion={}", result[6]);

        InstruccionResponse resp = new InstruccionResponse(
                result[0] != null ? (String) result[0] : null, // rutaArchivo
                result[1] != null ? (String) result[1] : null, // folioMonetaria
                result[2] != null ? (String) result[2] : null, // fideicomisoFolio
                result[3] != null ? (String) result[3] : null, // fideicomisoAlias
                result[4] != null ? (String) result[4] : null, // fechaAprobacion
                result[5] != null ? (String) result[5] : null, // fechaAlta
                result[6] != null ? (String) result[6] : null, // folioInstruccion
                result[7] != null ? (String) result[7] : null, // fechaCancelacion
                result[8] != null ? (String) result[8] : null, // status
                result[9] != null ? (String) result[9] : null, // clienteCarga
                result[10] != null ? (String) result[10] : null, // documentos
                result[11] != null ? (String) result[11] : null, // fechasDocumentos
                result[12] != null ? (String) result[12] : null, // comentarios
                result[13] != null ? (String) result[13] : null, // comentariosFecha
                result[14] != null ? ((Integer) result[14]) > 0 : null, // programada (INT -> Boolean)
                result[15] != null ? (String) result[15] : null, // rutaEdoCta
                result[16] != null ? (String) result[16] : null, // tipoInstruccionTxt
                result[17] != null ? (Integer) result[17] : null // isMonetaria
        );

        // Si la consulta ya trae la 19ª columna (fechaRechazo), la asignamos por setter
        if (result.length >= 19) {
            resp.setFechaRechazo(result[18] != null ? (String) result[18] : null);
        }

        return resp;
    }

    // Método modificado para devolver una lista de ProgramacionResponse
    public List<ProgramacionResponse> findFechasInstruccionProgramada(String folioInstruccion) {
        log.info("Buscando fechas programadas para folio de instruccion: {}", folioInstruccion);
        try {
            List<Object[]> results = instruccionRepository.findFechasInstruccionProgramada(folioInstruccion);
            log.debug("Consulta de fechas programadas para {} devolvio {} resultados.", folioInstruccion,
                    results.size());
            if (results.isEmpty()) {
                log.info("No se encontraron fechas programadas para el folio {}.", folioInstruccion);
                return Collections.emptyList();
            }
            return results.stream()
                    .map(this::mapToProgramacionResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Fallo al buscar fechas programadas para {}.", folioInstruccion, e);
            throw e;
        }
    }

    // Nuevo método de mapeo para ProgramacionResponse
    private ProgramacionResponse mapToProgramacionResponse(Object[] result) {
        // Asegúrate de que los índices y tipos de datos coinciden con tu consulta SQL
        // y con la definición de tu ProgramacionResponse

        return new ProgramacionResponse(
                result[0] != null ? result[0].toString() : null, // idProgramacion
                result[1] != null ? result[1].toString() : null, // instruccionProgramadaFolio (asumiendo que es
                                                                 // numérico en la DB)
                result[2] != null ? (Integer) result[2] : null, // algunaOtraPropiedad (ajusta el tipo si es diferente)
                result[3] != null ? (String) result[3] : null // fechaProgramada
        );
    }

    public List<InstruccionJuridicaDto> findInstruccionesConSolicitudDeArchivos(String user)
            throws InterruptedException {
        log.info("Buscando instrucciones juridicas con solicitud de archivos para el usuario: {}", user);
        try {
            Optional<List<Map<String, Object>>> findJuridicas = instruccionRepository
                    .findBuscarInstruccionesSolicitudes(user);
            if (findJuridicas.isPresent()) {
                List<InstruccionJuridicaDto> juridicas = mapInstruccionJuridicaDto(findJuridicas);
                log.info("Encontradas {} instrucciones juridicas con solicitud de archivos para {}", juridicas.size(),
                        user);
                return juridicas;
            }
            log.info("No se encontraron instrucciones juridicas pendientes para el usuario {} ", user);
            return java.util.Collections.emptyList(); // ← en vez de null
        } catch (Exception e) {
            log.error("Fallo al buscar instrucciones juridicas para {}.", user, e);
            throw new InterruptedException("Error de servicio al buscar instrucciones juridicas.");
        }
    }

    // temp solicitud archivo
    public List<OperacionJuridicaDto> findOperacionesConSolicitudDeArchivos(String folioInstruccion)
            throws InterruptedException {
        log.info("Buscando operaciones juridicas con solicitud de archivos para folio: {}", folioInstruccion);
        List<OperacionJuridicaDto> operaciones = new ArrayList<>();

        try {
            Optional<List<Map<String, Object>>> operacionesFind = instruccionRepository
                    .obtenerOperacionesJuridicas(folioInstruccion);

            if (operacionesFind.isPresent() && !operacionesFind.get().isEmpty()) {
                operaciones = mapOperacionJuridica(operacionesFind.get());
                log.debug("Mapeadas {} operaciones juridicas para el folio {}.", operaciones.size(), folioInstruccion);

                // Por cada operación, buscar sus solicitudes de archivo
                for (OperacionJuridicaDto operacion : operaciones) {
                    log.debug("Buscando solicitudes de archivos para la Operacion ID: {}", operacion.getId());
                    Optional<List<Map<String, Object>>> solicitudesArchivosFind = instruccionRepository
                            .obtenerArchivosSolicitadosPorOperacion(operacion.getId());

                    if (solicitudesArchivosFind.isPresent() && !solicitudesArchivosFind.get().isEmpty()) {
                        List<SolicitudArchivoJuridicaDto> solicitudesArchivos = mapSolicitudesArchivos(
                                solicitudesArchivosFind.get());
                        operacion.setSolicitudArchivoJuridica(solicitudesArchivos);
                        log.debug("Aniadidas {} solicitudes de archivos a la Operacion ID: {}",
                                solicitudesArchivos.size(), operacion.getId());
                    } else {
                        log.debug("No se encontraron solicitudes de archivos para la Operacion ID: {}",
                                operacion.getId());
                    }
                } // for loop

                log.info(
                        "Proceso completado. Retornando {} Operaciones Juridicas con sus solicitudes de archivo para {}.",
                        operaciones.size(), folioInstruccion);
            } else {
                log.info("No se encontraron operaciones juridicas para el folio {}.", folioInstruccion);
            }

            return operaciones;

        } catch (Exception e) {
            log.error("Fallo al buscar operaciones juridicas para {}.", folioInstruccion, e);
            throw new InterruptedException("Error de servicio al buscar operaciones juridicas.");
        }
    }

    private List<InstruccionJuridicaDto> mapInstruccionJuridicaDto(Optional<List<Map<String, Object>>> findJuridicas) {
        List<InstruccionJuridicaDto> instrucciones = new ArrayList<>();

        if (findJuridicas.isPresent()) {
            List<Map<String, Object>> getJuridicas = findJuridicas.get();
            log.debug("Iniciando mapeo de {} resultados de Instrucción Juridica.", getJuridicas.size());

            for (Map<String, Object> instruccionJuridicaMap : getJuridicas) {
                InstruccionJuridicaDto InstruccionJuridica = new InstruccionJuridicaDto();

                InstruccionJuridica.setFolio((String) instruccionJuridicaMap.get("folio"));
                InstruccionJuridica.setEstatusCve("PE"); // se mantiene fijo como lo tienes
                InstruccionJuridica.setResponsable((String) instruccionJuridicaMap.get("responsable"));

                // solicitudCorreccion (soporta Boolean/Number/String)
                Object solicitudCorreccionObj = instruccionJuridicaMap.get("solicitudCorreccion");
                InstruccionJuridica.setSolicitudCorreccion(toBooleanSafe(solicitudCorreccionObj));

                // fecha_alta con casteo tolerante
                Object fechaAltaObj = instruccionJuridicaMap.get("fecha_alta");
                InstruccionJuridica.setFechaHoraAlta(toTimestampSafe(fechaAltaObj));

                InstruccionJuridica.setAliasFideicomiso((String) instruccionJuridicaMap.get("alias"));
                InstruccionJuridica.setTipoInstruccion("No monetaria"); // lo dejas fijo
                InstruccionJuridica.setRutaArchivo((String) instruccionJuridicaMap.get("ruta_archivo"));
                InstruccionJuridica.setComentariosInstruccion((String) instruccionJuridicaMap.get("comentario"));

                instrucciones.add(InstruccionJuridica);
            }

            log.debug("Mapeo de Instruccion Juridica completado.");
        }

        return instrucciones; // ← nunca null
    }

    /* ===== Helpers mínimos y seguros, sin cambiar tu estructura ===== */

    private boolean toBooleanSafe(Object o) {
        if (o == null)
            return false;
        if (o instanceof Boolean)
            return (Boolean) o;
        if (o instanceof Number)
            return ((Number) o).intValue() != 0; // BigInteger/Integer/Long
        String s = String.valueOf(o).trim();
        if ("1".equals(s))
            return true;
        if ("0".equals(s))
            return false;
        return Boolean.parseBoolean(s); // "true"/"false"/"t"/"f"
    }

    private java.sql.Timestamp toTimestampSafe(Object o) {
        if (o == null)
            return null;
        if (o instanceof java.sql.Timestamp)
            return (java.sql.Timestamp) o;
        if (o instanceof java.time.LocalDateTime) {
            return java.sql.Timestamp.valueOf((java.time.LocalDateTime) o);
        }
        if (o instanceof java.time.OffsetDateTime) {
            return java.sql.Timestamp.from(((java.time.OffsetDateTime) o).toInstant());
        }
        if (o instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) o).getTime());
        }
        try {
            // último recurso: intentar parsear ISO LocalDateTime
            return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(String.valueOf(o)));
        } catch (Exception ignore) {
            return null;
        }
    }


    private List<OperacionJuridicaDto> mapOperacionJuridica(List<Map<String, Object>> operacionesMap) {
        log.debug("Iniciando mapeo de {} resultados de Operacion Juridica.", operacionesMap.size());
        List<OperacionJuridicaDto> operaciones = new ArrayList<>();

        for (Map<String, Object> operacionMap : operacionesMap) {
            OperacionJuridicaDto operacion = new OperacionJuridicaDto();
            BigInteger id = (BigInteger) operacionMap.get("id");
            operacion.setId(Long.valueOf(id.longValue()));

            // Mapeo corregido de TipoOperacionJuridica
            TipoOperacionJuridica tipoOperacionJuridica = new TipoOperacionJuridica();
            tipoOperacionJuridica.setCve((String) operacionMap.get("tipo_operacion_cve"));
            tipoOperacionJuridica.setDescripcion((String) operacionMap.get("descripcion"));

            operacion.setTipoOperacionJuridica(tipoOperacionJuridica);
            operacion.setEstatusCve((String) operacionMap.get("estatus_cve"));
            operacion.setDescripcionOperacion((String) operacionMap.get("descripcion_operacion"));
            operacion.setComentario((String) operacionMap.get("comentario"));
            operacion.setObservaciones((String) operacionMap.get("observaciones"));

            operaciones.add(operacion);
        }

        log.debug("Mapeo de operacion Juridica contemplado");

        return operaciones;
    }

    private List<SolicitudArchivoJuridicaDto> mapSolicitudesArchivos(List<Map<String, Object>> rows) {
        log.debug("Iniciando mapeo de {} resultados de Solicitud de Archivo Juridica", rows.size());

        List<SolicitudArchivoJuridicaDto> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            SolicitudArchivoJuridicaDto d = new SolicitudArchivoJuridicaDto();
            // id (INT IDENTITY) -> Long en DTO
            d.setId(getLong(r.get("id")));
            // id_operacion (BIGINT) -> Long en DTO
            d.setIdOperacion(getLong(r.get("id_operacion")));

            d.setFechaSolicitud(getDate(r.get("fecha_solicitud")));
            d.setFechaCarga(getDate(r.get("fecha_carga")));
            d.setNombreArchivo(getString(r.get("nombre_archivo")));
            d.setRutaArchivo(getString(r.get("ruta_archivo")));
            d.setDescripcionDelActo(getString(r.get("descripcion_del_acto")));
            d.setNombreFormato(getString(r.get("nombre_formato")));
            d.setRutaFormato(getString(r.get("ruta_formato")));
            d.setNota(getString(r.get("nota")));

            out.add(d);
        }

        log.debug("Mapeo de Solicitud de Archivo Juridica completado: {} items", out.size());
        return out;
    }

    // Helpers null-safe y tolerantes de tipo

    private Long getLong(Object o) {
        if (o == null)
            return null;
        if (o instanceof Long)
            return (Long) o;
        if (o instanceof Integer)
            return ((Integer) o).longValue();
        if (o instanceof Short)
            return ((Short) o).longValue();
        if (o instanceof Byte)
            return ((Byte) o).longValue();
        if (o instanceof java.math.BigInteger)
            return ((java.math.BigInteger) o).longValue();
        if (o instanceof java.math.BigDecimal)
            return ((java.math.BigDecimal) o).longValue();
        if (o instanceof Number)
            return ((Number) o).longValue();
        return Long.valueOf(o.toString()); // último recurso
    }

    private String getString(Object o) {
        return o == null ? null : o.toString();
    }

    private java.util.Date getDate(Object o) {
        if (o == null)
            return null;
        if (o instanceof java.util.Date)
            return (java.util.Date) o; // cubre java.sql.Date y Timestamp
        if (o instanceof java.time.LocalDate) {
            return java.sql.Date.valueOf((java.time.LocalDate) o);
        }
        if (o instanceof java.time.LocalDateTime) {
            return java.sql.Timestamp.valueOf((java.time.LocalDateTime) o);
        }
        // intento de parseo si viene como String (yyyy-MM-dd o yyyy-MM-ddTHH:mm:ss)
        String s = o.toString();
        try {
            if (s.length() <= 10) {
                return java.sql.Date.valueOf(java.time.LocalDate.parse(s));
            } else {
                return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(s.replace("Z", "").replace("T", " ")));
            }
        } catch (Exception ignore) {
            // Si no se pudo parsear, devuélvelo como null o lanza excepción controlada
            // según tu criterio
            return null;
        }
    }

    @Transactional
    public void subirArchivosYSetear(String folioStr,
            String directorioUsuario,
            List<MultipartFile> files) throws Exception {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No se recibieron archivos para subir.");
        }

        Long folio = parseFolio(folioStr);

        // Normaliza ruta relativa (sin root)
        String rutaRel = normalizarRel(directorioUsuario);
        LocalDate hoyMx = LocalDate.now(ZoneId.of("America/Mexico_City"));

        log.info("Subiendo {} archivo(s) al SFTP en '{}', para folio {}.", files.size(), rutaRel, folio);

        // 1) Subir cada archivo y actualizar su ruta/fecha en BD
        for (MultipartFile mf : files) {
            if (mf.isEmpty()) {
                log.warn("Archivo vacío ignorado (nombre: {}).", safeName(mf));
                continue;
            }

            final String fileName = safeName(mf); // nombre.ext
            final byte[] content = mf.getBytes(); // contenido

            // SUBIDA SFTP (usa FileServiceSSH que ya creará directorios)
            fileServiceSSH.enviarPorSFTP(fileName, rutaRel, content);

            // Ruta que guardamos en BD (coincide con tu ejemplo hardcodeado, SIN root)
            final String rutaGuardada = joinRel(rutaRel, fileName);

            // UPDATE por nombre_archivo (parametrizado; sin hardcode)
            int upd = instruccionRepository.updateSolicitudDocumentoRuta(
                    folio,
                    fileName,
                    rutaGuardada,
                    hoyMx);

            // Fallback: si no encontró por "nombre.ext", intenta por nombre sin extensión
            if (upd == 0) {
                final String base = stripExt(fileName);
                upd = instruccionRepository.updateSolicitudDocumentoRuta(
                        folio,
                        base,
                        rutaGuardada,
                        hoyMx);
            }

            // (Opcional) Fallback más laxo: normaliza espacios/acentos si tu DB guarda
            // distinto
            if (upd == 0) {
                log.warn("No se encontró registro de solicitud para nombre '{}'. Verifica 'nombre_archivo' en la DB.",
                        fileName);
            } else {
                log.info("Actualizada ruta/fecha para '{}': {}", fileName, rutaGuardada);
            }
        }

        // 2) Setear estatus (igual que tu método existente)
        int instruccionRows = instruccionRepository.updateInstruccionJuridica(folio);
        int operacionRows = instruccionRepository.updateOperacionJuridica(folio);
        log.info("Set estatus: instruccionRows={}, operacionRows={} para folio {}", instruccionRows, operacionRows,
                folio);
    }

    private Long parseFolio(String folioStr) {
        try {
            return Long.valueOf(folioStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Folio inválido: " + folioStr, e);
        }
    }

    private String safeName(MultipartFile mf) {
        String n = (mf.getOriginalFilename() == null) ? "archivo" : mf.getOriginalFilename().trim();
        return n.replace("\\", "/").replaceAll("/+", "/").replaceAll("^/|/$", "");
    }

    private String stripExt(String s) {
        int i = (s == null) ? -1 : s.lastIndexOf('.');
        return (i > 0) ? s.substring(0, i) : s;
    }

    /**
     * normaliza una ruta relativa (sin incluir ftpRoot), sin slashes al inicio/fin
     */
    private String normalizarRel(String ruta) {
        if (ruta == null)
            return "";
        String r = ruta.replace("\\", "/").replaceAll("/+", "/").trim();
        return r.replaceAll("^/|/$", "");
    }

    private String joinRel(String dir, String name) {
        String d = normalizarRel(dir);
        String n = (name == null) ? "" : name.trim();
        return d.isEmpty() ? n : (d + "/" + n);
    }
}
