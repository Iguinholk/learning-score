package br.com.learningscore.notificacao;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notificacao")
@Getter
@Setter
public class Notificacao extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida;

    /** Identifica o fato gerador (ex.: "prazo:12:2026-11-10") para não repetir a notificação. */
    @Column(name = "chave_unica", nullable = false, length = 120)
    private String chaveUnica;
}
