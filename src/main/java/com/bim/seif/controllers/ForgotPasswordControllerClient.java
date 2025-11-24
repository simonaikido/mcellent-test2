package com.bim.seif.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bim.seif.models.dto.ForgotPasswordClientDTO;
import com.bim.seif.services.ForgotPasswordServiceClient;

@Slf4j
@RestController
@RequestMapping("/forgotPass") 
@RequiredArgsConstructor
public class ForgotPasswordControllerClient {

    private final ForgotPasswordServiceClient fpwdService;

    /**
     * Endpoint para validar si el usuario (email) existe y es apto para el proceso de recuperación.
     * @param request Contiene el email del usuario.
     * @return ResponseEntity con el resultado de la validación.
     */
    @PostMapping("/validateUsr")
    public ResponseEntity<?> validateUsr(@RequestBody ForgotPasswordClientDTO request) {
        String user = request.getUsuario();
        log.info("Peticion para validar usuario en proceso de recuperacion de contrasena: {}", user);
        
        try {
            ResponseEntity<?> response = fpwdService.validateUsr(request);
            log.info("Validacion de usuario {} completada. Status HTTP: {}", user, response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Fallo en la validacion de usuario {}.", user, e);
            // Delegamos el manejo HTTP al servicio, pero registramos el error aquí.
            throw e; 
        }
    }

    /**
     * Endpoint para actualizar la contraseña del usuario, asumiendo que el proceso de OTP fue exitoso.
     * @param request Contiene el email y la nueva contraseña.
     * @return ResponseEntity con el resultado de la actualización.
     */
    @PostMapping("/updatePass")
    public ResponseEntity<?> updatePass(@RequestBody ForgotPasswordClientDTO request) {
        String user = request.getUsuario();
        log.info("Peticion para actualizar contraseña del usuario: {}", user);
        
        try {
            ResponseEntity<?> response = fpwdService.updatePass(request);
            log.info("Actualizacion de contraseña para {} completada. Status HTTP: {}", user, response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Fallo al actualizar contrasena para {}.", user, e);
            throw e;
        }
    }

    /**
     * Endpoint para solicitar el envío del código OTP (One-Time Password) al usuario.
     * @param request Contiene el email del usuario.
     * @return ResponseEntity con el resultado del envío.
     */
    @PostMapping("/otpSend")
    public ResponseEntity<?> otpSend(@RequestBody ForgotPasswordClientDTO request) {
        String user = request.getUsuario();
        log.info("Petición para enviar OTP al usuario: {}", user);
        
        try {
            ResponseEntity<?> response = fpwdService.otpSend(request);
            log.info("Envío de OTP para {} completado. Status HTTP: {}", user, response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Fallo en el envio de OTP para {}.", user, e);
            throw e;
        }
    }

    /**
     * @param request Contiene el email del usuario y el código OTP.
     * @return ResponseEntity con el resultado de la validación del OTP.
     */
    @PostMapping("/otpValidate")
    public ResponseEntity<?> otpValidate(@RequestBody ForgotPasswordClientDTO request) {
        String user = request.getUsuario();
        log.info("Peticion para validar OTP del usuario: {}", user);
        
        try {
            ResponseEntity<?> response = fpwdService.otpValidate(request);
            log.info("Validacion de OTP para {} completada. Status HTTP: {}", user, response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Fallo en la validacion de OTP para {}.", user, e);
            throw e;
        }
    }
}