package com.bim.seif.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AuthEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        // Log compacto con causa
        log.warn("401 {} {} - {}", request.getMethod(), request.getRequestURI(), authException.toString());

        // Respuesta base
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("path", request.getRequestURI());

        // Mensaje y código por tipo de excepción
        String code = "unauthorized";
        String message = "No autorizado o token inválido.";

        if (authException instanceof OAuth2AuthenticationException oae) {
            code = oae.getError().getErrorCode();
            message = oae.getError().getDescription() != null
                    ? oae.getError().getDescription()
                    : oae.getMessage();
        } else if (authException instanceof InvalidBearerTokenException) {
            code = "invalid_token";
            message = "El token es inválido o ha expirado.";
        }

        body.put("error", code);
        body.put("message", message);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), body);
    }
}