package br.com.learningscore.common;

/** 404. Também usada quando o recurso existe mas pertence a outro estudante (não vazamos a existência). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) { super(mensagem); }
}
