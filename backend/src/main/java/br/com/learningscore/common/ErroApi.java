package br.com.learningscore.common;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato padronizado de erro devolvido pela API. */
public record ErroApi(int status, String erro, String mensagem, Map<String, String> campos, LocalDateTime em) {
    public static ErroApi de(int status, String erro, String mensagem) {
        return new ErroApi(status, erro, mensagem, null, LocalDateTime.now());
    }
}
