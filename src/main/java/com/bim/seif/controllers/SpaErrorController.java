package com.bim.seif.controllers;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class SpaErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(SpaErrorController.class);

    private static final String ERROR_PATH = "/error";

    @RequestMapping(ERROR_PATH)
    public String handleError(HttpServletRequest request) {
        log.info("Iniciando manejo de error. URI de la solicitud: {}",
                request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        String uri = uriObj != null ? uriObj.toString() : "";
        String contextPath = request.getContextPath() != null ? request.getContextPath() : "";

        // Normaliza el path quitando el context-path si viene incluido
        String path = uri;
        if (!contextPath.isEmpty() && uri.startsWith(contextPath)) {
            path = uri.substring(contextPath.length());
            log.debug("Path normalizado (sin context-path): {}", path);
        }

        int status = -1;
        try {
            status = (statusObj != null) ? Integer.parseInt(statusObj.toString()) : -1;
        } catch (NumberFormatException ignored) {
            // si no se puede parsear, tratamos como "otro código"
            log.warn("No se pudo parsear el código de estado: {}. Usando -1.", statusObj);
        }

        log.info("Status: {} para el path: {}", status, path);

        // Solo reescribimos a SPA en 404 de rutas "de app" (sin punto)
        if (status == HttpStatus.NOT_FOUND.value()) {
            log.debug("Procesando error 404 (NOT_FOUND).");

            // 1) Si parece archivo (tiene punto), no tocar (deja 404 estático o handler por
            // defecto)
            if (path.contains(".")) {
                log.debug("Path contiene punto (posible activo estático). Reenviando a forward:/.");
                return "forward:/";
            }

            // 2) Evita prefijos de backend (SIN incluir el context-path)
            if (path.startsWith("/api")
                    || path.startsWith("/docs")
                    || path.startsWith("/swagger-ui")
                    || path.startsWith("/v3")
                    || path.startsWith("/auth")
                    || path.startsWith("/archivos")
                    || path.startsWith("/error")) {
                log.debug("Path coincide con prefijo de backend. Reenviando a forward:/.");
                return "forward:/";
            }

            log.info("404 en ruta que parece de SPA. Reenviando a /ext/index.html: {}", path);
            return "forward:/ext/index.html";
        }

        log.warn("Status de error no 404 ({}). Reenviando al index de la SPA: {}", status, path);
        return "forward:/ext/index.html";
    }

    public String getErrorPath() {
        return ERROR_PATH;
    }
}