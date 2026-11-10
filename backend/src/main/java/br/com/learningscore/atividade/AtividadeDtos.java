package br.com.learningscore.atividade;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class AtividadeDtos {
    private AtividadeDtos() {}

    public record Request(
            @NotBlank @Size(max = 160) String titulo,
            @Size(max = 2000) String descricao,
            Long disciplinaId,
            @NotNull LocalDateTime prazo,
            Prioridade prioridade,
            StatusAtividade status,
            @Size(max = 500) @Pattern(regexp = "^(https?://\\S+)?$", message = "O link deve começar com http:// ou https://") String linkEntrega) {}

    public record Response(Long id, String titulo, String descricao, Long disciplinaId, String disciplinaNome,
                           LocalDateTime prazo, Prioridade prioridade, StatusAtividade status, String linkEntrega) {
        public static Response de(Atividade a) {
            return new Response(a.getId(), a.getTitulo(), a.getDescricao(),
                    a.getDisciplina() == null ? null : a.getDisciplina().getId(),
                    a.getDisciplina() == null ? null : a.getDisciplina().getNome(),
                    a.getPrazo(), a.getPrioridade(), a.getStatus(), a.getLinkEntrega());
        }
    }

    public record Pagina(java.util.List<Response> itens, int pagina, int tamanho, long total, int totalPaginas) {}
}
