package br.uepb.filasus.fila;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Check-in na recepção (US-10 - SCRUM-154).
 * Só o perfil RECEPCIONISTA acessa (/api/recepcao/** no SecurityConfig).
 *
 * Respostas:
 * - 200 com o resultado da busca ou com a senha confirmada
 * - 400 com { "mensagem": "..." } quando a busca está vazia ou não é CPF nem número
 * - 404 quando a senha não existe
 * - 409 quando a presença já foi confirmada ou a senha saiu da fila
 */
@RestController
@RequestMapping("/api/recepcao")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/check-in/busca")
    public BuscaCheckInResponse buscar(@Valid @RequestBody BuscaCheckInRequest request) {
        return checkInService.buscar(request.busca());
    }

    @PutMapping("/senhas/{senhaId}/check-in")
    public SenhaCheckInResponse confirmarPresenca(@PathVariable Long senhaId) {
        return checkInService.confirmarPresenca(senhaId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(MethodArgumentNotValidException exception) {
        String mensagem = exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(BuscaInvalidaException.class)
    public ResponseEntity<Map<String, String>> buscaInvalida(BuscaInvalidaException exception) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(SenhaNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> naoEncontrada(SenhaNaoEncontradaException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(CheckInNaoPermitidoException.class)
    public ResponseEntity<Map<String, String>> naoPermitido(CheckInNaoPermitidoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }
}
