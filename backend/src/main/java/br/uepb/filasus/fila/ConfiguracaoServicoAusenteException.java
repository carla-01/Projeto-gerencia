package br.uepb.filasus.fila;

public class ConfiguracaoServicoAusenteException extends RuntimeException {

    public ConfiguracaoServicoAusenteException() {
        super("Configuração do serviço não encontrada");
    }
}
