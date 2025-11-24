package com.bim.seif.services;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bim.seif.models.Cliente;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.models.dto.ForgotPasswordClientDTO;
import com.bim.seif.repositories.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordServiceClient {

    private final CustomerRepository customerRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ResponseEntity<?> validateUsr(ForgotPasswordClientDTO request) {
        try {
            if (request.getUsuario() == null || request.getUsuario().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "El correo es obligatorio"));
            }

            var usuarioOpt = customerRepository.findByEmailAndFechaBajaIsNull(request.getUsuario());
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("mensaje", "Usuario no encontrado o inactivo"));
            }

            return ResponseEntity.ok(Map.of("mensaje", "Correo valido"));

        } catch (Exception e) {
            log.error("Error al validar usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error interno del servidor"));
        }
    }

    // Enviar OTP
    public ResponseEntity<?> otpSend(ForgotPasswordClientDTO request) {
        try {
            if (request == null || request.getUsuario() == null || request.getUsuario().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "El correo es obligatorio"));
            }

            final String email = request.getUsuario().trim().toLowerCase();

            var usuarioOpt = customerRepository.findByEmailAndFechaBajaIsNull(email);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("mensaje", "Usuario no encontrado o inactivo"));
            }

            // 1) Genera y persiste el OTP con su TTL
            String otp = authService.generarOtp(email);// <-- IMPORTANTE: que retorne el código

            // 2) Envía correo usando tu plantilla con {{otp}}
            emailService.enviarCorreo(
                    email,
                    TipoEvento.seguridad_token_cliente,
                    Map.of(Propiedad.otp, otp));

            // 3) Respuesta sin filtrar OTP
            return ResponseEntity.ok(Map.of(
                    "mensaje", "OTP enviado correctamente",
                    "destinatario", maskEmail(email)));

        } catch (Exception e) {
            // No loggear el OTP jamás
            log.error("Error al enviar OTP para forgot-password cliente: {}", e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "No fue posible enviar el OTP. Intenta nuevamente."));
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1)
            return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.length() <= 3 ? local.substring(0, 1) : local.substring(0, 3);
        return visible + "****" + domain;
    }

    // Validar OTP
    public ResponseEntity<?> otpValidate(ForgotPasswordClientDTO request) {
        try {
            if (request.getUsuario() == null || request.getUsuario().isBlank() ||
                    request.getOtp() == null || request.getOtp().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "Usuario y OTP son obligatorios"));
            }

            boolean valido = authService.validarOTP(request.getUsuario(), request.getOtp());
            if (!valido) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("mensaje", "Codigo OTP incorrecto"));
            }

            return ResponseEntity.ok(Map.of("mensaje", "OTP válido"));

        } catch (Exception e) {
            log.error("Error al validar OTP: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error interno del servidor"));
        }
    }

    // Actualizar contraseña
    public ResponseEntity<?> updatePass(ForgotPasswordClientDTO request) {
        try {
            if (request.getUsuario() == null || request.getUsuario().isBlank() ||
                    request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "Usuario y contrasena son obligatorios"));
            }

            var usuarioOpt = customerRepository.findByEmailAndFechaBajaIsNull(request.getUsuario());
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("mensaje", "Usuario no encontrado o inactivo"));
            }

            Cliente usuario = usuarioOpt.get();
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            customerRepository.save(usuario);

            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));

        } catch (Exception e) {
            log.error("Error al actualizar contrasena: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "Error al actualizar la contrasena: " + e.getMessage()));
        }
    }
}
