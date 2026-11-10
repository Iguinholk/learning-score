package br.com.learningscore.notificacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class NotificacaoDtos {
    private NotificacaoDtos() {}

    public record Response(Long id, TipoNotificacao tipo, String titulo, String mensagem, boolean lida, LocalDateTime criadaEm) {
        static Response de(Notificacao n) {
            return new Response(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(), n.isLida(), n.getCriadoEm());
        }
    }

    public record Listagem(List<Response> itens, long naoLidas) {}

    public record PreferenciaItem(@NotNull TipoNotificacao tipo, String rotulo, boolean ativa) {}

    public record PreferenciasRequest(@NotEmpty List<@Valid PreferenciaItem> preferencias) {}
}
