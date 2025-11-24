package com.bim.seif.services;

import com.bim.seif.dto.AuthRequest;
import com.bim.seif.dto.AuthResponse;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.repositories.CustomerRepository;
import com.bim.seif.security.AccessTokenFactory;
import com.bim.seif.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${auth.code.expiration}")
    private int AUTH_OTP_EXPIRATION;
    @Value("${auth.block.expiration}")
    private int AUTH_BLOCK_EXPIRATION;
    @Value("${auth.block.attemps}")
    private int AUTH_BLOCK_ATTEMPS;

    @Value("${security.oauth2.access-token.ttl-minutes:10}") // 5–15
    private int accessTtlMinutes;

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService; // SOLO para refresh
    private final OtpService otpService;
    private final EmailService emailService;
    private final CustomerRepository customerRepository;

    private final AccessTokenFactory accessTokenFactory;
    private final RefreshTokenService refreshTokenService;

    private final String OTP_KEY = "OTP";
    private final String INTENTOS_KEY = "INTENTOS";

    /** Login + OTP → access + refresh */
    public AuthResponse autenticar(AuthRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsuario(), request.getPassword()));

        final String uid = request.getUsuario();
        final String otpNsKey = "OTP:" + uid; // namespace para separar intentos de OTP

        // Si ya está bloqueado por OTP, niega login
        if (getIntentosActuales(otpNsKey) >= AUTH_BLOCK_ATTEMPS) {
            // Opcional: aquí podrías notificar por correo si lo deseas.
            return null; // Controller mandará 401
        }

        // Validar OTP (sin borrar en fallo)
        if (!validarOTP(uid, request.getOtp())) {
            Long intentos = agregarIntentoFallido(otpNsKey);
            if (intentos >= AUTH_BLOCK_ATTEMPS) {
                // Llegó al límite → bloqueado por OTP
                return null; // Controller mandará 401
            }
            // Aún con intentos disponibles → unauthorized
            return null; // Controller mandará 401
        }

        // OTP correcto → limpiar intentos de OTP
        borrarIntentosFallidos(otpNsKey);

        var jwt = accessTokenFactory.encodeAccessToken(auth, accessTtlMinutes);
        String refresh = refreshTokenService.createRefreshToken(uid);

        int expiresIn = jwt.getExpiresAt() != null
                ? (int) java.time.Duration.between(java.time.Instant.now(), jwt.getExpiresAt()).getSeconds()
                : accessTtlMinutes * 60;

        return AuthResponse.builder()
                .token(jwt.getTokenValue())
                .refreshToken(refresh)
                .expiresIn(expiresIn)
                .build();
    }

    /** Refresh con rotación (consume el RT y emite par nuevo) */
    public AuthResponse refresh(String refreshToken) {
        // Consume RT (rotación: invalida el usado)
        String username = refreshTokenService.consumeRefreshToken(refreshToken);
        if (username == null) {
            log.warn("Refresh rechazado: RT inexistente/ya consumido/expirado");
            return null;
        }

        try {
            // reconstruye auth con roles actuales
            var ud = userDetailsService.loadUserByUsername(username);
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, "N/A", ud.getAuthorities());

            var jwt = accessTokenFactory.encodeAccessToken(auth, accessTtlMinutes);
            String newRefresh = refreshTokenService.createRefreshToken(username);

            int expiresIn = jwt.getExpiresAt() != null
                    ? (int) java.time.Duration.between(java.time.Instant.now(), jwt.getExpiresAt()).getSeconds()
                    : accessTtlMinutes * 60;

            String jti = jwt.getId(); // si no tienes jti, ver nota abajo
            log.info("Refresh OK: user={} expIn={}s{}",
                    username, expiresIn, (jti != null ? " jti=" + jti : ""));

            return AuthResponse.builder()
                    .token(jwt.getTokenValue())
                    .refreshToken(newRefresh)
                    .expiresIn(expiresIn)
                    .build();

        } catch (Exception ex) {
            log.error("Error al renovar sesión con RT (user={}): {}", username, ex.getMessage());
            return null;
        }
    }

    // ===== OTP
    public boolean validarOTP(String usuario, String otp) {
        boolean ok = compararOTP(usuario, otp);
        // Solo borra el OTP si fue correcto (permite hasta 3 intentos con el mismo
        // código o hasta que expire).
        if (ok) {
            borrarCodigoAutenticacion(usuario);
        }
        return ok;
    }

    public void enviarCodigoAutenticacion(AuthRequest request) throws Exception {
        var usuarioOpt = customerRepository.findByEmailAndFechaBajaIsNull(request.getUsuario());
        if (usuarioOpt.isEmpty())
            throw new Exception("Usuario no encontrado");

        if (agregarIntentoFallido(request.getUsuario()) < AUTH_BLOCK_ATTEMPS + 1) {
            try {
                // Usa el mismo provider/encoder que en /auth/credenciales
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsuario(),
                                request.getPassword()));
            } catch (org.springframework.security.core.AuthenticationException e) {
                throw new Exception("Credenciales invalidas", e);
            }
        } else {
            throw new DisabledException("Usuario Bloqueado");
        }

        borrarIntentosFallidos(request.getUsuario());

        String otp = generaNumeroAuntenticacion();
        log.info("OTP {} -> {}", request.getUsuario(), otp);
        guardarCodigoAutenticacion(request.getUsuario(), otp);
        emailService.enviarCorreo(request.getUsuario(), TipoEvento.seguridad_token_cliente,
                Map.of(Propiedad.otp, otp));
    }

    private void guardarCodigoAutenticacion(String key, String value) {
        otpService.setHashValue(key, OTP_KEY, value, AUTH_OTP_EXPIRATION, TimeUnit.MINUTES);
    }

    private Long agregarIntentoFallido(String key) {
        return otpService.incrementHashValue(key, INTENTOS_KEY, AUTH_BLOCK_EXPIRATION, TimeUnit.MINUTES);
    }

    private void borrarIntentosFallidos(String key) {
        otpService.deleteHashValue(key, INTENTOS_KEY);
    }

    private void borrarCodigoAutenticacion(String key) {
        otpService.deleteHashValue(key, OTP_KEY);
    }

    private boolean compararOTP(String user, String otp) {
        Map<String, Object> data = otpService.getUserData(user);
        String targetOTP = (String) data.get(OTP_KEY);
        return targetOTP != null && targetOTP.equalsIgnoreCase(otp);
    }

    private String generaNumeroAuntenticacion() {
        Random r = new Random();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 6; i++)
            s.append(r.nextInt(10));
        return s.toString();
    }

    /**
     * Genera un código OTP aleatorio de 6 dígitos, lo guarda en Redis con
     * expiración
     * y lo devuelve para ser usado en la autenticación por correo.
     */
    public String generarOtp(String usuario) {
        // Generar OTP de 6 dígitos numéricos
        Random random = new Random();
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int numero = random.nextInt(10); // 0-9
            codigo.append(numero);
        }

        String otp = codigo.toString();

        // Guardar OTP en Redis con expiración
        otpService.setHashValue(usuario, "OTP", otp, AUTH_OTP_EXPIRATION, TimeUnit.MINUTES);

        log.info("OTP generado para el usuario {}: {}", usuario, otp);
        return otp;
    }

    /**
     * Cierra sesión: coloca el JTI del access token en blacklist hasta que expire.
     */
    public void logout(org.springframework.security.oauth2.jwt.Jwt jwt) {
        if (jwt == null) {
            log.warn("Intento de logout sin token JWT activo.");
            return;
        }

        String jti = jwt.getId();
        var exp = jwt.getExpiresAt();

        if (jti == null || exp == null) {
            log.warn("JWT inválido o sin JTI/expiración al intentar logout.");
            return;
        }

        long secondsToLive = java.time.Duration.between(
                java.time.Instant.now(), exp).getSeconds();

        if (secondsToLive <= 0) {
            log.info("Token ya expiró, no se agrega a blacklist (jti={})", jti);
            return;
        }

        refreshTokenService.blacklistJti(jti, secondsToLive);
        log.info("JWT con jti={} agregado a blacklist por {} segundos", jti, secondsToLive);
    }

    private long getIntentosActuales(String key) {
        try {
            Map<String, Object> data = otpService.getUserData(key);
            Object v = (data != null) ? data.get(INTENTOS_KEY) : null;
            return (v == null) ? 0L : Long.parseLong(v.toString());
        } catch (Exception e) {
            log.warn("No se pudieron leer intentos para key {}: {}", key, e.getMessage());
            return 0L;
        }
    }
}