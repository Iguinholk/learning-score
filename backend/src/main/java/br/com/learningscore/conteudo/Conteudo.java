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

    @Column(name = "url_video", length = 300)
    private String urlVideo;

    @Column(name = "email_professor", length = 180)
    private String emailProfessor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_avaliacao", nullable = false, length = 16)
    private StatusAvaliacao statusAvaliacao = StatusAvaliacao.PENDENTE;

    @Column(name = "token_hash", length = 64, unique = true)
    private String tokenHash;

    @Column(name = "token_expira_em")
    private java.time.Instant tokenExpiraEm;

    @Column(name = "convite_em")
    private java.time.Instant conviteEm;

    @Column(name = "respondido_em")
    private java.time.Instant respondidoEm;

    @Column(length = 4000)
    private String justificativa;

    @Column(name = "email_convite_enviado", nullable = false)
    private boolean emailConviteEnviado;

    @Column(name = "email_resultado_enviado", nullable = false)
    private boolean emailResultadoEnviado;

    @Column(name = "sugerido_por")
    private Long sugeridoPor;
}
