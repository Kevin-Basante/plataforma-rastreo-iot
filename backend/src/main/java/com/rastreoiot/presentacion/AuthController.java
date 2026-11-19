package com.rastreoiot.presentacion;

import com.rastreoiot.negocio.AuthService;
import com.rastreoiot.negocio.dto.AuthResponse;
import com.rastreoiot.negocio.dto.LoginRequest;
import com.rastreoiot.negocio.dto.RegistroRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "HU-01 Registro e inicio de sesión seguro")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un Usuario Hogar B2C")
    public AuthResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener un token JWT")
    public AuthResponse iniciarSesion(@Valid @RequestBody LoginRequest req) {
        return authService.iniciarSesion(req);
    }
}
