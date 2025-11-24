package com.bim.seif.controllers;

import com.bim.seif.dto.FideicomisoDto;
import com.bim.seif.dto.InstruccionResponse;
import com.bim.seif.dto.ProgramacionResponse;
import com.bim.seif.models.*;
import com.bim.seif.models.dto.InstruccionJuridicaDto;
import com.bim.seif.models.dto.OperacionJuridicaDto;
import com.bim.seif.services.*;
import com.bim.seif.utils.JSONUtils;
import com.bim.seif.utils.SeifUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/externo")
public class InstruccionesController {

    @Value("${ruta.guardar.instruccion}")
    private String RUTA_GUARDAR_INSTRUCCIONES;

    @Value("${ruta.guardar.adicional}")
    private String RUTA_GUARDAR_ADICIONALES;

    @Value("${ruta.guardar.estadosCuenta}")
    private String RUTA_GUARDAR_ESTADOCUENTA;

    @Autowired
    private FideicomisoService fideicomisoService;
    @Autowired
    private InstruccionService instruccionService;
    @Autowired
    private DocumentoFideicomisoService documentoFideicomisoService;
    @Autowired
    private FileServiceSSH fileServiceSSH;
    @Autowired
    private EmailService emailService;
    @Autowired
    private AuditoriaService auditoriaService;

    SeifUtils seifUtils = new SeifUtils();

    @PostMapping(value = "/instrucciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> guardarInstrucciones(
            @RequestPart("folioFideicomiso") String folioFideicomiso,
            @RequestPart(value = "comentarios", required = false) String comentarios,
            @RequestPart("archivosInstrucciones") List<MultipartFile> archivosInstrucciones,
            @RequestPart(value = "archivosCuenta", required = false) List<MultipartFile> archivosCuenta,
            @RequestPart("cliente") String cliente,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest http) {
        final String username = (jwt != null ? jwt.getSubject() : "");
        String ip = "";
        String ua = "";

        // IP / User-Agent inline (sin helpers)
        if (http != null) {
            String xff = http.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                ip = (comma > -1 ? xff.substring(0, comma).trim() : xff.trim());
            } else {
                String remote = http.getRemoteAddr();
                ip = (remote == null ? "" : remote);
            }
            String hdrUA = http.getHeader("User-Agent");
            ua = (hdrUA == null ? "" : hdrUA);
        }

        try {
            // Timestamp actual
            Timestamp timestampActual = seifUtils.obtenerTimestampActual();

            // Actualiza/normaliza los fideicomisos del usuario autenticado
            if (!username.isBlank()) {
                List<FideicomisoDto> fideicomisoFind = fideicomisoService.obtenerFideicomisos(username);
                if (fideicomisoFind != null && !fideicomisoFind.isEmpty()) {
                    List<Fideicomiso> fideicomisos = new ArrayList<>();
                    for (FideicomisoDto fd : fideicomisoFind) {
                        Fideicomiso f2 = new Fideicomiso();
                        f2.setFolio(fd.getFolio());
                        f2.setAlias(fd.getAlias());
                        f2.setNombreCliente(cliente);
                        f2.setBloqueado(fd.getEstado().getCve().equalsIgnoreCase("B"));
                        Region region = new Region();
                        region.setCve(fd.getRegion().getCve());
                        region.setDescripcion(fd.getRegion().getDescripcion());
                        f2.setRegion(region);
                        fideicomisos.add(f2);
                    }
                    fideicomisoService.actualizarFideicomisos(fideicomisos);
                }
            }

            // Fideicomiso base para las instrucciones a guardar
            Fideicomiso f = new Fideicomiso();
            f.setFolio(folioFideicomiso);

            // Consecutivo
            Integer totalInstrucciones = instruccionService.obtenerTotalInstruccionesPorFideicomiso(folioFideicomiso);
            totalInstrucciones = (totalInstrucciones == null ? 0 : totalInstrucciones);

            List<Instruccion> instrucciones = new ArrayList<>();
            List<String> foliosGuardados = new ArrayList<>(); // <-- NUEVO
            String base = folioFideicomiso.trim();
            String folioPath = "";

            // Procesar cada archivo de instrucción
            for (MultipartFile instruccionFile : archivosInstrucciones) {
                totalInstrucciones++; // siguiente consecutivo
                String sufijo = homologarConsecutivo(totalInstrucciones);
                String folioInstruccion = base + sufijo;
                String rutaParaGuardarInstrucciones = cliente + "/" + folioInstruccion + "/"
                        + RUTA_GUARDAR_INSTRUCCIONES + "/";
                folioPath = folioInstruccion;

                // Validaciones
                if (!sufijo.matches("\\d{6}")) {
                    throw new IllegalStateException("Consecutivo inválido: '" + sufijo + "'");
                }
                if (!folioInstruccion.matches("\\d{14}")) { // 8 (base) + 6 (consecutivo)
                    throw new IllegalStateException("Folio inválido: '" + folioInstruccion + "'");
                }

                log.info("[INSTR] base={}, total={}, sufijo={}, folio={}", base, totalInstrucciones, sufijo,
                        folioInstruccion);

                String nombreArchivo = folioInstruccion
                        + seifUtils.extraerExtecionArchivo(instruccionFile.getOriginalFilename());
                fileServiceSSH.enviarPorSFTP(nombreArchivo, rutaParaGuardarInstrucciones, instruccionFile.getBytes());

                Instruccion nuevaInstruccion = new Instruccion();
                nuevaInstruccion.setRutaArchivo(rutaParaGuardarInstrucciones + nombreArchivo);
                nuevaInstruccion.setComentario(comentarios != null ? comentarios : "");
                nuevaInstruccion.setFechaAlta(timestampActual.toLocalDateTime());
                nuevaInstruccion.setFideicomiso(f);
                nuevaInstruccion.setClienteCarga(cliente);
                nuevaInstruccion.setFolio(folioInstruccion);

                Instruccion instruccionGuardada = instruccionService.guardarInstruccion(nuevaInstruccion);
                instrucciones.add(instruccionGuardada);

                // <-- NUEVO: acumular folio para correo por lote
                foliosGuardados.add(instruccionGuardada.getFolio());

                // Procesar archivos de estado de cuenta (si aplican) por cada folio
                if (archivosCuenta != null && !archivosCuenta.isEmpty()) {
                    String rutaParaGuardarCuenta = cliente + "/" + folioPath + "/" + RUTA_GUARDAR_ESTADOCUENTA + "/";
                    int count = 0;
                    for (MultipartFile archivoCuenta : archivosCuenta) {
                        String noDuplicados = seifUtils.fotmatoTimestamp(timestampActual, "HH-mm-ss") + count;
                        String nombreEstadoDeCuenta = "Cuenta_" +
                                seifUtils.fotmatoTimestamp(timestampActual, "yyyy-MM-dd") + "_" +
                                noDuplicados.replace("-", "");
                        count++;

                        String nombreArchivoCuenta = nombreEstadoDeCuenta +
                                seifUtils.extraerExtecionArchivo(archivoCuenta.getOriginalFilename());

                        fileServiceSSH.enviarPorSFTP(nombreArchivoCuenta, rutaParaGuardarCuenta,
                                archivoCuenta.getBytes());

                        DocumentoFideicomiso documentoFideicomisoObj = new DocumentoFideicomiso();
                        documentoFideicomisoObj.setFideicomisoFolio(folioPath);
                        documentoFideicomisoObj.setNombre(nombreArchivoCuenta);
                        documentoFideicomisoObj.setTipoArchivo("EdoCta");
                        documentoFideicomisoObj.setId(timestampActual.getTime());
                        documentoFideicomisoObj.setRuta(rutaParaGuardarCuenta + nombreArchivoCuenta);
                        documentoFideicomisoService.guardarDocumentoFideicomiso(documentoFideicomisoObj);
                    }
                }
            }

            // ======= CORREO POR LOTE (un solo envío) =======
            if (!username.isBlank() && !foliosGuardados.isEmpty()) {
                String listaFolios = String.join(", ", foliosGuardados);
                emailService.enviarCorreo(
                        username,
                        TipoEvento.instruccion_recepcion,
                        Map.of(
                                Propiedad.fideicomiso_folio, base, // folio de 8 dígitos
                                Propiedad.instruccion_folio, listaFolios // folios 14 dígitos separados por coma
                        ));
            }

            // Auditoría OK
            auditoriaService.registrarCargaInstruccion(username, ip, ua);

            return ResponseEntity.ok(JSONUtils.covertObjectToJSON(Collections.singletonMap("status", "success")));
        } catch (Exception e) {
            // Auditoría FAIL
            try {
                auditoriaService.registrarCargaInstruccionFail(username, ip, ua, e.getMessage());
            } catch (Exception auditEx) {
                log.warn("Fallo registrando auditoría de error: {}", auditEx.getMessage());
            }

            log.error("Error al guardar instrucciones (folio: {}, usuario: {}): {}", folioFideicomiso, username,
                    e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(JSONUtils.covertObjectToJSON(Collections.singletonMap("status", "Error")));
        }
    }

    private String homologarConsecutivo(Integer n) {
        if (n == null || n < 1)
            n = 1; // arranca 000001 si viene null/0/negativo
        if (n > 999_999)
            n = 999_999; // cap opcional (6 dígitos)
        return String.format("%06d", n); // siempre 6 dígitos
    }

    @PostMapping("/fideicomisos")
    public ResponseEntity<String> fideicomisos(@RequestPart(value = "emailCliente") String mailCliente) {
        List<FideicomisoDto> fideicomisos = fideicomisoService.obtenerFideicomisos(mailCliente);
        if (fideicomisos != null) {
            return ResponseEntity.ok(JSONUtils.covertObjectToJSON(fideicomisos));
        } else {
            return ResponseEntity.internalServerError()
                    .body(JSONUtils.covertObjectToJSON(Collections.singletonMap("status", "Error")));
        }
    }

    @GetMapping("/instrucciones")
    public ResponseEntity<String> leerInstrucciones(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        List<InstruccionResponse> instrucciones = instruccionService.getInstruccionesCompletas(username);
        return ResponseEntity.ok(JSONUtils.convertInstruccionesResponseToJSON(instrucciones));
    }

    @GetMapping("/fideicomisos")
    public ResponseEntity<List<FideicomisoDto>> obtenerFideicomisos(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        return ResponseEntity.ok(fideicomisoService.obtenerFideicomisos(username));
    }

    @GetMapping("/instrucciones/{folioInstruccion}/fechas-programadas")
    public ResponseEntity<List<ProgramacionResponse>> getFechasInstruccionProgramada(
            @PathVariable String folioInstruccion) {
        List<ProgramacionResponse> fechas = instruccionService
                .findFechasInstruccionProgramada(folioInstruccion.replace("-", ""));
        if (fechas != null && !fechas.isEmpty()) {
            return ResponseEntity.ok(fechas);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/solicituddearchivo")
    public ResponseEntity<List<InstruccionJuridicaDto>> getInstruccionSolicitudDeArchivos(
            @AuthenticationPrincipal Jwt jwt) throws InterruptedException {
        String userEmail = jwt.getSubject();
        List<InstruccionJuridicaDto> instruccionJuridica = instruccionService
                .findInstruccionesConSolicitudDeArchivos(userEmail);
        if (!instruccionJuridica.isEmpty()) {
            return ResponseEntity.ok(instruccionJuridica);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/solicituddearchivofolio")
    public ResponseEntity<List<OperacionJuridicaDto>> getInstruccionSolicitudDeArchivos(
            @RequestParam("folio_instruccion") String folioInstruccion) throws InterruptedException {
        List<OperacionJuridicaDto> instruccionJuridica = instruccionService
                .findOperacionesConSolicitudDeArchivos(folioInstruccion);
        if (!instruccionJuridica.isEmpty()) {
            return ResponseEntity.ok(instruccionJuridica);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping(value = "/solicituddearchivo/{folio_instruccion}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadJuridica(
            @PathVariable("folio_instruccion") String folioInstruccion,
            @RequestParam("directorioUsuario") String directorioUsuario,
            @RequestPart("files") List<MultipartFile> files) {
        try {
            instruccionService.subirArchivosYSetear(
                    folioInstruccion,
                    directorioUsuario,
                    files);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Error al subir archivos y setear estatus para folio {}.", folioInstruccion, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}