package br.com.learningscore.integracao;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** RN01: um estudante (identidade única) pode associar várias plataformas externas. */
@Entity
@Table(name = "plataforma_externa")
@Getter
@Setter
public class PlataformaExterna extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "identificador_externo", length = 120)
    private String identificadorExterno;

    /** TRUE = não há integração real: a sincronização é apenas uma simulação identificada como tal. */
    @Column(nullable = false)
    private boolean simulada = true;

    @Column(nullable = false)
    private boolean ativa = true;
}
