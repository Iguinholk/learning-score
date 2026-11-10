package br.com.learningscore.notificacao;

import br.com.learningscore.agenda.EventoAgenda;
import br.com.learningscore.agenda.EventoAgendaRepository;
import br.com.learningscore.atividade.Atividade;
import br.com.learningscore.atividade.AtividadeRepository;
import br.com.learningscore.atividade.AtividadeService;
import br.com.learningscore.atividade.StatusAtividade;
import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.notificacao.NotificacaoDtos.*;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/** RF07/RF08: notificações internas, geradas por fatos reais (prazos, atrasos, eventos, notas) e filtradas pelas preferências. */
@Service
@RequiredArgsConstructor
public class NotificacaoService {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final NotificacaoRepository repo;
    private final PreferenciaNotificacaoRepository preferencias;
    private final AtividadeRepository atividades;
    private final EventoAgendaRepository eventos;
    private final UsuarioRepository usuarios;

    /** Sem preferência gravada = ativa (padrão). */
    public boolean estaAtiva(Long uid, TipoNotificacao tipo) {
        return preferencias.findByUsuarioIdAndTipo(uid, tipo).map(PreferenciaNotificacao::isAtiva).orElse(true);
    }

    @Transactional
    public void criarSePermitido(Long uid, TipoNotificacao tipo, String titulo, String mensagem, String chave) {
        if (!estaAtiva(uid, tipo) || repo.existsByUsuarioIdAndChaveUnica(uid, chave)) return;
        Notificacao n = new Notificacao();
        n.setUsuario(usuarios.getReferenceById(uid));
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem.length() > 500 ? mensagem.substring(0, 500) : mensagem);
        n.setChaveUnica(chave);
        repo.save(n);
    }

    /** Verifica prazos, atrasos e eventos do estudante e cria as notificações que ainda não existem. */
    @Transactional
    public void gerarPara(Long uid) {
        LocalDateTime agora = LocalDateTime.now();
        // Prazos nas próximas 48 horas
        for (Atividade a : atividades.findByUsuarioIdAndStatusInAndPrazoBetween(uid, AtividadeService.ABERTAS, agora, agora.plusHours(48))) {
            criarSePermitido(uid, TipoNotificacao.PRAZO_PROXIMO, "Prazo próximo",
                    "\"" + a.getTitulo() + "\" vence em " + a.getPrazo().format(FORMATO) + ".",
                    "prazo:" + a.getId() + ":" + a.getPrazo().toLocalDate());
        }
        // Atrasadas
        atividades.marcarAtrasadas(uid, agora, AtividadeService.ABERTAS, StatusAtividade.ATRASADA);
        for (Atividade a : atividades.findByUsuarioIdAndStatusIn(uid, List.of(StatusAtividade.ATRASADA))) {
            criarSePermitido(uid, TipoNotificacao.ATIVIDADE_ATRASADA, "Atividade atrasada",
                    "\"" + a.getTitulo() + "\" venceu em " + a.getPrazo().format(FORMATO) + ".", "atraso:" + a.getId());
        }
        // Eventos nos próximos 3 dias
        LocalDate hoje = LocalDate.now();
        for (EventoAgenda e : eventos.findByUsuarioIdAndDataBetweenOrderByDataAscHorarioAsc(uid, hoje, hoje.plusDays(3))) {
            criarSePermitido(uid, TipoNotificacao.EVENTO_FUTURO, "Evento em breve",
                    "\"" + e.getTitulo() + "\" em " + e.getData().format(DateTimeFormatter.ofPattern("dd/MM")) + ".",
                    "evento:" + e.getId());
        }
    }

    @Transactional
    public Listagem listar(Long uid) {
        gerarPara(uid);
        return new Listagem(repo.findByUsuarioIdOrderByCriadoEmDesc(uid).stream().map(Response::de).toList(),
                repo.countByUsuarioIdAndLidaFalse(uid));
    }

    @Transactional
    public Response marcarLida(Long uid, Long id, boolean lida) {
        Notificacao n = repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada"));
        n.setLida(lida);
        return Response.de(n);
    }

    @Transactional
    public void marcarTodasLidas(Long uid) {
        repo.findByUsuarioIdOrderByCriadoEmDesc(uid).forEach(n -> n.setLida(true));
    }

    @Transactional
    public void excluir(Long uid, Long id) {
        repo.delete(repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada")));
    }

    @Transactional(readOnly = true)
    public List<PreferenciaItem> listarPreferencias(Long uid) {
        return Arrays.stream(TipoNotificacao.values())
                .map(t -> new PreferenciaItem(t, t.getRotulo(), estaAtiva(uid, t))).toList();
    }

    @Transactional
    public List<PreferenciaItem> salvarPreferencias(Long uid, PreferenciasRequest req) {
        for (PreferenciaItem item : req.preferencias()) {
            PreferenciaNotificacao p = preferencias.findByUsuarioIdAndTipo(uid, item.tipo()).orElseGet(() -> {
                PreferenciaNotificacao nova = new PreferenciaNotificacao();
                nova.setUsuario(usuarios.getReferenceById(uid));
                nova.setTipo(item.tipo());
                return nova;
            });
            p.setAtiva(item.ativa());
            preferencias.save(p);
        }
        return listarPreferencias(uid);
    }

    /** Usado pelo agendador para todos os usuários ativos. */
    @Transactional
    public void gerarParaTodos() {
        usuarios.findAll().stream().filter(u -> u.isAtivo()).forEach(u -> gerarPara(u.getId()));
    }
}
