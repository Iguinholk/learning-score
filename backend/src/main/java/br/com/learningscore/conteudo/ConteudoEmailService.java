package br.com.learningscore.conteudo;

import br.com.learningscore.privacidade.EmailService;
import br.com.learningscore.usuario.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;
import org.springframework.web.util.HtmlUtils;
import java.net.URI;
import java.time.Instant;

/** O envio ocorre depois do commit: falha no SMTP nunca desfaz uma decisão já salva. */
@Service
public class ConteudoEmailService {
    private final ConteudoRepository repo;
    private final UsuarioRepository usuarios;
    private final EmailService email;
    private final String baseUrl;
    public ConteudoEmailService(ConteudoRepository repo, UsuarioRepository usuarios, EmailService email,
            @Value("${learningscore.public-base-url:http://localhost:8080}") String baseUrl) {
        this.repo = repo; this.usuarios = usuarios; this.email = email;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }
    @PostConstruct
    void validarEndereco() {
        URI u = URI.create(baseUrl);
        boolean local = "localhost".equals(u.getHost()) || "127.0.0.1".equals(u.getHost()) || "[::1]".equals(u.getHost());
        if (u.getHost() == null || u.getUserInfo() != null || u.getRawQuery() != null || u.getRawFragment() != null
                || !("https".equals(u.getScheme()) || (local && "http".equals(u.getScheme()))))
            throw new IllegalStateException("PUBLIC_BASE_URL deve usar HTTPS (HTTP apenas em localhost).");
    }
    private static String h(String s) { return HtmlUtils.htmlEscape(s == null ? "" : s); }
    private static String botao(String link, String texto) {
        return "<a style='display:inline-block;padding:12px 20px;margin:8px;background:#153c38;color:white;border-radius:8px' href='"
                + h(link) + "'>" + h(texto) + "</a>";
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void convite(ConteudoService.Convite evento) {
        Conteudo c = repo.bloquearPorId(evento.id()).orElse(null);
        if (c == null || c.getStatusAvaliacao() != StatusAvaliacao.PENDENTE
                || !ConteudoService.hashToken(evento.token()).equals(c.getTokenHash())
                || !c.getTokenExpiraEm().isAfter(Instant.now())) return;
        String link = baseUrl + "/avaliar-conteudo.html#token=" + evento.token();
        String aprovar = link + "&decisao=APROVAR", recusar = link + "&decisao=RECUSAR";
        String detalhes = "Título: " + c.getTitulo() + "\nDescrição: " + (c.getDescricao() == null ? "" : c.getDescricao())
                + "\nVídeo: " + (c.getUrlVideo() == null ? "Não informado" : c.getUrlVideo())
                + "\nPalavras-chave: " + c.getPalavrasChave();
        String texto = "Um estudante pediu sua avaliação no Learning Score. Não é necessário criar conta.\n\n" + detalhes
                + "\n\nAprovar: " + aprovar + "\nRecusar: " + recusar + "\nVálido até " + c.getTokenExpiraEm()
                + ". O clique não registra a decisão; confirme na página. Não encaminhe este convite.";
        String corpo = "<h1>Avaliar conteúdo</h1><p>Um estudante pediu sua avaliação. Você não precisa de conta.</p><pre style='white-space:pre-wrap'>"
                + h(detalhes) + "</pre>" + botao(aprovar, "Aprovar") + botao(recusar, "Recusar")
                + "<p>O clique abre uma página de confirmação. Válido até " + h(c.getTokenExpiraEm().toString())
                + ". Não encaminhe este convite.</p>";
        c.setEmailConviteEnviado(email.enviarHtml(c.getEmailProfessor(), "Learning Score — avaliar conteúdo", texto, corpo));
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resultado(ConteudoService.Resultado evento) { enviarResultado(evento.id()); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviarResultado(Long id) {
        Conteudo c = repo.bloquearPorId(id).orElse(null);
        if (c == null || c.getRespondidoEm() == null || c.isEmailResultadoEnviado() || c.getSugeridoPor() == null) return;
        var aluno = usuarios.findById(c.getSugeridoPor()).orElse(null);
        if (aluno == null) return;
        String decisao = c.isAprovado() ? "Aprovado" : "Recusado";
        String texto = "Sua sugestão: " + c.getTitulo() + "\nDecisão: " + decisao + "\nData: " + c.getRespondidoEm()
                + "\nJustificativa e links sugeridos:\n" + c.getJustificativa() + "\nConsulte: " + baseUrl + "/app.html#/conteudos";
        StringBuilder links = new StringBuilder();
        for (String url : LinksAvaliacao.extrair(c.getJustificativa()))
            links.append("<li><a href='").append(h(url)).append("' rel='noopener noreferrer'>").append(h(url)).append("</a></li>");
        String corpo = "<h1>" + decisao + "</h1><p>" + h(c.getTitulo()) + "</p><p>Respondido em " + h(c.getRespondidoEm().toString())
                + "</p><h2>Justificativa e links sugeridos</h2><p style='white-space:pre-wrap'>" + h(c.getJustificativa())
                + "</p><ul>" + links + "</ul>" + botao(baseUrl + "/app.html#/conteudos", "Ver no Learning Score");
        c.setEmailResultadoEnviado(email.enviarHtml(aluno.getEmail(), "Learning Score — resultado da sua sugestão", texto, corpo));
    }
}
