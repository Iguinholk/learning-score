package br.com.learningscore.privacidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class PrivacidadeDtos {
    private PrivacidadeDtos() {}

    public record ConsentimentoRequest(@NotNull TipoConsentimento tipo, boolean concedido) {}
    public record ConsentimentoResponse(TipoConsentimento tipo, boolean concedido, LocalDateTime registradoEm) {}
    public record ExclusaoRequest(@NotBlank String senha) {}
    public record ExclusaoResponse(String status, LocalDateTime solicitadaEm, boolean emailEnviado, String mensagem) {}

    /** Cópia dos dados pessoais armazenados (direito de acesso — LGPD art. 18). */
    public record MeusDados(Map<String, Object> conta, Map<String, Integer> quantidades,
                            List<ConsentimentoResponse> consentimentos, ExclusaoResponse exclusao) {}
}
