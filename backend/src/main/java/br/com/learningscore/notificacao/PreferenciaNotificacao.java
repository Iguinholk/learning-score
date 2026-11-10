package br.com.learningscore.notificacao;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** RF08: quais tipos de notificação o estudante deseja receber (persistido no banco). */
@Entity
@Table(name = "preferencia_notificacao")
@Getter
@Setter
public class PreferenciaNotificacao extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacao tipo;

    @Column(nullable = false)
    private boolean ativa = true;
}
