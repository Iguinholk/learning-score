package br.com.learningscore.agenda;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoAgendaRepository extends JpaRepository<EventoAgenda, Long> {
    Optional<EventoAgenda> findByIdAndUsuarioId(Long id, Long usuarioId);
    List<EventoAgenda> findByUsuarioIdAndDataBetweenOrderByDataAscHorarioAsc(Long usuarioId, LocalDate de, LocalDate ate);
}
