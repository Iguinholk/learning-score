package br.com.learningscore.conteudo;
import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.conteudo.ConteudoDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
@RequiredArgsConstructor
public class ConteudoService {
    private static final Pattern YOUTUBE = Pattern.compile(
            "^https://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})(?:[?&#].*)?$");
    private static final Pattern VIMEO = Pattern.compile("^https://(?:www\\.)?vimeo\\.com/(\\d{6,12})(?:[/?#].*)?$");

    private static final SecureRandom RANDOM = new SecureRandom();
    private final ConteudoRepository repo;
    private final ApplicationEventPublisher eventos;
    @Value("${learningscore.avaliacao.validade-horas:72}")
    private long validadeHoras = 72;
    public record Convite(Long id, String token) {}
    public record Resultado(Long id) {}
    /** Converte links comuns em URL de incorporação segura. Aceita apenas YouTube e Vimeo (lista de permissão). */
    public static String paraEmbed(String url) {
        if (url == null || url.isBlank()) return null;
        String u = url.trim();
        Matcher y = YOUTUBE.matcher(u);
        if (y.matches()) return "https://www.youtube-nocookie.com/embed/" + y.group(1);
        Matcher v = VIMEO.matcher(u);
        if (v.matches()) return "https://player.vimeo.com/video/" + v.group(1);
        throw new RegraNegocioException("Use um link https do YouTube ou do Vimeo.");
    }

    @Transactional(readOnly = true)
    public List<Response> aprovados() {
        return repo.findByAprovadoTrueOrderByTituloAsc().stream().map(Response::de).toList();
    }
    @Transactional(readOnly = true)
    public List<SugestaoResponse> minhas(Long uid) {
        return repo.findBySugeridoPorOrderByCriadoEmDesc(uid).stream().map(SugestaoResponse::de).toList();
    }
    @Transactional
    public SugestaoResponse sugerir(Long uid, Request r) {
        Conteudo c = new Conteudo();
        c.setTitulo(r.titulo().trim());
        c.setDescricao(r.descricao());
        c.setUrlEmbed(paraEmbed(r.urlVideo()));
        c.setUrlVideo(r.urlVideo() == null || r.urlVideo().isBlank() ? null : r.urlVideo().trim());
        c.setPalavrasChave(r.palavrasChave() == null ? "" : r.palavrasChave().trim());
        c.setEmailProfessor(r.emailProfessor().trim().toLowerCase(Locale.ROOT));
        c.setSugeridoPor(uid);
        c.setAprovado(false);
        c.setStatusAvaliacao(StatusAvaliacao.PENDENTE);
        String token = novoConvite(c);
        c = repo.save(c);
        eventos.publishEvent(new Convite(c.getId(), token));
        return SugestaoResponse.de(c);
    }
    @Transactional
    public void reenviar(Long uid, Long id) {
        Conteudo c = repo.bloquearMinhaSugestao(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sugestão não encontrada"));
        if (c.getStatusAvaliacao() != StatusAvaliacao.PENDENTE || c.getEmailProfessor() == null)
            throw new RegraNegocioException("Esta sugestão não permite reenvio. Sugestões antigas precisam ser cadastradas novamente com o e-mail do professor.");
        if (c.getConviteEm() != null && c.getConviteEm().plusSeconds(60).isAfter(Instant.now()))
            throw new RegraNegocioException("Aguarde um minuto para reenviar o convite.");
        eventos.publishEvent(new Convite(c.getId(), novoConvite(c)));
    }
    private String novoConvite(Conteudo c) {
        if (validadeHoras < 1 || validadeHoras > 168) throw new IllegalStateException("AVALIACAO_VALIDADE_HORAS deve estar entre 1 e 168.");
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        c.setTokenHash(hashToken(token));
        c.setConviteEm(Instant.now());
        c.setTokenExpiraEm(c.getConviteEm().plus(Duration.ofHours(validadeHoras)));
        c.setEmailConviteEnviado(false);
        return token;
    }
    public static String hashToken(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw linkInvalido();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static RegraNegocioException linkInvalido() {
        return new RegraNegocioException("Link inválido, expirado ou já utilizado. Peça ao estudante um novo convite.");
    }
    private static void verificar(Conteudo c) {
        if (c.getStatusAvaliacao() != StatusAvaliacao.PENDENTE || c.getTokenExpiraEm() == null
                || !c.getTokenExpiraEm().isAfter(Instant.now())) throw linkInvalido();
    }
    @Transactional(readOnly = true)
    public AvaliacaoResponse consultar(String token) {
        Conteudo c = repo.findByTokenHash(hashToken(token)).orElseThrow(ConteudoService::linkInvalido);
        verificar(c);
        return new AvaliacaoResponse(c.getTitulo(), c.getDescricao(), c.getUrlVideo(), c.getPalavrasChave(), c.getTokenExpiraEm());
    }
    @Transactional
    public void confirmar(ConfirmacaoRequest r) {
        Conteudo c = repo.bloquearPorToken(hashToken(r.token())).orElseThrow(ConteudoService::linkInvalido);
        verificar(c);
        LinksAvaliacao.extrair(r.justificativa());
        c.setStatusAvaliacao(r.decisao() == Decisao.APROVAR ? StatusAvaliacao.APROVADO : StatusAvaliacao.RECUSADO);
        c.setAprovado(r.decisao() == Decisao.APROVAR);
        c.setAprovadoPor(c.isAprovado() ? "Professor convidado" : null);
        c.setJustificativa(r.justificativa().trim());
        c.setRespondidoEm(Instant.now());
        c.setTokenHash(null);
        eventos.publishEvent(new Resultado(c.getId()));
    }
}
