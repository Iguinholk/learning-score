package br.com.learningscore.privacidade;

import br.com.learningscore.common.EntidadeBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** RF12: registro da solicitação de exclusão (sem FK, para sobreviver à exclusão da conta). */
@Entity
@Table(name = "solicitacao_exclusao")
@Getter
@Setter
public class SolicitacaoExclusao extends EntidadeBase {
    public enum Status { PENDENTE, CONCLUIDA, CANCELADA }

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.PENDENTE;

    @Column(name = "solicitada_em", nullable = false)
    private LocalDateTime solicitadaEm = LocalDateTime.now();

    @Column(name = "processada_em")
    private LocalDateTime processadaEm;
}
