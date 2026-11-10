package br.com.learningscore.privacidade;

import br.com.learningscore.agenda.EventoAgendaRepository;
import br.com.learningscore.atividade.AtividadeRepository;
import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.dadosacademicos.DisciplinaRepository;
import br.com.learningscore.dadosacademicos.RegistroFinanceiroRepository;
import br.com.learningscore.integracao.PlataformaExterna;
import br.com.learningscore.integracao.PlataformaExternaRepository;
import br.com.learningscore.privacidade.PrivacidadeDtos.*;
import br.com.learningscore.privacidade.SolicitacaoExclusao.Status;
import br.com.learningscore.usuario.Usuario;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RF12 / LGPD: consulta de dados, consentimentos e exclusão de conta. */
@Service
@RequiredArgsConstructor
public class PrivacidadeService {
    private static final Logger log = LoggerFactory.getLogger(PrivacidadeService.class);

    private final UsuarioRepository usuarios;
    private final ConsentimentoRepository consentimentos;
    private final ConsentimentoService consentimentoService;
    private final SolicitacaoExclusaoRepository solicitacoes;
    private final PlataformaExternaRepository plataformas;
    private final DisciplinaRepository disciplinas;
    private final AtividadeRepository atividades;
    private final EventoAgendaRepository eventos;
    private final RegistroFinanceiroRepository financeiro;
    private final PasswordEncoder encoder;
    private final EmailService email;

    @Value("${learningscore.exclusao.atraso-minutos}")
    private long atrasoMinutos;

    @Transactional(readOnly = true)
    public MeusDados meusDados(Long uid) {
        Usuario u = usuarios.findById(uid).orElseThrow();
        Map<String, Object> conta = new LinkedHashMap<>();
        conta.put("id", u.getId());
        conta.put("nome", u.getNome());
        conta.put("email", u.getEmail());
        conta.put("papel", u.getPapel().name());
        conta.put("criadoEm", u.getCriadoEm());
        Map<String, Integer> q = new LinkedHashMap<>();
        q.put("disciplinas", disciplinas.findByUsuarioIdOrderByNomeAsc(uid).size());
        q.put("atividades", atividades.findByUsuarioId(uid).size());
        q.put("registrosFinanceiros", financeiro.findByUsuarioIdOrderByVencimentoDesc(uid).size());
        q.put("plataformasExternas", plataformas.findByUsuarioIdOrderByNomeAsc(uid).size());
        return new MeusDados(conta, q, listarConsentimentos(uid), situacaoExclusao(uid));
    }

    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> listarConsentimentos(Long uid) {
        return consentimentos.findByUsuarioId(uid).stream()
                .map(c -> new ConsentimentoResponse(c.getTipo(), c.isConcedido(), c.getRegistradoEm())).toList();
    }

    @Transactional
    public List<ConsentimentoResponse> definirConsentimento(Long uid, ConsentimentoRequest r) {
        if (r.tipo() == TipoConsentimento.TERMOS_USO && !r.concedido()) {
            throw new RegraNegocioException("Para revogar os termos de uso, solicite a exclusão da conta.");
        }
        consentimentoService.registrar(uid, r.tipo(), r.concedido());
        if (r.tipo() == TipoConsentimento.INTEGRACOES && !r.concedido()) {
            // Revogar consentimento interrompe as sincronizações
            plataformas.findByUsuarioIdOrderByNomeAsc(uid).forEach(p -> p.setAtiva(false));
        }
        return listarConsentimentos(uid);
    }

    @Transactional
    public ExclusaoResponse solicitarExclusao(Long uid, String senha) {
        Usuario u = usuarios.findById(uid).orElseThrow();
        if (!encoder.matches(senha, u.getSenhaHash())) {
            throw new RegraNegocioException("Senha incorreta.");
        }
        if (solicitacoes.findFirstByUsuarioIdAndStatus(uid, Status.PENDENTE).isPresent()) {
            throw new RegraNegocioException("Já existe uma solicitação de exclusão em andamento.");
        }
        SolicitacaoExclusao s = new SolicitacaoExclusao();
        s.setUsuarioId(uid);
        s.setEmailHash(sha256(u.getEmail()));
        solicitacoes.save(s);
        boolean enviado = email.enviar(u.getEmail(), "Learning Score — solicitação de exclusão recebida",
                "Olá, " + u.getNome() + ".\n\nRecebemos sua solicitação de exclusão de conta. Ela será processada em até 24 horas. "
                + "Para cancelar, acesse Privacidade e conta antes do processamento.\n\nLearning Score");
        return new ExclusaoResponse("PENDENTE", s.getSolicitadaEm(), enviado,
                enviado ? "Enviamos uma confirmação por e-mail." : "Solicitação registrada. (E-mail não configurado neste ambiente.)");
    }

    @Transactional
    public void cancelarExclusao(Long uid) {
        SolicitacaoExclusao s = solicitacoes.findFirstByUsuarioIdAndStatus(uid, Status.PENDENTE)
                .orElseThrow(() -> new RegraNegocioException("Não há solicitação pendente."));
        s.setStatus(Status.CANCELADA);
        s.setProcessadaEm(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public ExclusaoResponse situacaoExclusao(Long uid) {
        return solicitacoes.findFirstByUsuarioIdAndStatus(uid, Status.PENDENTE)
                .map(s -> new ExclusaoResponse("PENDENTE", s.getSolicitadaEm(), false,
                        "Sua conta será excluída em até 24 horas."))
                .orElse(null);
    }

    /** Executado pelo agendador: exclui as contas cujas solicitações já passaram da janela de cancelamento. */
    @Transactional
    public int processarPendentes() {
        List<SolicitacaoExclusao> prontas = solicitacoes.findByStatusAndSolicitadaEmBefore(
                Status.PENDENTE, LocalDateTime.now().minusMinutes(atrasoMinutos));
        for (SolicitacaoExclusao s : prontas) {
            usuarios.deleteById(s.getUsuarioId());      // ON DELETE CASCADE remove os dados pessoais
            s.setStatus(Status.CONCLUIDA);
            s.setProcessadaEm(LocalDateTime.now());
            log.info("Conta {} excluída (solicitação {})", s.getUsuarioId(), s.getId());
        }
        return prontas.size();
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
