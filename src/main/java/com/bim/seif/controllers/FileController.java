package com.bim.seif.controllers;

import com.bim.seif.dto.FideicomisoDto;
import com.bim.seif.dto.MetadatosRequest;
import com.bim.seif.models.DocumentoFideicomiso;
import com.bim.seif.models.EstatusInstruccion;
import com.bim.seif.models.Instruccion;
import com.bim.seif.models.SolicitudCuenta;
import com.bim.seif.repositories.InstruccionMonetariaRepository;
import com.bim.seif.repositories.InstruccionRepository;
import com.bim.seif.repositories.SolicitudCuentaRepository;
import com.bim.seif.services.AuditoriaService;
import com.bim.seif.services.DocumentoFideicomisoService;
import com.bim.seif.services.FideicomisoService;
import com.bim.seif.services.FileServiceSSH;
import com.bim.seif.services.FileServiceSSH.ArchivoInfo;
import com.bim.seif.utils.JSONUtils;
import com.bim.seif.utils.SeifUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/archivos")
@RequiredArgsConstructor
public class FileController {

    // Servicios/Repos
    private final FileServiceSSH fileServiceSSH;
    private final FideicomisoService fideicomisoService;
    private final InstruccionRepository instruccionRepository;
    private final InstruccionMonetariaRepository instruccionMonetariaRepository;
    private final SolicitudCuentaRepository solicitudCuentaRepository;
    private final AuditoriaService auditoriaService;
    private final DocumentoFideicomisoService documentoFideicomisoService;

    // Utilerías/Config
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final SeifUtils seifUtils = new SeifUtils();

    @Value("${ruta.guardar.adicional}")
    private String RUTA_GUARDAR_ADICIONALES;

    @Value("${ftp.path}")
    private String FTP_PATH;

    @Value("${ruta.guardar.estadosCuenta}")
    private String estadosDeCuentaDir;

    // 1) SUBIDA DE ARCHIVOS ADICIONALES (síncrono, opción A)
    // Endpoint equivalente a /api/upload (lo mantenemos colgado de /archivos)
    @PostMapping(path = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> archivosSolicitados(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            HttpServletRequest http) {

        try {
            log.info("Petición recibida con {} archivo(s). Metadata: {}",
                    files != null ? files.size() : 0,
                    (metadataJson != null && !metadataJson.isBlank()) ? "Presente" : "Ausente");

            MetadatosRequest metadata = MAPPER.readValue(metadataJson, MetadatosRequest.class);

            // --- Validaciones iniciales ---
            if (files == null || files.isEmpty()) {
                auditoriaService.registrarCargaArchivosFail(metadata.getCliente(), getIp(http),
                        http.getHeader("User-Agent"), "Debe enviar al menos un archivo en 'files'.");
                return badRequest("Debe enviar al menos un archivo en 'files'.");
            }
            if (metadataJson == null || metadataJson.isBlank()) {
                auditoriaService.registrarCargaArchivosFail(metadata.getCliente(), getIp(http),
                        http.getHeader("User-Agent"), "Se requiere 'metadata' con 'cliente' y 'folioFideicomiso'.");
                return badRequest("Se requiere 'metadata' con 'cliente' y 'folioFideicomiso'.");
            }

            // --- Deserializar metadatos ---
            String clienteEmail = Optional.ofNullable(metadata.getCliente()).orElse("").trim().toLowerCase();
            String folioFideicomiso = Optional.ofNullable(metadata.getFolioFideicomiso()).orElse("").trim();

            if (clienteEmail.isEmpty() || folioFideicomiso.isEmpty()) {
                auditoriaService.registrarCargaArchivosFail(metadata.getCliente(), getIp(http),
                        http.getHeader("User-Agent"),
                        "Metadata incompleta: 'cliente' y 'folioFideicomiso' son obligatorios.");
                return badRequest("Metadata incompleta: 'cliente' y 'folioFideicomiso' son obligatorios.");
            }

            // --- Validar fideicomiso del cliente ---
            Optional<FideicomisoDto> f = fideicomisoService
                    .obtenerFideicomisoPorFolio(clienteEmail, folioFideicomiso);

            if (f.isEmpty()) {
                String msg = String.format("Fideicomiso no encontrado para Cliente '%s' y Folio '%s'.",
                        clienteEmail, folioFideicomiso);
                log.error(msg);
                return notFound(msg);
            }
            log.debug("Fideicomiso encontrado: {}", f.get().getFolio());

            // --- Ruta relativa de destino en el SFTP ---
            String rutaRelativa = normalizarRuta(
                    clienteEmail + "/" + folioFideicomiso + "/" + RUTA_GUARDAR_ADICIONALES + "/");
            log.debug("Ruta SFTP relativa final: {}", rutaRelativa);

            // --- Prefijo de fecha para nombre de archivo ---
            Timestamp now = seifUtils.obtenerTimestampActual();
            String fecha = seifUtils.fotmatoTimestamp(now, "yyyy-MM-dd");

            // --- Subir archivos (síncrono) ---
            List<String> subidos = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    log.warn("Archivo nulo/vacío, se omite.");
                    continue;
                }
                String original = Optional.ofNullable(file.getOriginalFilename()).orElse("archivo");
                String limpio = original.replaceAll("\\s+", "_");
                String nombreFinal = fecha + "_" + limpio;

                log.info("Subiendo archivo: {} ({} bytes) → {}", original, file.getSize(), rutaRelativa);
                try {
                    fileServiceSSH.enviarPorSFTP(nombreFinal, rutaRelativa, file.getBytes()); // síncrono
                    subidos.add(nombreFinal);
                    auditoriaService.registrarCargaArchivos(metadata.getCliente(), getIp(http),
                            http.getHeader("User-Agent"));
                    log.info("Archivo '{}' subido correctamente.", nombreFinal);
                } catch (JSchException | SftpException ex) {
                    String msg = "Fallo SFTP al subir '" + original + "': " + ex.getMessage();
                    auditoriaService.registrarCargaArchivosFail(metadata.getCliente(), getIp(http),
                            http.getHeader("User-Agent"), "Fallo SFTP al subir");
                    log.error(msg, ex);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body(JSONUtils.covertObjectToJSON(Map.of(
                                    "status", "error",
                                    "message", msg)));
                } catch (Exception ex) {
                    String msg = "Error inesperado al subir '" + original + "'.";
                    auditoriaService.registrarCargaArchivosFail(metadata.getCliente(), getIp(http),
                            http.getHeader("User-Agent"), "Error inesperado al subir");
                    log.error(msg, ex);
                    return internalServerError(msg);
                }
            }

            if (subidos.isEmpty()) {
                return badRequest("Ningún archivo válido para subir.");
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("status", "success");
            payload.put("rutaRelativa", rutaRelativa);
            payload.put("archivos", subidos);
            payload.put("total", subidos.size());

            return ResponseEntity.ok(JSONUtils.covertObjectToJSON(payload));

        } catch (Exception ex) {
            log.error("Fallo general al procesar la subida de archivos adicionales.", ex);
            return internalServerError("Error al procesar los archivos.");
        }
    }

    @GetMapping
    public void descargarArchivo(@RequestParam("nombreArchivo") String nombreArchivo,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletResponse response,
            HttpServletRequest http) {
        final String cliente = (jwt != null ? jwt.getSubject() : "anon");
        try {
            // 1) Toma el valor CRUDO de la query (percent-encoded) si existe
            final String raw = extractRawParamFromQuery(http.getQueryString(), "nombreArchivo");

            // 2) Construye las dos rutas más confiables:
            // a) La que ya te entrega Spring (normalmente ya UTF-8)
            // b) La decodificación explícita de lo crudo en UTF-8 (por si el contenedor no
            // lo hizo)
            final String springDecoded = java.text.Normalizer.normalize(
                    (nombreArchivo == null ? "" : nombreArchivo).replace("\\", "/").replaceAll("/+", "/"),
                    java.text.Normalizer.Form.NFC);

            final String rawDecoded = (raw == null) ? null
                    : java.text.Normalizer.normalize(
                            java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8)
                                    .replace("\\", "/").replaceAll("/+", "/"),
                            java.text.Normalizer.Form.NFC);

            // 3) Última red de seguridad: corrige mojibake comunes (Ã³, ¾, etc.)
            final String springFixed = fixCommonMojibake(springDecoded);
            final String rawFixed = (rawDecoded == null) ? null : fixCommonMojibake(rawDecoded);

            // 4) Intenta en orden: rawDecoded → springDecoded → rawFixed → springFixed
            // (evitamos múltiples decodificaciones que causan “No such file”)
            List<String> candidatos = new ArrayList<>();
            if (rawDecoded != null)
                candidatos.add(rawDecoded);
            candidatos.add(springDecoded);
            if (rawFixed != null && !rawFixed.equals(rawDecoded))
                candidatos.add(rawFixed);
            if (!springFixed.equals(springDecoded))
                candidatos.add(springFixed);

            boolean success = false;
            String elegido = null;

            for (String path : candidatos) {
                if (path == null || path.isBlank())
                    continue;
                log.info("Probando ruta SFTP candidata: {}", path);
                if (fileServiceSSH.getFileSmart(path, response.getOutputStream())) {
                    success = true;
                    elegido = path;
                    break;
                }
            }

            if (success) {
                final String filename = java.nio.file.Paths.get(elegido).getFileName().toString();
                final String contentType = guessContentTypeByName(filename);
                response.setContentType(contentType);

                // Content-Disposition con RFC 5987 (UTF-8)
                final String filenameParam = java.net.URLEncoder
                        .encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filenameParam);

                auditoriaService.registrarDescargaArchivos(cliente, getIp(http), http.getHeader("User-Agent"));
                log.info("Archivo descargado con éxito usando: {}", elegido);
                return;
            }

            // 5) Si falló todo, deja trazas útiles listando el directorio padre
            final String ultimo = candidatos.stream().filter(Objects::nonNull).reduce((a, b) -> b)
                    .orElse(springDecoded);
            final String parent = (ultimo != null && ultimo.contains("/"))
                    ? ultimo.substring(0, ultimo.lastIndexOf('/'))
                    : ".";
            log.error("SFTP: No such file. Ningún candidato existió. Padre: {}", parent);

            auditoriaService.registrarDescargaArchivosFail(
                    cliente, getIp(http), http.getHeader("User-Agent"),
                    "SFTP no encontró ninguna de las rutas candidatas");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error("Fallo general al descargar archivo: {}", nombreArchivo, e);
            auditoriaService.registrarDescargaArchivosFail(
                    cliente, getIp(http), http.getHeader("User-Agent"),
                    "Fallo general al descargar archivo");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /* ===== Helpers ===== */

    // Lee el parámetro crudo (percent-encoded) desde la query string
    private static String extractRawParamFromQuery(String query, String param) {
        if (query == null || query.isBlank())
            return null;
        // Soporta múltiples parámetros y valores con '=' dentro
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0)
                continue;
            String key = pair.substring(0, idx);
            String val = pair.substring(idx + 1);
            if (key.equals(param)) {
                return val; // ¡AÚN percent-encoded!
            }
        }
        return null;
    }

    // Corrige mojibake más comunes (incluye tu caso '¾' 'ó')
    private static String fixCommonMojibake(String s) {
        if (s == null)
            return null;
        String r = s;
        // Mojibake UTF-8 visto como ISO-8859-1
        r = r.replace("Ã¡", "á").replace("Ã©", "é").replace("Ã­", "í")
                .replace("Ã³", "ó").replace("Ãº", "ú")
                .replace("Ã±", "ñ").replace("Ã‘", "Ñ")
                .replace("Ã¼", "ü").replace("Ãœ", "Ü")
                .replace("Â", ""); // basura frecuente antes de símbolos (°, ©, etc.)
        // Caso reportado en tus logs: “¾” en lugar de “ó”
        r = r.replace("¾", "ó");
        // También ocurren “³” (superíndice 3) por “ó” en algunos ambientes
        r = r.replace("³", "ó");
        return r;
    }

    // Igual al que ya te pasé
    private static String guessContentTypeByName(String path) {
        String p = (path == null ? "" : path).toLowerCase(java.util.Locale.ROOT);
        if (p.endsWith(".pdf"))
            return "application/pdf";
        if (p.endsWith(".xls"))
            return "application/vnd.ms-excel";
        if (p.endsWith(".xlsx"))
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (p.endsWith(".xlsm"))
            return "application/vnd.ms-excel.sheet.macroEnabled.12";
        if (p.endsWith(".doc"))
            return "application/msword";
        if (p.endsWith(".docx"))
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (p.endsWith(".csv"))
            return "text/csv";
        return org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    // 3) SUBIDA DE ESTADO DE CUENTA (PDF) + actualización entidades
    @PostMapping(path = "/api/upload/estado-cuenta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadEstadoCuenta(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("metadata") String metadataJson,
            HttpServletRequest http) {

        String clienteError = "N/A";
        String folioError = "N/A";
        String client = "";

        log.info("Petición recibida con {} archivos. Metadata: {}", files != null ? files.size() : 0, metadataJson);

        try {
            MetadatosRequest metadataObj = MAPPER.readValue(metadataJson, MetadatosRequest.class);
            client = metadataObj.getCliente();
            if (files == null || files.isEmpty()) {
                auditoriaService.registrarCargaArchivosFail(client, getIp(http), http.getHeader("User-Agent"),
                        "No se recibieron archivos");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No se recibieron archivos"));
            }

            final String finalCliente = Optional.ofNullable(metadataObj.getCliente()).orElse("").trim().toLowerCase();
            final String finalFolio = Optional.ofNullable(metadataObj.getFolioFideicomiso()).orElse("").trim();

            clienteError = finalCliente;
            folioError = finalFolio;

            if (finalCliente.isEmpty() || finalFolio.isEmpty()) {
                auditoriaService.registrarCargaArchivosFail(client, getIp(http), http.getHeader("User-Agent"),
                        "Faltan campos en metadata ");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Faltan campos en metadata (cliente/folioFideicomiso)"));
            }

            final String rutaRelativa = normalizarRuta(
                    finalCliente + "/" + finalFolio + "/" + estadosDeCuentaDir + "/");
            final String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.debug("Ruta SFTP relativa: {}", rutaRelativa);

            // Instrucción
            final Instruccion instruccion = instruccionRepository.findById(finalFolio)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Instruccion no encontrada para folio=" + finalFolio));

            // Validaciones de archivo
            final long MAX = 15L * 1024 * 1024;
            final long MIN = 1L * 1024;
            List<Map<String, Object>> subidos = new ArrayList<>();

            for (MultipartFile file : files) {
                String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("archivo.pdf").trim();
                String lower = originalFilename.toLowerCase(Locale.ROOT);
                long fileSize = file.getSize();

                if (!lower.endsWith(".pdf")) {
                    auditoriaService.registrarCargaArchivosFail(client, getIp(http), http.getHeader("User-Agent"),
                            "Solo se permiten PDFs");
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body(Map.of("error", "Solo se permiten PDFs", "archivo", originalFilename));
                }
                if (fileSize > MAX || fileSize <= MIN) {
                    auditoriaService.registrarCargaArchivosFail(client, getIp(http), http.getHeader("User-Agent"),
                            "Tamaño inválido (min 1KB, max 15MB)");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Tamaño inválido (min 1KB, max 15MB)", "archivo", originalFilename));
                }

                String nombreFinal = fecha + "_" + originalFilename;
                log.info("Subiendo archivo: {} a ruta: {}", nombreFinal, rutaRelativa);

                // Sube síncrono
                fileServiceSSH.enviarPorSFTP(nombreFinal, rutaRelativa, file.getBytes());
                log.debug("Archivo {} subido a SFTP.", nombreFinal);

                // SolicitudCuenta (crear/actualizar)
                SolicitudCuenta sc = solicitudCuentaRepository.findByInstruccionFolio(finalFolio)
                        .orElseGet(() -> {
                            log.debug("Creando nueva SolicitudCuenta para el folio {}.", finalFolio);
                            SolicitudCuenta newSc = new SolicitudCuenta();
                            newSc.setFolio(instruccion); // setea la relación con la Instruccion
                            return newSc;
                        });

                sc.setFechaCarga(LocalDateTime.now());
                sc.setRutaEdoCta(rutaRelativa + nombreFinal);
                solicitudCuentaRepository.save(sc);

                subidos.add(Map.of(
                        "archivo", nombreFinal,
                        "bytes", fileSize,
                        "ruta", rutaRelativa));
            }

            // Limpiar fechaCancelacion y guardar
            instruccion.setFechaCancelacion(null);
            instruccionRepository.save(instruccion);

            // Estatus monetaria = PR
            var monetaria = instruccionMonetariaRepository.findByInstruccionFolio(finalFolio)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Instruccion monetaria no encontrada para folio=" + finalFolio));

            EstatusInstruccion estatusPR = new EstatusInstruccion();
            estatusPR.setCve("PR");
            monetaria.setEstatus(estatusPR);
            instruccionMonetariaRepository.save(monetaria);

            auditoriaService.registrarCargaArchivos(client, getIp(http), http.getHeader("User-Agent"));

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "cliente", finalCliente,
                    "folio", finalFolio,
                    "rutaRelativa", rutaRelativa,
                    "archivos", subidos));

        } catch (ResponseStatusException e) {
            auditoriaService.registrarCargaArchivosFail(client, getIp(http), http.getHeader("User-Agent"),
                    "No se recibieron archivos");
            log.warn("Excepción controlada ({}): {}", e.getStatus(), e.getReason());
            throw e;
        } catch (Exception e) {
            auditoriaService.registrarCargaArchivosFail(client, getIp(http), http.getHeader("User-Agent"),
                    "No se recibieron archivos");
            log.error("Fallo general al subir estados de cuenta para cliente {} / folio {}.", clienteError, folioError,
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la carga"));
        }
    }

    // Helpers
    private static String normalizarRuta(String ruta) {
        if (ruta == null)
            return "/";
        String limpia = ruta.replace("\\", "/").replaceAll("/+", "/");
        if (limpia.startsWith("/"))
            limpia = limpia.substring(1);
        if (!limpia.endsWith("/"))
            limpia = limpia + "/";
        return limpia;
    }

    private static ResponseEntity<String> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(JSONUtils.covertObjectToJSON(Map.of("status", "error", "message", message)));
    }

    private static ResponseEntity<String> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(JSONUtils.covertObjectToJSON(Map.of("status", "error", "message", message)));
    }

    private static ResponseEntity<String> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(JSONUtils.covertObjectToJSON(Map.of("status", "error", "message", message)));
    }

    private String getIp(HttpServletRequest http) {
        String ip = http.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank())
            ip = http.getRemoteAddr();
        return ip;
    }

    // Nuevo Endpoint para Documentos de Fideicomiso
    @GetMapping("/fideicomiso/{folio}")
    public ResponseEntity<List<DocumentoFideicomiso>> getDocumentosFideicomisoPorFolio(@PathVariable String folio) {

        List<DocumentoFideicomiso> documentos = documentoFideicomisoService.getDocumentosByFolioFideicomiso(folio);

        if (documentos.isEmpty()) {
            log.info("No se encontraron documentos para el folio: {}", folio);
            return ResponseEntity.noContent().build();
        }
        log.info("Se encontraron {} documentos para el folio: {}", documentos.size(), folio);
        return ResponseEntity.ok(documentos);
    }

    @GetMapping(value = "/listar/formatosBIM", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ArchivoInfo>> listarFormatosBIM() {
        try {
            List<ArchivoInfo> formatos = fileServiceSSH.listarArchivosSFTP(null); // el método arma la ruta internamente
            if (formatos == null || formatos.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(formatos);
        } catch (Exception e) {
            log.error("Error al listar formatos BIM", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}