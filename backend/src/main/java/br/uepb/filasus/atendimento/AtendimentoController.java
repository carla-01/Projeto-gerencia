package br.uepb.filasus.atendimento;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uepb.filasus.fila.ProfissionalSemSetorException;
import br.uepb.filasus.fila.SenhaNaoEncontradaException;

/**
 * Iniciar e finalizar o atendimento (US-14 - SCRUM-167).
 * Só o perfil PROFISSIONAL acessa (/api/profissional/** no SecurityConfig).
 *
 * Respostas:
 * - 200 com o atendimento e os horários gravados
 * - 404 quando a senha não existe ou é de outro setor
 * - 409 quando a senha não está na situação certa, por exemplo
 *   "Inicie o atendimento antes de finalizar" (Cenário 2), ou quando o profissional não tem setor
 */
@RestController
@RequestMapping("/api/profissional/atendimentos")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    public AtendimentoController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @PostMapping("/{senhaId}/inicio")
    public AtendimentoResponse iniciar(Authentication autenticacao, @PathVariable Long senhaId) {
        // O "sub" do token é o id do usuário (JwtAuthenticationFilter).
        return atendimentoService.iniciar(Long.valueOf(autenticacao.getName()), senhaId);
    }

    @PostMapping("/{senhaId}/fim")
    public AtendimentoResponse finalizar(Authentication autenticacao, @PathVariable Long senhaId) {
        return atendimentoService.finalizar(Long.valueOf(autenticacao.getName()), senhaId);
    }

    @ExceptionHandler(SenhaNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> naoEncontrada(SenhaNaoEncontradaException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", exception.getMessage()));
    }

    @ExceptionHandler({AtendimentoNaoPermitidoException.class, ProfissionalSemSetorException.class})
    public ResponseEntity<Map<String, String>> naoPermitido(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }
}
