package br.uepb.filasus.fila;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Chamar próximo" do profissional de saúde (US-13 - SCRUM-162).
 * Só o perfil PROFISSIONAL acessa (/api/profissional/** no SecurityConfig).
 *
 * Respostas:
 * - 200 com a senha chamada, ou com "Não há pacientes aguardando" quando não há ninguém presente
 * - 409 quando o profissional não tem setor
 */
@RestController
@RequestMapping("/api/profissional")
public class ChamadaController {

    private final ChamadaService chamadaService;

    public ChamadaController(ChamadaService chamadaService) {
        this.chamadaService = chamadaService;
    }

    @PostMapping("/chamadas")
    public ChamadaResponse chamarProximo(Authentication autenticacao) {
        // O "sub" do token é o id do usuário (JwtAuthenticationFilter).
        Long profissionalId = Long.valueOf(autenticacao.getName());
        return chamadaService.chamarProximo(profissionalId);
    }

    @ExceptionHandler(ProfissionalSemSetorException.class)
    public ResponseEntity<Map<String, String>> semSetor(ProfissionalSemSetorException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensagem", exception.getMessage()));
    }
}
