package br.com.learningscore.conteudo;

import br.com.learningscore.common.EntidadeBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Conteúdo educacional. Só aparece como "aprovado" depois da verificação de um professor (RF09). */
@Entity
@Table(name = "conteudo")
@Getter
@Setter
public class Conteudo extends EntidadeBase {
    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    /** Apenas o endereço do vídeo incorporado — nunca armazenamos o vídeo (RF11). */
    @Column(name = "url_embed", length = 300)
    private String urlEmbed;

    @Column(name = "palavras_chave", nullable = false, length = 500)
    private String palavrasChave = "";

    @Column(nullable = false)
    private boolean aprovado;

    @Column(name = "aprovado_por", length = 120)
    private String aprovadoPor;

    @Column(name = "sugerido_por")
    private Long sugeridoPor;
}
