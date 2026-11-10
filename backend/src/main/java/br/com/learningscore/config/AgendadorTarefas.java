package br.com.learningscore.config;

import br.com.learningscore.atividade.AtividadeRepository;
import br.com.learningscore.atividade.AtividadeService;
import br.com.learningscore.atividade.StatusAtividade;
import br.com.learningscore.integracao.IntegracaoService;
import br.com.learningscore.notificacao.NotificacaoService;
import br.com.learningscore.privacidade.PrivacidadeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Tarefas periódicas do sistema. */
@Component
@RequiredArgsConstructor
public class AgendadorTarefas {
    private static final Logger log = LoggerFactory.getLogger(AgendadorTarefas.class);

    private final AtividadeRepository atividades;
    private final NotificacaoService notificacoes;
    private final IntegracaoService integracoes;
    private final PrivacidadeService privacidade;

    /** Marca como ATRASADA as atividades vencidas (a cada 10 minutos). */
    @Scheduled(fixedRate = 600_000, initialDelay = 30_000)
    @Transactional
    public void atualizarAtrasadas() {
        int n = atividades.marcarTodasAtrasadas(LocalDateTime.now(), AtividadeService.ABERTAS, StatusAtividade.ATRASADA);
        if (n > 0) log.info("{} atividade(s) marcada(s) como atrasada(s)", n);
    }

    /** Gera notificações de prazos, atrasos e eventos (a cada 30 minutos). */
    @Scheduled(fixedRate = 1_800_000, initialDelay = 60_000)
    public void gerarNotificacoes() { notificacoes.gerarParaTodos(); }

    /** RN02: sincronização automática a cada 6 horas. */
    @Scheduled(fixedRate = 21_600_000, initialDelay = 120_000)
    public void sincronizar() { integracoes.sincronizarAutomaticamente(); }

    /** RF12: processa exclusões de conta (a cada 10 minutos; o prazo máximo é 24 horas). */
    @Scheduled(fixedRate = 600_000, initialDelay = 45_000)
    public void processarExclusoes() { privacidade.processarPendentes(); }
}
