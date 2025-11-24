package com.bim.seif.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RevokedJwtFilter extends OncePerRequestFilter {

    private final RefreshTokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // ---- BYPASS para rutas públicas y preflight ----
        final String ctx = req.getContextPath() == null ? "" : req.getContextPath();
        final String uri = req.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())
                || uri.startsWith(ctx + "/auth")
                || uri.startsWith(ctx + "/forgotPass")
                || uri.startsWith(ctx + "/updatePass")
                || uri.startsWith(ctx + "/validateUsr")
                || uri.startsWith(ctx + "/error")) {
            chain.doFilter(req, res);
            return;
        }

        // ---- Solo valida si realmente hay un JWT autenticado en el contexto ----
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();
            String jti = jwt.getId(); // "jti"

            if (jti != null && tokenService.isBlacklisted(jti)) {
                // Token revocado → 401
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                // Opcional: cuerpo breve para clientes que consumen JSON
                res.getWriter().write("{\"error\":\"invalid_token\",\"message\":\"Token revoked\"}");
                res.getWriter().flush();
                return;
            }
        }

        // Si no hay JWT o no está revocado, continúa el flujo normal
        chain.doFilter(req, res);
    }
}