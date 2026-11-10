package br.com.learningscore.conteudo;

import jakarta.validation.constraints.*;

public final class ConteudoDtos {
    private ConteudoDtos() {}

    public record Request(
            @NotBlank @Size(max = 160) String titulo,
            @Size(max = 1000) String descricao,
            @Size(max = 300) String urlVideo,
            @Size(max = 500) String palavrasChave) {}

    public record Response(Long id, String titulo, String descricao, String urlEmbed, String palavrasChave,
                           boolean aprovado, String aprovadoPor) {
        static Response de(Conteudo c) {
            return new Response(c.getId(), c.getTitulo(), c.getDescricao(), c.getUrlEmbed(), c.getPalavrasChave(),
                    c.isAprovado(), c.getAprovadoPor());
        }
    }
}
