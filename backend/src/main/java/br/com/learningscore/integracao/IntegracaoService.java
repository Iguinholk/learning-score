package br.com.learningscore.integracao;

import br.com.learningscore.common.LimiteExcedidoException;
import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.integracao.HistoricoSincronizacao.Status;
import br.com.learningscore.integracao.HistoricoSincronizacao.Tipo;
import br.com.learningscore.integracao.IntegracaoDtos.*;
import br.com.learningscore.privacidade.ConsentimentoService;
import br.com.learningscore.privacidade.TipoConsentimento;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * RF10 / RN01–RN04:
 *  - RN04: exige consentimento explícito antes de associar ou sincronizar plataformas;
 *  - RN03: sincronização manual limitada a 1 por minuto por estudante;
 *  - RN02: sincronização automática a cada 6 horas (ver AgendadorTarefas);
 *  - cada sincronização tem limite de 10 segundos.
 */
@Service
@RequiredArgsConstructor
public class IntegracaoService {
    private static final Logger log = LoggerFactory.getLogger(IntegracaoService.class);
    public static final Duration INTERVALO_MANUAL = Duration.ofMinutes(1);
    public static final Duration LIMITE_SINCRONIZACAO = Duration.ofSeconds(10);

    private final PlataformaExternaRepository plataformas;
    private final HistoricoSincronizacaoRepository historico;
    private final ConsentimentoService consentimentos;
    private final UsuarioRepository usuarios;
    private final List<ConectorPlataforma> conectores;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Transactional(readOnly = true)
    public List<PlataformaResponse> listar(Long uid) {
        return plataformas.findByUsuarioIdOrderByNomeAsc(uid).stream().map(p -> {
            var ultima = historico.findTop20ByUsuarioIdOrderByIniciadoEmDesc(uid).stream()
                    .filter(h -> h.getPlataforma().getId().equals(p.getId())).findFirst();
            return new PlataformaResponse(p.getId(), p.getNome(), p.getTipo(), p.getIdentificadorExterno(), p.isSimulada(),
                    p.isAtiva(), ultima.map(HistoricoSincronizacao::getIniciadoEm).orElse(null),
                    ultima.map(h -> h.getStatus().name()).orElse(null));
        }).toList();
    }

    @Transactional
    public PlataformaResponse associar(Long uid, PlataformaRequest r) {
        exigirConsentimento(uid);
        if (plataformas.existsByUsuarioIdAndNomeIgnoreCase(uid, r.nome().trim())) {
            throw new RegraNegocioException("Você já associou uma plataforma com este nome.");
        }
        PlataformaExterna p = new PlataformaExterna();
        p.setUsuario(usuarios.getReferenceById(uid));
        p.setNome(r.nome().trim());
        p.setTipo(r.tipo().trim().toUpperCase());
        p.setIdentificadorExterno(r.identificadorExterno());
        p.setSimulada(true);   // não há integração real disponível: tudo é simulação
        p = plataformas.save(p);
        return new PlataformaResponse(p.getId(), p.getNome(), p.getTipo(), p.getIdentificadorExterno(), true, true, null, null);
    }

    @Transactional
    public void remover(Long uid, Long id) {
        plataformas.delete(obter(uid, id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> historico(Long uid) {
        return historico.findTop20ByUsuarioIdOrderByIniciadoEmDesc(uid).stream().map(HistoricoResponse::de).toList();
    }

    /** Sincronização manual (RN03: no máximo 1 por minuto por estudante). */
    @Transactional(noRollbackFor = RuntimeException.class)
    public HistoricoResponse sincronizarManual(Long uid, Long plataformaId) {
        exigirConsentimento(uid);
        PlataformaExterna p = obter(uid, plataformaId);
        historico.findFirstByUsuarioIdAndTipoOrderByIniciadoEmDesc(uid, Tipo.MANUAL).ifPresent(ultima -> {
            Duration decorrido = Duration.between(ultima.getIniciadoEm(), LocalDateTime.now());
            if (decorrido.compareTo(INTERVALO_MANUAL) < 0) {
                long restante = INTERVALO_MANUAL.minus(decorrido).toSeconds() + 1;
                throw new LimiteExcedidoException("Aguarde " + restante + "s para sincronizar novamente (limite de 1 por minuto).", restante);
            }
        });
        return HistoricoResponse.de(executar(p, Tipo.MANUAL));
    }

    /** Sincronização automática (a cada 6 horas), apenas para quem concedeu consentimento. */
    @Transactional
    public void sincronizarAutomaticamente() {
        for (PlataformaExterna p : plataformas.findByAtivaTrue()) {
            Long uid = p.getUsuario().getId();
            if (!consentimentos.possui(uid, TipoConsentimento.INTEGRACOES)) continue;
            try {
                executar(p, Tipo.AUTOMATICA);
            } catch (Exception e) {
                log.warn("Falha na sincronização automática da plataforma {}: {}", p.getId(), e.getMessage());
            }
        }
    }

    private HistoricoSincronizacao executar(PlataformaExterna p, Tipo tipo) {
        HistoricoSincronizacao h = new HistoricoSincronizacao();
        h.setUsuario(p.getUsuario());
        h.setPlataforma(p);
        h.setTipo(tipo);
        h.setIniciadoEm(LocalDateTime.now());
        ConectorPlataforma conector = conectores.stream().filter(c -> c.suporta(p)).findFirst().orElse(null);
        if (conector == null) {
            h.setStatus(Status.FALHA);
            h.setMensagem("Nenhum conector disponível para esta plataforma.");
        } else {
            Future<String> f = executor.submit(() -> conector.sincronizar(p));
            try {
                h.setMensagem(f.get(LIMITE_SINCRONIZACAO.toSeconds(), TimeUnit.SECONDS));
                h.setStatus(Status.SUCESSO);
            } catch (TimeoutException e) {
                f.cancel(true);
                h.setStatus(Status.TIMEOUT);
                h.setMensagem("A sincronização excedeu 10 segundos.");
            } catch (Exception e) {
                h.setStatus(Status.FALHA);
                h.setMensagem("Falha na sincronização.");
            }
        }
        h.setFinalizadoEm(LocalDateTime.now());
        return historico.save(h);
    }

    private void exigirConsentimento(Long uid) {
        if (!consentimentos.possui(uid, TipoConsentimento.INTEGRACOES)) {
            throw new RegraNegocioException("É necessário conceder consentimento para integrações (menu Privacidade).");
        }
    }

    private PlataformaExterna obter(Long uid, Long id) {
        return plataformas.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plataforma não encontrada"));
    }
}
