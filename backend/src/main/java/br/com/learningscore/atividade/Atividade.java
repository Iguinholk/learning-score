package br.com.learningscore.atividade;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.dadosacademicos.Disciplina;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "atividade")
@Getter
@Setter
public class Atividade extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridade prioridade = Prioridade.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtividade status = StatusAtividade.PENDENTE;

    /** RF03: apenas um link externo; o sistema NÃO envia trabalhos a plataformas externas. */
    @Column(name = "link_entrega", length = 500)
    private String linkEntrega;
}
