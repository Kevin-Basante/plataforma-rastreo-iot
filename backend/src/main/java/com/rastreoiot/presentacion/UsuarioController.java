package com.rastreoiot.presentacion;

import com.rastreoiot.modelo.Empresa;
import com.rastreoiot.negocio.UsuarioService;
import com.rastreoiot.negocio.dto.EmpresaRequest;
import com.rastreoiot.negocio.dto.PerfilRequest;
import com.rastreoiot.negocio.dto.PerfilResponse;
import com.rastreoiot.negocio.dto.UsuarioNuevoRequest;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "HU-02 Permisos por rol y HU-03 Datos personales y preferencias")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar el perfil propio")
    public PerfilResponse perfil() {
        return usuarioService.perfil(SesionActual.obtener());
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar datos personales, contactos de emergencia y preferencias")
    public PerfilResponse actualizar(@Valid @RequestBody PerfilRequest req) {
        return usuarioService.actualizarPerfil(SesionActual.obtener(), req);
    }

    @PutMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cambiar la foto de perfil (JPG, PNG o WEBP de hasta 5 MB)")
    public PerfilResponse actualizarFoto(@RequestParam("archivo") MultipartFile archivo) {
        return usuarioService.actualizarFoto(SesionActual.obtener(), archivo);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios (SuperAdmin: todos; Admin B2B: los de su empresa)")
    public List<PerfilResponse> listar() {
        return usuarioService.listar(SesionActual.obtener());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario respetando los permisos por rol")
    public PerfilResponse obtener(@PathVariable String id) {
        return usuarioService.obtener(SesionActual.obtener(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un usuario (solo SuperAdmin)")
    public PerfilResponse crear(@Valid @RequestBody UsuarioNuevoRequest req) {
        return usuarioService.crearUsuario(SesionActual.obtener(), req);
    }

    @GetMapping("/empresas")
    @Operation(summary = "Listar empresas visibles para el usuario")
    public List<Empresa> empresas() {
        return usuarioService.listarEmpresas(SesionActual.obtener());
    }

    @PostMapping("/empresas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar una empresa cliente (solo SuperAdmin)")
    public Empresa crearEmpresa(@Valid @RequestBody EmpresaRequest req) {
        return usuarioService.crearEmpresa(SesionActual.obtener(), req);
    }
}
