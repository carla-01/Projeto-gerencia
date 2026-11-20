package br.uepb.filasus.usuario;

import java.util.Comparator;

import br.uepb.filasus.fila.Servico;

/**
 * Funcionário salvo ou listado (US03). Nunca devolve senha, CPF nem cartão SUS.
 * setorId e setor ficam vazios para quem não é profissional.
 */
public record FuncionarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo,
                                  Long setorId, String setor) {

    public FuncionarioResponse(Usuario usuario) {
        this(usuario, usuario.getSetores().stream().min(Comparator.comparing(Servico::getId)).orElse(null));
    }

    private FuncionarioResponse(Usuario usuario, Servico setor) {
        this(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), usuario.isAtivo(),
            setor == null ? null : setor.getId(), setor == null ? null : setor.getNome());
    }
}
