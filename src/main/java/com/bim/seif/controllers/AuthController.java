package com.bim.seif.controllers;

import com.bim.seif.dto.AuthRequest;
import com.bim.seif.dto.AuthResponse;
import com.bim.seif.services.AuditoriaService;
import com.bim.seif.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditoriaService auditoriaService;

    @PostMapping("/otp")
    public ResponseEntity<String> solicitarCodigoAcceso(@RequestBody AuthRequest request,
        HttpServletRequest http) {
        log.info(">> /auth/otp hit: {}", request.getUsuario());
        try {
            authService.enviarCodigoAutenticacion(request);
            auditoriaService.registrarOtpSolicitado(request.getUsuario(),
             getIp(http),
            http.getHeader("User-Agent"));

            return ResponseEntity.ok("{\"code\":\"OK\"}");
        } catch (DisabledException e) {
            log.warn("Usuario bloqueado: {}", request.getUsuario());
            auditoriaService.registrarLoginFail(request.getUsuario(), getIp(http),
            http.getHeader("User-Agent"), "Usuario bloqueado por intentos");
            return ResponseEntity.status(HttpStatus.LOCKED) // 423
                    .body("{\"error\":\"Usuario bloqueado por intentos\"}");
        } catch (Exception e) {
            log.warn("Credenciales invalidas u otro error controlado: {}", e.getMessage());
            auditoriaService.registrarLoginFail(request.getUsuario(), getIp(http),
            http.getHeader("User-Agent"), "El usuario o la contraseña incorrectos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"El usuario o la contraseña no son correctos\"}");
        }
    }

    @PostMapping("/credenciales")
    public ResponseEntity<AuthResponse> autenticar(@RequestBody AuthRequest request, HttpServletRequest http) {
        AuthResponse result = authService.autenticar(request);
        if(result != null){
            auditoriaService.registrarLoginOk(request.getUsuario(), getIp(http),
                http.getHeader("User-Agent"));
            return ResponseEntity.ok(result);
        }else{
            auditoriaService.registrarLoginFail(request.getUsuario(), getIp(http),
                http.getHeader("User-Agent"), "El usuario o la contraseña incorrectos");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String rt = body.get("refreshToken");
        if (rt == null || rt.isBlank()) {
            log.warn("Refresh rechazado: refreshToken vacío o ausente");
            return ResponseEntity.badRequest().build();
        }

        AuthResponse result = authService.refresh(rt);

        if (result != null) {
            log.info("Sesión renovada correctamente (rotación de RT).");
            return ResponseEntity.ok(result);
        } else {
            log.warn("Refresh inválido: token no encontrado/consumido o expirado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
        authService.logout(jwt); // coloca el jti en blacklist hasta su expiración
        auditoriaService.registrarLogOutOk(jwt.getSubject(), getIp(http),
                http.getHeader("User-Agent"));
        
        return ResponseEntity.noContent().build();
    }

    private String getIp(HttpServletRequest http) {
        String ip = http.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank())
            ip = http.getRemoteAddr();
        return ip;
    }
}