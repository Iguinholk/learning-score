package br.com.learningscore.recomendacao;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.conteudo.Conteudo;
import br.com.learningscore.dadosacademicos.Disciplina;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "recomendacao")
@Getter
@Setter
public class Recomendacao extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteudo_id")
    private Conteudo conteudo;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal compatibilidade;
}
