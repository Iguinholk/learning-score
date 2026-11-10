package br.com.learningscore.agenda;

import br.com.learningscore.agenda.AgendaDtos.*;
import br.com.learningscore.atividade.Atividade;
import br.com.learningscore.atividade.AtividadeRepository;
import br.com.learningscore.atividade.AtividadeService;
import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** RF05/RF06: calendário mensal (prazos + eventos) e CRUD de eventos personalizados. */
@Service
@RequiredArgsConstructor
public class AgendaService {
    private final EventoAgendaRepository eventos;
    private final AtividadeRepository atividades;
    private final AtividadeService atividadeService;
    private final UsuarioRepository usuarios;

    @Transactional
    public List<ItemCalendario> mes(Long uid, int ano, int mes) {
        atividadeService.marcarAtrasadas(uid);
        YearMonth ym = YearMonth.of(ano, mes);
        List<ItemCalendario> itens = new ArrayList<>();
        for (Atividade a : atividades.findByUsuarioIdAndPrazoBetween(uid,
                ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay())) {
            itens.add(new ItemCalendario("ATIVIDADE", a.getId(), a.getTitulo(), a.getDescricao(),
                    a.getPrazo().toLocalDate(), a.getPrazo().toLocalTime(), "PRAZO_ENTREGA",
                    a.getStatus().name(), a.getLinkEntrega()));
        }
        for (EventoAgenda e : eventos.findByUsuarioIdAndDataBetweenOrderByDataAscHorarioAsc(uid, ym.atDay(1), ym.atEndOfMonth())) {
            itens.add(new ItemCalendario("EVENTO", e.getId(), e.getTitulo(), e.getDescricao(), e.getData(),
                    e.getHorario(), e.getCategoria().name(), null, null));
        }
        itens.sort(Comparator.comparing(ItemCalendario::data)
                .thenComparing(ItemCalendario::horario, Comparator.nullsFirst(Comparator.naturalOrder())));
        return itens;
    }

    @Transactional
    public EventoResponse criar(Long uid, EventoRequest r) {
        EventoAgenda e = new EventoAgenda();
        e.setUsuario(usuarios.getReferenceById(uid));
        aplicar(e, r);
        return EventoResponse.de(eventos.save(e));
    }

    @Transactional(readOnly = true)
    public EventoResponse buscar(Long uid, Long id) { return EventoResponse.de(obter(uid, id)); }

    @Transactional
    public EventoResponse atualizar(Long uid, Long id, EventoRequest r) {
        EventoAgenda e = obter(uid, id);
        aplicar(e, r);
        return EventoResponse.de(e);
    }

    @Transactional
    public void excluir(Long uid, Long id) { eventos.delete(obter(uid, id)); }

    private EventoAgenda obter(Long uid, Long id) {
        return eventos.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado"));
    }

    private void aplicar(EventoAgenda e, EventoRequest r) {
        e.setTitulo(r.titulo().trim());
        e.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        e.setData(r.data());
        e.setHorario(r.horario());
        e.setCategoria(r.categoria());
        e.setObservacoes(r.observacoes() == null || r.observacoes().isBlank() ? null : r.observacoes().trim());
    }
}
