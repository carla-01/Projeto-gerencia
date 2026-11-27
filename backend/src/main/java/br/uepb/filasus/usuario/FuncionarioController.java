package br.uepb.filasus.usuario;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Cadastro de funcionários (US03 - SCRUM-15). Só o GESTOR acessa (SecurityConfig). */
@RestController
@RequestMapping("/api/gestor/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @PostMapping
    public ResponseEntity<FuncionarioResponse> cadastrar(@Valid @RequestBody CadastroFuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.cadastrar(request));
    }

    /** Lista de usuários da UBS (Cenário 1): a equipe, com o setor de cada profissional. */
    @GetMapping
    public List<FuncionarioResponse> listar() {
        return funcionarioService.listar();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(MethodArgumentNotValidException exception) {
        FieldError erro = exception.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest().body(Map.of("campo", erro.getField(), "mensagem", erro.getDefaultMessage()));
    }

    /** Corpo ilegível, por exemplo um papel que não existe ("perfil": "ADMIN"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Dados inválidos"));
    }

    @ExceptionHandler(PerfilNaoPermitidoException.class)
    public ResponseEntity<Map<String, String>> perfilNaoPermitido(PerfilNaoPermitidoException exception) {
        return ResponseEntity.badRequest().body(Map.of("campo", "perfil", "mensagem", exception.getMessage()));
    }

    @ExceptionHandler(SetorInvalidoException.class)
    public ResponseEntity<Map<String, String>> setorInvalido(SetorInvalidoException exception) {
        return ResponseEntity.badRequest().body(Map.of("campo", "servicoId", "mensagem", exception.getMessage()));
    }

    @ExceptionHandler(CpfInvalidoException.class)
    public ResponseEntity<Map<String, String>> cpfInvalido(CpfInvalidoException exception) {
        return ResponseEntity.badRequest().body(Map.of("campo", "cpf", "mensagem", exception.getMessage()));
    }

    @ExceptionHandler(CpfJaCadastradoException.class)
    public ResponseEntity<Map<String, String>> cpfJaCadastrado(CpfJaCadastradoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, String>> emailJaCadastrado(EmailJaCadastradoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }

    /**
     * Dois cadastros com o mesmo e-mail ou CPF ao mesmo tempo: a UNIQUE do banco barra o segundo.
     * Não dá para saber qual campo repetiu sem ler a mensagem do banco, então a resposta é genérica.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> cadastroSimultaneo(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", "E-mail ou CPF já cadastrado"));
    }
}
