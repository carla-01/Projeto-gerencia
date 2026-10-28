package br.uepb.filasus.recepcao;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recepcao")
public class RecepcaoController {

    @GetMapping("/fila")
    public ResponseEntity<FilaRecepcaoResponse> filaDoDia(Authentication autenticacao) {
        // TODO(SCRUM-US): substituir pelo cadastro de UBS e pela fila persistida da recepcionista.
        return ResponseEntity.ok(new FilaRecepcaoResponse(
            autenticacao.getName(),
            List.of()));
    }

    public record FilaRecepcaoResponse(String usuarioId, List<Object> fila) {
    }
}
