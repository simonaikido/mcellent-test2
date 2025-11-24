package com.bim.seif.security;

import com.bim.seif.models.Cliente;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccessTokenFactory {

    private final @Qualifier("hsJwtEncoder") JwtEncoder encoder;

    /** Emite token con: sub, p (roles), no, ap, am + jti */
    public Jwt encodeAccessToken(Authentication auth, int ttlMinutes) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        // p = authorities como lista (igual que tu TokenService)
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String no = null, ap = null, am = null;
        if (auth.getPrincipal() instanceof Cliente c) {
            no = c.getNombre();
            ap = c.getApellido_paterno();
            am = c.getApellido_materno();
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(auth.getName())                 // sub
                .issuedAt(now)
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .id(jti)                                  // jti para blacklist
                .claim("p", roles)                        // p = authorities (lista)
                .claim("no", no)
                .claim("ap", ap)
                .claim("am", am)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims));
    }
}