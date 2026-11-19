package com.rastreoiot.seguridad;

import com.rastreoiot.modelo.Rol;
import com.rastreoiot.modelo.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration vigencia;

    public JwtService(@Value("${rastreo.jwt.secreto}") String secreto,
                      @Value("${rastreo.jwt.horas-vigencia:12}") long horasVigencia) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 32 caracteres (variable JWT_SECRETO)");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.vigencia = Duration.ofHours(horasVigencia);
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        var constructor = Jwts.builder()
                .subject(usuario.getId())
                .claim("correo", usuario.getCorreo())
                .claim("nombre", usuario.getNombre())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave);
        if (usuario.getEmpresaId() != null) {
            constructor.claim("empresaId", usuario.getEmpresaId());
        }
        return constructor.compact();
    }

    /** Devuelve la sesión si el token es válido y no ha expirado. */
    public Optional<SesionUsuario> validarToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new SesionUsuario(
                    claims.getSubject(),
                    claims.get("correo", String.class),
                    claims.get("nombre", String.class),
                    Rol.valueOf(claims.get("rol", String.class)),
                    claims.get("empresaId", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
