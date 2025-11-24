package com.bim.seif.services;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import com.bim.seif.models.ClienteAudit;
import com.bim.seif.repositories.ClienteAuditRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {
    private final ClienteAuditRepository cAuditRepository;

    private void generarRegistroAuditoria(String uid, String accion, String actor, String ip, String ua,
            String comentario) {
        try {
            ClienteAudit ca = new ClienteAudit();
            ca.setUid(uid);
            ca.setAccion(accion);
            ca.setActor(actor);
            ca.setIp(ip);
            ca.setUserAgent(ua);
            ca.setComentario(comentario);
            ca.setCreatedAt(OffsetDateTime.now());

            cAuditRepository.save(ca);
        } catch (Exception ex) {
            log.error("Error registrando: ", ex.getMessage());
        }
    }

    public void registrarLoginOk(String uid, String ip, String userAgent) {
        log.info("Registrando login ok para usuario: {}", uid);
        generarRegistroAuditoria(uid, "LOGIN_OK",
                (uid == null || uid.isEmpty()) ? "desconocido" : uid, ip, userAgent, "OTP confirmado / login exitoso");
    }

    public void registrarLogOutOk(String uid, String ip, String userAgent) {
        log.info("Registrando logout ok para usuario: {}", uid);
        generarRegistroAuditoria(uid, "LOGOUT_OK",
                (uid == null || uid.isEmpty()) ? "desconocido" : uid, ip, userAgent, "Logout exitoso");
    }

    public void registrarLoginFail(String uid, String ip, String userAgent, String motivo) {
        log.info("Registrando login fallido para usuario: {}", uid);
        generarRegistroAuditoria(uid, "LOGIN_FAIL", (uid == null || uid.isEmpty()) ? "anon" : uid,
                ip, userAgent, motivo);
    }

    public void registrarOtpSolicitado(String uid, String ip, String userAgent) {
        log.info("Registrando Solicitud de Otp");
        generarRegistroAuditoria((uid == null || uid.isEmpty()) ? "anon" : uid, "OTP_REQUESTED",
                (uid == null || uid.isEmpty()) ? "anon" : uid, ip, userAgent, "OTP enviado");
    }

    public void registrarCargaInstruccion(String uid, String ip, String userAgent) {
        log.info("Registrando Alta de Instruccion");
        generarRegistroAuditoria((uid == null || uid.isEmpty()) ? "anon" : uid, "CREATE_INSTRUCCION",
                (uid == null || uid.isEmpty()) ? "anon" : uid, ip, userAgent, "Alta de Instruccion");
    }

    public void registrarCargaInstruccionFail(String uid, String ip, String userAgent, String motiivo) {
        log.info("Registrando Alta de Instruccion");
        generarRegistroAuditoria((uid == null || uid.isEmpty()) ? "anon" : uid, "CREATE_INSTRUCCION_FAIL",
                (uid == null || uid.isEmpty()) ? "anon" : uid, ip, userAgent, motiivo);
    }

    public void registrarCargaArchivos(String uid, String ip, String userAgent) {
        log.info("Registrando Carga de Archivo");
        generarRegistroAuditoria((uid == null || uid.isEmpty()) ? "anon" : uid, "UPLOAD_FILE",
                (uid == null || uid.isEmpty()) ? "anon" : uid, ip, userAgent, "Carga de Archivo");
    }

    public void registrarCargaArchivosFail(String uid, String ip, String userAgent, String motiivo) {
        log.info("Registrando Carga de Archivo");
        generarRegistroAuditoria((uid == null || uid.isEmpty()) ? "anon" : uid, "UPLOAD_FILE_FAIL",
                (uid == null || uid.isEmpty()) ? "anon" : uid, ip, userAgent, motiivo);
    }
public void registrarDescargaArchivos(String uid, String ip, String userAgent) {
        log.info("Registrando Descarga de Archivo");
        generarRegistroAuditoria((uid == null || uid.isEmpty()) ? "anon" : uid, "DOWNLOAD_FILE",
                (uid == null || uid.isEmpty()) ? "anon" : uid, ip, userAgent, "Descarga de Archivo");
    }

    public void registrarDescargaArchivosFail(String uid, String ip, String userAgent, String motiivo) {
        log.info("Registrando Descarga de Archivo");
        generarRegistroAuditoria((uid == null || uid.isEmpty()) ? "anon" : uid, "DOWNLOAD_FILE_FAIL",
                (uid == null || uid.isEmpty()) ? "anon" : uid, ip, userAgent, motiivo);
    }

}
