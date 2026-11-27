package br.uepb.filasus.fila;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Prioridade legal marcada pela recepção (US-11 - SCRUM-158).
 * Só o perfil RECEPCIONISTA acessa (/api/recepcao/** no SecurityConfig).
 *
 * Respostas:
 * - 200 com a senha e a prioridade salva
 * - 400 com { "campo": "...", "mensagem": "..." } sem prioridade, com prioridade
 *   que não existe ou com urgência sem motivo ("Informe o motivo da urgência")
 * - 404 quando a senha não existe
 * - 409 quando a senha já saiu da fila (chamada, atendida, cancelada ou faltosa)
 */
@RestController
@RequestMapping("/api/recepcao/senhas")
public class PrioridadeSenhaController {

    private final PrioridadeSenhaService prioridadeSenhaService;

    public PrioridadeSenhaController(PrioridadeSenhaService prioridadeSenhaService) {
        this.prioridadeSenhaService = prioridadeSenhaService;
    }

    @PutMapping("/{senhaId}/prioridade")
    public PrioridadeSenhaResponse aplicar(@PathVariable Long senhaId,
                                           @Valid @RequestBody AplicarPrioridadeRequest request,
                                           Authentication autenticacao) {
        // O "sub" do token é o id do usuário (JwtAuthenticationFilter).
        Long recepcionistaId = Long.valueOf(autenticacao.getName());
        return prioridadeSenhaService.aplicar(senhaId, request, recepcionistaId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(MethodArgumentNotValidException exception) {
        FieldError erro = exception.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest().body(Map.of("campo", erro.getField(), "mensagem", erro.getDefaultMessage()));
    }

    /** Corpo ilegível, por exemplo uma prioridade que não existe ("prioridade": "VIP"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(
            Map.of("campo", "prioridade", "mensagem", PrioridadeSenhaService.MENSAGEM_PRIORIDADE_INVALIDA));
    }

    @ExceptionHandler(PrioridadeInvalidaException.class)
    public ResponseEntity<Map<String, String>> prioridadeInvalida(PrioridadeInvalidaException exception) {
        return ResponseEntity.badRequest().body(Map.of("campo", exception.getCampo(), "mensagem", exception.getMessage()));
    }

    @ExceptionHandler(SenhaNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> naoEncontrada(SenhaNaoEncontradaException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(SenhaForaDaFilaException.class)
    public ResponseEntity<Map<String, String>> foraDaFila(SenhaForaDaFilaException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }
}
