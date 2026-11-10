package br.com.learningscore.dadosacademicos;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Mensalidade ou outra cobrança da instituição, cadastrada manualmente (RF04, 1ª versão). */
@Entity
@Table(name = "registro_financeiro")
@Getter
@Setter
public class RegistroFinanceiro extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 160)
    private String descricao;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusFinanceiro status = StatusFinanceiro.PENDENTE;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;
}
