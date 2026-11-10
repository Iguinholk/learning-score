package br.com.learningscore.dashboard;

import br.com.learningscore.atividade.*;
import br.com.learningscore.dadosacademicos.*;
import br.com.learningscore.dashboard.DashboardDtos.*;
import br.com.learningscore.notificacao.NotificacaoRepository;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** RF01: dados do painel inicial em UMA chamada (para carregar em menos de 3 s). */
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final AtividadeRepository atividades;
    private final AtividadeService atividadeService;
    private final DisciplinaRepository disciplinas;
    private final RegistroFinanceiroRepository financeiro;
    private final NotificacaoRepository notificacoes;
    private final UsuarioRepository usuarios;

    @Transactional
    public Resposta montar(Long uid) {
        atividadeService.marcarAtrasadas(uid);
        List<Atividade> todas = atividades.findByUsuarioId(uid);
        LocalDateTime agora = LocalDateTime.now();

        List<Atividade> abertas = todas.stream().filter(a -> a.getStatus() != StatusAtividade.CONCLUIDA).toList();

        // Prioritárias: maior prioridade primeiro; empate = prazo mais próximo. Máximo de 5.
        List<AtividadeDtos.Response> prioritarias = abertas.stream()
                .sorted(Comparator.comparingInt((Atividade a) -> -a.getPrioridade().getPeso())
                        .thenComparing(Atividade::getPrazo))
                .limit(5).map(AtividadeDtos.Response::de).toList();

        List<AtividadeDtos.Response> proximas = abertas.stream()
                .filter(a -> !a.getPrazo().isBefore(agora))
                .sorted(Comparator.comparing(Atividade::getPrazo))
                .limit(5).map(AtividadeDtos.Response::de).toList();

        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusAtividade s : StatusAtividade.values()) porStatus.put(s.name(), 0L);
        todas.forEach(a -> porStatus.merge(a.getStatus().name(), 1L, Long::sum));

        List<Disciplina> discs = disciplinas.findByUsuarioIdOrderByNomeAsc(uid);
        List<MediaDisciplina> medias = new ArrayList<>();
        BigDecimal somaMedias = BigDecimal.ZERO, somaFreq = BigDecimal.ZERO;
        int comMedia = 0, comFreq = 0;
        for (Disciplina d : discs) {
            BigDecimal m = DisciplinaService.calcularMedia(d.getNotas());
            if (m != null) { medias.add(new MediaDisciplina(d.getNome(), m)); somaMedias = somaMedias.add(m); comMedia++; }
            if (d.getFrequenciaPercentual() != null) { somaFreq = somaFreq.add(d.getFrequenciaPercentual()); comFreq++; }
        }

        long concluidas = porStatus.get("CONCLUIDA");
        int taxa = todas.isEmpty() ? 0 : (int) Math.round(concluidas * 100.0 / todas.size());
        long em7dias = abertas.stream().filter(a -> !a.getPrazo().isBefore(agora) && a.getPrazo().isBefore(agora.plusDays(7))).count();

        LocalDate hoje = LocalDate.now();
        BigDecimal aberto = financeiro.findByUsuarioIdOrderByVencimentoDesc(uid).stream()
                .filter(r -> r.getStatus() == StatusFinanceiro.PENDENTE)
                .map(RegistroFinanceiro::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        Indicadores ind = new Indicadores(todas.size(), concluidas, porStatus.get("ATRASADA"), em7dias, taxa,
                comMedia == 0 ? null : somaMedias.divide(BigDecimal.valueOf(comMedia), 2, RoundingMode.HALF_UP),
                comFreq == 0 ? null : somaFreq.divide(BigDecimal.valueOf(comFreq), 1, RoundingMode.HALF_UP),
                aberto, notificacoes.countByUsuarioIdAndLidaFalse(uid));

        String nome = usuarios.findById(uid).orElseThrow().getNome();
        int hora = agora.getHour();
        String saud = hora < 12 ? "Bom dia" : hora < 18 ? "Boa tarde" : "Boa noite";
        return new Resposta(saud, nome.split(" ")[0], prioritarias, proximas, porStatus, medias, ind);
    }
}
