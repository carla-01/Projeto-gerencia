package br.uepb.filasus.fila;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Entrada na fila pelo paciente (US-06 - SCRUM-136). Só o perfil PACIENTE acessa (SecurityConfig).
 */
@RestController
@RequestMapping("/api/paciente")
public class EntradaFilaController {

    private final EntradaFilaService entradaFilaService;

    public EntradaFilaController(EntradaFilaService entradaFilaService) {
        this.entradaFilaService = entradaFilaService;
    }

    /** Serviços para a tela de escolha (SCRUM-137). */
    @GetMapping("/servicos")
    public List<ServicoDisponivelResponse> servicos() {
        return entradaFilaService.servicosDisponiveis();
    }

    @PostMapping("/senhas")
    public ResponseEntity<SenhaResponse> entrar(@Valid @RequestBody EntradaFilaRequest request,
                                                Authentication autenticacao) {
        // O "sub" do token é o id do usuário (JwtAuthenticationFilter).
        Long usuarioId = Long.valueOf(autenticacao.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(entradaFilaService.entrar(usuarioId, request.servicoId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(MethodArgumentNotValidException exception) {
        FieldError erro = exception.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest()
            .body(Map.of("campo", erro.getField(), "mensagem", erro.getDefaultMessage()));
    }

    @ExceptionHandler({ServicoNaoEncontradoException.class, PacienteNaoEncontradoException.class})
    public ResponseEntity<Map<String, String>> naoEncontrado(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler({FilaFechadaException.class, SenhaAtivaExistenteException.class})
    public ResponseEntity<Map<String, String>> naoPodeEntrar(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }

    /** Só chega aqui se o número repetiu nas três tentativas (muitas entradas ao mesmo tempo). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> entradaSimultanea(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("mensagem", "Não foi possível gerar a senha. Tente novamente"));
    }
}
