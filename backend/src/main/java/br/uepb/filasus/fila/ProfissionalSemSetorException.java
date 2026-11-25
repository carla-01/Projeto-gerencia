package br.uepb.filasus.fila;

/** O profissional não está ligado a nenhum serviço (profissional_servico), então não há fila para chamar. */
public class ProfissionalSemSetorException extends RuntimeException {

    public ProfissionalSemSetorException() {
        super("Seu usuário não tem setor. Peça ao gestor para configurar");
    }
}
