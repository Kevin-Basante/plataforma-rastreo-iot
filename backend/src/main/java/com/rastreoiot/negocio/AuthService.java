package com.rastreoiot.negocio;

import com.rastreoiot.datos.EmpresaRepository;
import com.rastreoiot.datos.UsuarioRepository;
import com.rastreoiot.modelo.Empresa;
import com.rastreoiot.modelo.Rol;
import com.rastreoiot.modelo.Usuario;
import com.rastreoiot.negocio.dto.AuthResponse;
import com.rastreoiot.negocio.dto.LoginRequest;
import com.rastreoiot.negocio.dto.PerfilResponse;
import com.rastreoiot.negocio.dto.RegistroRequest;
import com.rastreoiot.negocio.excepciones.NegocioException;
import com.rastreoiot.seguridad.JwtService;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Registro e inicio de sesión seguro (HU-01). */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Crea una cuenta de Usuario Hogar B2C y devuelve su credencial de acceso. */
    public AuthResponse registrar(RegistroRequest req) {
        String correo = normalizarCorreo(req.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe una cuenta registrada con el correo " + correo + ".");
        }
        Usuario usuario = new Usuario(req.nombre().trim(), correo, passwordEncoder.encode(req.contrasena()),
                req.telefono().trim(), Rol.USUARIO_HOGAR_B2C);
        usuario = usuarioRepository.save(usuario);
        return respuesta(usuario);
    }

    public AuthResponse iniciarSesion(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(req.correo()))
                .filter(u -> passwordEncoder.matches(req.contrasena(), u.getContrasenaHash()))
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED,
                        "Correo o contraseña incorrectos. Verifica tus datos e inténtalo de nuevo."));
        return respuesta(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        String empresa = usuario.getEmpresaId() == null ? null
                : empresaRepository.findById(usuario.getEmpresaId()).map(Empresa::getNombre).orElse(null);
        return new AuthResponse(jwtService.generarToken(usuario), PerfilResponse.de(usuario, empresa));
    }

    static String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
