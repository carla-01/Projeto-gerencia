package br.uepb.filasus.recepcao;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uepb.filasus.paciente.CadastroInvalidoException;
import br.uepb.filasus.paciente.CadastroPresencialRequest;
import br.uepb.filasus.paciente.CadastroPresencialResponse;
import br.uepb.filasus.paciente.PacienteService;
import jakarta.validation.Valid;

/**
 * Cadastro de paciente pela recepção (US-05 - SCRUM-40).
 *
 * POST /api/recepcao/pacientes — só para o perfil RECEPCIONISTA (SecurityConfig).
 * - 201 com o paciente e o registro do consentimento verbal quando cria o cadastro
 * - 200 com o cadastro existente (jaCadastrado = true) quando o CPF já existe,
 *   sem criar outro (CT-US05-02)
 * - 400 com { "campo": "...", "mensagem": "..." } quando falta dado, o CPF é
 *   inválido ou o consentimento verbal não foi registrado
 * - 401 sem login e 403 para os outros perfis (SecurityConfig)
 */
@RestController
@RequestMapping("/api/recepcao/pacientes")
public class RecepcaoPacienteController {

    private final PacienteService pacienteService;

    public RecepcaoPacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    public ResponseEntity<CadastroPresencialResponse> cadastrar(@Valid @RequestBody CadastroPresencialRequest request,
                                                                Authentication autenticacao) {
        // O filtro JWT coloca o id do usuário logado como "nome" da autenticação.
        Long recepcionistaId = Long.valueOf(autenticacao.getName());

        CadastroPresencialResponse resposta = pacienteService.cadastrarPelaRecepcao(request, recepcionistaId);

        if (resposta.jaCadastrado()) {
            return ResponseEntity.ok(resposta);
        }
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

    /** Dois cadastros iguais ao mesmo tempo: o UNIQUE do banco barra o segundo. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> cadastroSimultaneo(DataIntegrityViolationException e) {
        Map<String, String> corpo = Map.of("mensagem", "Já existe um paciente com este CPF");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpo);
    }
}
