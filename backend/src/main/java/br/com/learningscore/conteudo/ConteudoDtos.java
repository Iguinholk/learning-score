package br.com.learningscore.conteudo;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ConteudoDtos {
    private ConteudoDtos() {}
    public record Request(
            @NotBlank @Size(max = 160) String titulo,
            @Size(max = 1000) String descricao,
            @Size(max = 300) String urlVideo,
            @Size(max = 500) String palavrasChave,
            @NotBlank @Email @Size(max = 180) @Pattern(regexp = "[^\\r\\n]+") String emailProfessor) {}

    // Catálogo público aos usuários autenticados: não expõe e-mails, tokens ou respostas particulares.
    public record Response(Long id, String titulo, String descricao, String urlEmbed, String palavrasChave,
                           boolean aprovado, String aprovadoPor) {
        public static Response de(Conteudo c) {
            return new Response(c.getId(), c.getTitulo(), c.getDescricao(), c.getUrlEmbed(), c.getPalavrasChave(),
                    c.isAprovado(), c.getAprovadoPor());
        }
    }
    public record SugestaoResponse(Long id, String titulo, String descricao, String urlVideo, String palavrasChave,
            String emailProfessor, StatusAvaliacao status, String justificativa, List<String> linksSugeridos,
            Instant respondidoEm, Instant expiraEm, boolean emailConviteEnviado, boolean emailResultadoEnviado) {
        static SugestaoResponse de(Conteudo c) {
            return new SugestaoResponse(c.getId(), c.getTitulo(), c.getDescricao(), c.getUrlVideo(), c.getPalavrasChave(),
                    c.getEmailProfessor(), c.getStatusAvaliacao(), c.getJustificativa(), LinksAvaliacao.extrair(c.getJustificativa()),
                    c.getRespondidoEm(), c.getTokenExpiraEm(), c.isEmailConviteEnviado(), c.isEmailResultadoEnviado());
        }
    }
    public enum Decisao { APROVAR, RECUSAR }
    public record ConfirmacaoRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @NotNull Decisao decisao,
            @NotBlank @Size(max = 4000) String justificativa) {}
    public record AvaliacaoResponse(String titulo, String descricao, String urlVideo, String palavrasChave, Instant expiraEm) {}
}
