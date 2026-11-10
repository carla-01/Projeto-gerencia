package br.uepb.filasus.fila;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Posição do paciente na fila (US-07 - SCRUM-139). Só o perfil PACIENTE acessa (SecurityConfig).
 */
@RestController
@RequestMapping("/api/paciente/senhas")
public class PosicaoFilaController {

    private final PosicaoFilaService posicaoFilaService;

    public PosicaoFilaController(PosicaoFilaService posicaoFilaService) {
        this.posicaoFilaService = posicaoFilaService;
    }

    /** Senhas ativas de hoje com a posição; lista vazia quando o paciente não está na fila. */
    @GetMapping
    public List<PosicaoFilaResponse> minhasSenhas(Authentication autenticacao) {
        // O "sub" do token é o id do usuário (JwtAuthenticationFilter).
        Long usuarioId = Long.valueOf(autenticacao.getName());
        return posicaoFilaService.minhasSenhas(usuarioId);
    }

    @ExceptionHandler(PacienteNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(PacienteNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }
}
