package br.uepb.filasus.fila;

/** O usuário logado tem perfil PACIENTE, mas não tem cadastro de paciente ligado ao login. */
public class PacienteNaoEncontradoException extends RuntimeException {

    public PacienteNaoEncontradoException() {
        super("Cadastro de paciente não encontrado");
    }
}
