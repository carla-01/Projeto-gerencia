package br.uepb.filasus.fila;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cancelamento da senha pelo paciente (US-08 - SCRUM-43). Só o perfil PACIENTE acessa (SecurityConfig).
 */
@RestController
@RequestMapping("/api/paciente/senhas")
public class CancelamentoSenhaController {

    private final CancelamentoSenhaService cancelamentoSenhaService;

    public CancelamentoSenhaController(CancelamentoSenhaService cancelamentoSenhaService) {
        this.cancelamentoSenhaService = cancelamentoSenhaService;
    }

    @PostMapping("/{senhaId}/cancelar")
    public SenhaResponse cancelar(@PathVariable Long senhaId, Authentication autenticacao) {
        // O "sub" do token é o id do usuário (JwtAuthenticationFilter).
        Long usuarioId = Long.valueOf(autenticacao.getName());
        return cancelamentoSenhaService.cancelar(usuarioId, senhaId);
    }

    @ExceptionHandler({SenhaNaoEncontradaException.class, PacienteNaoEncontradoException.class})
    public ResponseEntity<Map<String, String>> naoEncontrada(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(CancelamentoNaoPermitidoException.class)
    public ResponseEntity<Map<String, String>> naoPodeCancelar(CancelamentoNaoPermitidoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }
}
