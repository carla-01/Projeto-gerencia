package br.uepb.filasus.fila;

public class ServicoDuplicadoException extends RuntimeException {

    public ServicoDuplicadoException() {
        super("Serviço já cadastrado");
    }
}
