package br.com.learningscore.common;

/** 422. Violação de uma regra de negócio (ex.: RN04, consentimento ausente). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) { super(mensagem); }
}
