package com.bim.seif.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.mail.MessagingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioBloqueado(DisabledException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "user_disabled");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Usuario bloqueado");
        body.put("type", "authentication");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> badCreds(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Collections.singletonMap("error", "El usuario o la contraseña no son correctos"));
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<Map<String, String>> messaging(MessagingException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Collections.singletonMap("error", "No fue posible entregar el código, inténtalo más tarde"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> badJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "Solicitud inválida"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Validación fallida");
        body.put("message", e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "Datos inválidos");
        return ResponseEntity.badRequest().body(body);
    }

    // ⬇️ ÚNICO catch-all (evita ambigüedad)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> fallback(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", "Error interno"));
    }
}