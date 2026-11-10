package br.com.learningscore.privacidade;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** RN04: consentimento explícito (guardamos o valor atual e quando foi registrado). */
@Entity
@Table(name = "consentimento")
@Getter
@Setter
public class Consentimento extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoConsentimento tipo;

    @Column(nullable = false)
    private boolean concedido;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm = LocalDateTime.now();
}
