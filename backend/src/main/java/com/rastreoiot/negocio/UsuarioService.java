package com.rastreoiot.negocio;

import com.rastreoiot.datos.EmpresaRepository;
import com.rastreoiot.datos.UsuarioRepository;
import com.rastreoiot.modelo.Empresa;
import com.rastreoiot.modelo.PreferenciaNotificacion;
import com.rastreoiot.modelo.Rol;
import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.modelo.Usuario;
import com.rastreoiot.negocio.dto.EmpresaRequest;
import com.rastreoiot.negocio.dto.PerfilRequest;
import com.rastreoiot.negocio.dto.PerfilResponse;
import com.rastreoiot.negocio.dto.UsuarioNuevoRequest;
import com.rastreoiot.negocio.excepciones.NegocioException;
import com.rastreoiot.negocio.excepciones.PermisosInsuficientesException;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.negocio.excepciones.ValidacionException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Perfil, preferencias y administración de usuarios (HU-02, HU-03). */
@Service
public class UsuarioService {

    private static final long TAMANO_MAXIMO_FOTO = 5L * 1024 * 1024;
    private static final Set<String> FORMATOS_FOTO = Set.of("image/jpeg", "image/png", "image/webp");

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ControlAcceso controlAcceso;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
                          ControlAcceso controlAcceso, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.controlAcceso = controlAcceso;
        this.passwordEncoder = passwordEncoder;
    }

    public PerfilResponse perfil(SesionUsuario sesion) {
        return respuesta(buscar(sesion.id()));
    }

    public PerfilResponse obtener(SesionUsuario sesion, String usuarioId) {
        controlAcceso.verificarUsuario(sesion, usuarioId);
        return respuesta(buscar(usuarioId));
    }

    public PerfilResponse actualizarPerfil(SesionUsuario sesion, PerfilRequest req) {
        Usuario usuario = buscar(sesion.id());
        usuario.setNombre(req.nombre().trim());
        usuario.setTelefono(req.telefono().trim());
        usuario.setCiudad(req.ciudad() == null || req.ciudad().isBlank() ? null : req.ciudad().trim());
        if (req.contactosEmergencia() != null) {
            usuario.setContactosEmergencia(new ArrayList<>(req.contactosEmergencia()));
        }
        if (req.preferencias() != null) {
            usuario.setPreferencias(combinarPreferencias(req.preferencias()));
        }
        return respuesta(usuarioRepository.save(usuario));
    }

    public PerfilResponse actualizarFoto(SesionUsuario sesion, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidacionException("Selecciona una imagen para tu foto de perfil.");
        }
        if (!FORMATOS_FOTO.contains(archivo.getContentType())) {
            throw new ValidacionException("La foto debe ser una imagen JPG, PNG o WEBP.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_FOTO) {
            throw new ValidacionException("La foto no puede superar 5 MB.");
        }
        Usuario usuario = buscar(sesion.id());
        try {
            usuario.setFotoPerfil("data:" + archivo.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(archivo.getBytes()));
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer la imagen. Inténtalo de nuevo.");
        }
        return respuesta(usuarioRepository.save(usuario));
    }

    /** Lista de usuarios visible según el rol: todos (SuperAdmin) o los de su empresa (Admin B2B). */
    public List<PerfilResponse> listar(SesionUsuario sesion) {
        if (sesion.esUsuarioHogar()) {
            throw new PermisosInsuficientesException("Permisos insuficientes: no puedes consultar otras cuentas.");
        }
        List<Usuario> usuarios = sesion.esSuperAdmin()
                ? usuarioRepository.findAll()
                : usuarioRepository.findByEmpresaId(sesion.empresaId());
        Map<String, String> empresas = nombresEmpresas();
        return usuarios.stream().map(u -> PerfilResponse.de(u, empresas.get(u.getEmpresaId()))).toList();
    }

    public PerfilResponse crearUsuario(SesionUsuario sesion, UsuarioNuevoRequest req) {
        if (!sesion.esSuperAdmin()) {
            throw new PermisosInsuficientesException();
        }
        String correo = AuthService.normalizarCorreo(req.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe una cuenta registrada con el correo " + correo + ".");
        }
        if (req.rol() == Rol.ADMIN_FLOTA_B2B && (req.empresaId() == null || !empresaRepository.existsById(req.empresaId()))) {
            throw new ValidacionException("Un administrador de flota debe pertenecer a una empresa válida.");
        }
        Usuario usuario = new Usuario(req.nombre().trim(), correo, passwordEncoder.encode(req.contrasena()), req.telefono(), req.rol());
        usuario.setEmpresaId(req.rol() == Rol.ADMIN_FLOTA_B2B ? req.empresaId() : null);
        return respuesta(usuarioRepository.save(usuario));
    }

    public List<Empresa> listarEmpresas(SesionUsuario sesion) {
        if (sesion.esSuperAdmin()) {
            return empresaRepository.findAll();
        }
        if (sesion.esAdminFlota() && sesion.empresaId() != null) {
            return empresaRepository.findById(sesion.empresaId()).map(List::of).orElse(List.of());
        }
        throw new PermisosInsuficientesException();
    }

    public Empresa crearEmpresa(SesionUsuario sesion, EmpresaRequest req) {
        if (!sesion.esSuperAdmin()) {
            throw new PermisosInsuficientesException();
        }
        if (empresaRepository.findByNit(req.nit().trim()).isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe una empresa con el NIT " + req.nit() + ".");
        }
        return empresaRepository.save(new Empresa(req.nombre().trim(), req.nit().trim()));
    }

    private List<PreferenciaNotificacion> combinarPreferencias(List<PreferenciaNotificacion> recibidas) {
        Map<TipoAlerta, Boolean> mapa = recibidas.stream()
                .collect(Collectors.toMap(PreferenciaNotificacion::getTipoAlerta, PreferenciaNotificacion::isActiva, (a, b) -> b));
        List<PreferenciaNotificacion> resultado = new ArrayList<>();
        for (TipoAlerta tipo : TipoAlerta.values()) {
            resultado.add(new PreferenciaNotificacion(tipo, mapa.getOrDefault(tipo, true)));
        }
        return resultado;
    }

    private Map<String, String> nombresEmpresas() {
        return empresaRepository.findAll().stream().collect(Collectors.toMap(Empresa::getId, Empresa::getNombre));
    }

    private Usuario buscar(String id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }

    private PerfilResponse respuesta(Usuario u) {
        String empresa = u.getEmpresaId() == null ? null
                : empresaRepository.findById(u.getEmpresaId()).map(Empresa::getNombre).orElse(null);
        return PerfilResponse.de(u, empresa);
    }
}
