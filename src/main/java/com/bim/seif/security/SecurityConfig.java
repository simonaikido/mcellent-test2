package com.bim.seif.security;

import com.bim.seif.config.AuthEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.*;
import java.util.stream.Collectors;

/** Encoder: el raw password YA es SHA-256 (hex); aquí sólo aplicamos BCrypt */
class PreHashedSha256HexBcryptPasswordEncoder implements PasswordEncoder {
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder bcrypt =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @Override public String encode(CharSequence sha256Hex) {
        return bcrypt.encode(sha256Hex.toString());
    }
    @Override public boolean matches(CharSequence sha256Hex, String encodedPassword) {
        return bcrypt.matches(sha256Hex.toString(), encodedPassword);
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    @Autowired(required = false)
    private AuthEntryPoint authEntryPoint;

    /** Estáticos reales fuera de Spring Security */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().antMatchers(
                "/index.html", "/favicon.ico",
                "/img/**", "/assets/**", "/static/**",
                "/**/*.js", "/**/*.css", "/**/*.map",
                "/**/*.png", "/**/*.jpg", "/**/*.svg",
                "/**/*.woff", "/**/*.woff2"
        );
    }

    @Bean
    public SecurityFilterChain spaChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/", "/ext", "/ext/**", "/ext/index.html").permitAll()
                .antMatchers(
                    "/forgotPass/**",
                    "/updatePass/**",
                    "/validateUsr/**",
                    "/auth/**",
                    "/ping/**",
                    "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**", "/metrics/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                authEntryPoint != null
                    ? authEntryPoint
                    : new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                        org.springframework.http.HttpStatus.UNAUTHORIZED)))
            // Deja habilitado el resource server para los endpoints que requieren token
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /** CORS global */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(Collections.singletonList("http://localhost:4200"));
        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(Collections.singletonList("*"));
        cfg.setAllowCredentials(true);
        cfg.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    /** Convierte claim “p” → ROLE_* */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object p = jwt.getClaim("p");
            List<String> roles = new ArrayList<>();
            if (p instanceof String s) roles.add(s);
            else if (p instanceof Collection<?> col)
                col.forEach(o -> { if (o instanceof String s2) roles.add(s2); });
            return roles.stream()
                .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> s.startsWith("ROLE_") ? s : "ROLE_" + s)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        });
        return conv;
    }

    /** Encoder: DB debe guardar BCrypt(sha256HexDelFront) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PreHashedSha256HexBcryptPasswordEncoder();
    }

    /**
     * AuthenticationManager explícito con DaoAuthenticationProvider
     * para garantizar que use el mismo PasswordEncoder.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}