package br.uepb.filasus.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Endpoint de login (US01 - SCRUM-37).
 *
 * POST /auth/login
 * - 200 com { "token": "...", "perfil": "GESTOR" } quando dá certo
 * - 401 com { "mensagem": "CPF ou senha inválidos" } quando falha
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse resposta = authService.login(request);
        return ResponseEntity.ok(resposta);
    }

    @ExceptionHandler(LoginInvalidoException.class)
    public ResponseEntity<Map<String, String>> loginInvalido(LoginInvalidoException e) {
        Map<String, String> corpo = Map.of("mensagem", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(corpo);
    }
}
