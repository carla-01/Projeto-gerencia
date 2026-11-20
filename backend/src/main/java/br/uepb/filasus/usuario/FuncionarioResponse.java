package br.uepb.filasus.usuario;

/** Funcionário salvo (US03). Nunca devolve a senha nem o hash dela. */
public record FuncionarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo) {

    public FuncionarioResponse(Usuario usuario) {
        this(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), usuario.isAtivo());
    }
}
