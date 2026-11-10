package br.com.learningscore.common;

/** 429. Usada no limite de 1 sincronização manual por minuto (RN03). */
public class LimiteExcedidoException extends RuntimeException {
    private final long segundosRestantes;
    public LimiteExcedidoException(String mensagem, long segundosRestantes) {
        super(mensagem);
        this.segundosRestantes = segundosRestantes;
    }
    public long getSegundosRestantes() { return segundosRestantes; }
}
