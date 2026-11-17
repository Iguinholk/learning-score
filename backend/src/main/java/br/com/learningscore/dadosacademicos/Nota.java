package br.com.learningscore.dadosacademicos;

import br.com.learningscore.common.EntidadeBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "nota")
@Getter
@Setter
public class Nota extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @Column(nullable = false, length = 120)
    private String descricao;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_avaliacao", nullable = false, precision = 4, scale = 2)
    private BigDecimal valorAvaliacao = BigDecimal.TEN;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal peso = BigDecimal.ONE;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate dataLancamento;
}
