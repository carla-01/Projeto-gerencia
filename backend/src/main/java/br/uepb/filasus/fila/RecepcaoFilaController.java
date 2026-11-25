package br.uepb.filasus.fila;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * A recepção coloca na fila o paciente que chegou sem senha (US-10 - SCRUM-155).
 * Só o perfil RECEPCIONISTA acessa (/api/recepcao/** no SecurityConfig).
 *
 * Respostas:
 * - 200 com os serviços e se a fila de cada um está aberta
 * - 201 com a senha gerada
 * - 400 com { "mensagem": "..." } sem paciente ou sem serviço
 * - 404 quando o paciente ou o serviço não existe
 * - 409 com a fila fechada (horário ou limite do dia) ou se o paciente já tem senha no serviço
 */
@RestController
@RequestMapping("/api/recepcao")
public class RecepcaoFilaController {

    static final String MENSAGEM_JA_TEM_SENHA = "Este paciente já tem uma senha ativa neste serviço";
    static final String MENSAGEM_TENTE_DE_NOVO = "Não foi possível gerar a senha. Tente novamente";

    private final EntradaFilaService entradaFilaService;

    public RecepcaoFilaController(EntradaFilaService entradaFilaService) {
        this.entradaFilaService = entradaFilaService;
    }

    @GetMapping("/servicos")
    public List<ServicoDisponivelResponse> servicos() {
        return entradaFilaService.servicosDisponiveis();
    }

    @PostMapping("/senhas")
    public ResponseEntity<SenhaResponse> colocarNaFila(@Valid @RequestBody SenhaRecepcaoRequest request) {
        SenhaResponse senha = entradaFilaService.entrarPelaRecepcao(request.pacienteId(), request.servicoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(senha);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(MethodArgumentNotValidException exception) {
        String mensagem = exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler({ServicoNaoEncontradoException.class, PacienteNaoEncontradoException.class})
    public ResponseEntity<Map<String, String>> naoEncontrado(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(FilaFechadaException.class)
    public ResponseEntity<Map<String, String>> filaFechada(FilaFechadaException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }

    /** A mensagem da exceção fala com o paciente ("Você já tem..."): aqui quem lê é a recepção. */
    @ExceptionHandler(SenhaAtivaExistenteException.class)
    public ResponseEntity<Map<String, String>> jaTemSenha(SenhaAtivaExistenteException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", MENSAGEM_JA_TEM_SENHA));
    }

    /** Só chega aqui se o número repetiu nas três tentativas (muitas entradas ao mesmo tempo). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> entradaSimultanea(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", MENSAGEM_TENTE_DE_NOVO));
    }
}
