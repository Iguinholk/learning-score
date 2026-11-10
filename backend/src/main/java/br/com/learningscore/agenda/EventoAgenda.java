package br.com.learningscore.agenda;

import br.com.learningscore.common.EntidadeBase;
import br.com.learningscore.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "evento_agenda")
@Getter
@Setter
public class EventoAgenda extends EntidadeBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private LocalDate data;

    private LocalTime horario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaEvento categoria = CategoriaEvento.COMPROMISSO;

    @Column(length = 1000)
    private String observacoes;
}
