package br.uepb.filasus.paciente;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Autocadastro do paciente (US-04 - SCRUM-39).
 *
 * POST /pacientes/autocadastro — rota pública: quem está se cadastrando
 * ainda não tem login. A US-02 (SCRUM-38) precisa deixar esta rota liberada.
 * - 201 com o paciente e o registro do aceite quando dá certo
 * - 400 com { "campo": "...", "mensagem": "..." } quando falta dado, o CPF é
 *   inválido ou o termo não foi aceito
 * - 409 com { "mensagem": "..." } quando o CPF ou o e-mail já estão cadastrados
 */
@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping("/autocadastro")
    public ResponseEntity<AutocadastroResponse> autocadastrar(@Valid @RequestBody AutocadastroRequest request) {
        AutocadastroResponse resposta = pacienteService.autocadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /** Erro de validação: devolve o primeiro campo com problema, para a tela marcar. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(MethodArgumentNotValidException e) {
        FieldError erro = e.getBindingResult().getFieldErrors().get(0);
        Map<String, String> corpo = Map.of("campo", erro.getField(), "mensagem", erro.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(CadastroInvalidoException.class)
    public ResponseEntity<Map<String, String>> cadastroInvalido(CadastroInvalidoException e) {
        Map<String, String> corpo = Map.of("campo", e.getCampo(), "mensagem", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(CadastroDuplicadoException.class)
    public ResponseEntity<Map<String, String>> cadastroDuplicado(CadastroDuplicadoException e) {
        Map<String, String> corpo = Map.of("mensagem", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpo);
    }

    /** Dois cadastros iguais ao mesmo tempo: o UNIQUE do banco barra o segundo. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> cadastroSimultaneo(DataIntegrityViolationException e) {
        Map<String, String> corpo = Map.of("mensagem", "Já existe um cadastro com este CPF ou e-mail");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpo);
    }
}
