package br.com.learningscore.agenda;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public final class AgendaDtos {
    private AgendaDtos() {}

    public record EventoRequest(
            @NotBlank @Size(max = 160) String titulo,
            @Size(max = 2000) String descricao,
            @NotNull LocalDate data,
            LocalTime horario,
            @NotNull CategoriaEvento categoria,
            @Size(max = 1000) String observacoes) {}

    public record EventoResponse(Long id, String titulo, String descricao, LocalDate data, LocalTime horario,
                                 CategoriaEvento categoria, String observacoes) {
        static EventoResponse de(EventoAgenda e) {
            return new EventoResponse(e.getId(), e.getTitulo(), e.getDescricao(), e.getData(), e.getHorario(),
                    e.getCategoria(), e.getObservacoes());
        }
    }

    /** Item do calendário: origem = ATIVIDADE (prazo de entrega) ou EVENTO (cadastrado pelo estudante). */
    public record ItemCalendario(String origem, Long id, String titulo, String descricao, LocalDate data,
                                 LocalTime horario, String categoria, String status, String link) {}
}
