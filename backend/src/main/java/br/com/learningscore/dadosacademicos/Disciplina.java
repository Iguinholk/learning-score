package br.com.learningscore.dadosacademicos;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disciplina")
@Getter
@Setter
public class Disciplina extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 120)
    private String professor;

    /** Palavras-chave separadas por vírgula; usadas no cálculo de compatibilidade (RF09). */
    @Column(name = "palavras_chave", nullable = false, length = 500)
    private String palavrasChave = "";

    @Column(name = "frequencia_percentual", precision = 5, scale = 2)
    private BigDecimal frequenciaPercentual;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataLancamento DESC")
    private List<Nota> notas = new ArrayList<>();
}
