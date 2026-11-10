package br.com.learningscore.integracao;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class IntegracaoDtos {
    private IntegracaoDtos() {}

    public record PlataformaRequest(@NotBlank @Size(max = 80) String nome,
                                    @NotBlank @Size(max = 30) String tipo,
                                    @Size(max = 120) String identificadorExterno) {}

    public record PlataformaResponse(Long id, String nome, String tipo, String identificadorExterno,
                                     boolean simulada, boolean ativa, LocalDateTime ultimaSincronizacao, String ultimoStatus) {}

    public record HistoricoResponse(Long id, String plataforma, String tipo, String status, String mensagem,
                                    LocalDateTime iniciadoEm, LocalDateTime finalizadoEm) {
        static HistoricoResponse de(HistoricoSincronizacao h) {
            return new HistoricoResponse(h.getId(), h.getPlataforma().getNome(), h.getTipo().name(), h.getStatus().name(),
                    h.getMensagem(), h.getIniciadoEm(), h.getFinalizadoEm());
        }
    }
}
