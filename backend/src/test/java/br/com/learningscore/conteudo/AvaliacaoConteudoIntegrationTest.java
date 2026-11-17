package br.com.learningscore.conteudo;

import br.com.learningscore.privacidade.EmailService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Executar apenas com banco PostgreSQL de testes vazio/isolado, nunca com o banco de uso. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "learningscore.demo.enabled=false", "learningscore.public-base-url=http://localhost:8080"})
@EnabledIfEnvironmentVariable(named = "LS_TEST_DB_URL", matches = ".+")
class AvaliacaoConteudoIntegrationTest {
    @DynamicPropertySource static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("LS_TEST_DB_URL"));
        r.add("spring.datasource.username", () -> System.getenv().getOrDefault("LS_TEST_DB_USER", "learning_score_test"));
        r.add("spring.datasource.password", () -> System.getenv().getOrDefault("LS_TEST_DB_PASSWORD", ""));
    }
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired ConteudoRepository repo;
    @Autowired ConteudoService service;
    @MockBean EmailService email;
    final List<String> mensagens = new CopyOnWriteArrayList<>();
    @BeforeEach void smtp() {
        when(email.enviarHtml(anyString(), anyString(), anyString(), anyString())).thenAnswer(i -> {
            mensagens.add(i.getArgument(0) + "\n" + i.getArgument(3)); return true;
        });
    }
    private HttpClient cliente() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build(); }
    private HttpResponse<String> req(HttpClient c, String metodo, String rota, Object body, boolean csrf, String token) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rota));
        if (token != null) b.header("X-Avaliacao-Token", token);
        if (csrf) {
            CookieManager cm = (CookieManager)c.cookieHandler().orElseThrow();
            String valor = cm.getCookieStore().getCookies().stream().filter(x -> x.getName().equals("XSRF-TOKEN"))
                    .map(HttpCookie::getValue).findFirst().orElse("");
            b.header("X-XSRF-TOKEN", valor);
        }
        if (body != null) b.header("Content-Type", "application/json");
        return c.send(b.method(metodo, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpClient aluno() throws Exception {
        var c = cliente(); String mail = "aluno" + UUID.randomUUID() + "@example.test";
        assertEquals(201, req(c, "POST", "/api/auth/register", Map.of("nome", "Aluno", "email", mail, "senha", "Senha12345", "aceitouTermos", true), false, null).statusCode());
        assertEquals(200, req(c, "POST", "/api/auth/login", Map.of("email", mail, "senha", "Senha12345"), false, null).statusCode());
        req(c, "GET", "/api/auth/me", null, false, null); return c;
    }
    private record Sugestao(long id, String token, String professor) {}
    private Sugestao sugerir(HttpClient c) throws Exception {
        String prof = "prof" + UUID.randomUUID() + "@example.test";
        var resposta = req(c, "POST", "/api/conteudos", Map.of("titulo", "Vídeo <script>alert(1)</script>", "descricao", "Descrição", "urlVideo", "https://youtu.be/abcdefghijk", "palavrasChave", "java", "emailProfessor", prof), true, null);
        assertEquals(201, resposta.statusCode(), resposta.body());
        var node = json.readTree(resposta.body()); long id = node.get("id").asLong();
        assertEquals("PENDENTE", node.get("status").asText()); assertFalse(node.has("tokenHash"));
        String convite = mensagens.stream().filter(x -> x.startsWith(prof + "\n")).findFirst().orElseThrow();
        assertTrue(convite.contains("Aprovar")); assertTrue(convite.contains("Recusar")); assertFalse(convite.contains("<script>"));
        var m = Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(convite); assertTrue(m.find());
        String token = m.group(1); assertNotEquals(token, repo.findById(id).orElseThrow().getTokenHash());
        return new Sugestao(id, token, prof);
    }
    @Test void fluxoCompletoSemContaDoProfessorEComIsolamento() throws Exception {
        var dono = aluno(); var outro = aluno(); var s = sugerir(dono); var professor = cliente();
        assertEquals(401, req(cliente(), "GET", "/api/conteudos/minhas-sugestoes", null, false, null).statusCode());
        assertEquals("[]", req(outro, "GET", "/api/conteudos/minhas-sugestoes", null, false, null).body());
        assertEquals(404, req(outro, "POST", "/api/conteudos/" + s.id + "/reenviar-convite", Map.of(), true, null).statusCode());
        var consulta = req(professor, "GET", "/api/avaliacoes-conteudo", null, false, s.token);
        assertEquals(200, consulta.statusCode(), consulta.body());
        assertEquals("no-store", consulta.headers().firstValue("Cache-Control").orElse(""));
        assertFalse(repo.findById(s.id).orElseThrow().isAprovado());
        Map<String, String> body = Map.of("token", s.token, "decisao", "APROVAR", "justificativa", "Bom! Veja https://exemplo.com/material");
        assertEquals(403, req(professor, "POST", "/api/avaliacoes-conteudo/confirmar", body, false, null).statusCode());
        assertEquals(204, req(professor, "POST", "/api/avaliacoes-conteudo/confirmar", body, true, null).statusCode());
        assertEquals(422, req(professor, "POST", "/api/avaliacoes-conteudo/confirmar", body, true, null).statusCode());
        var c = repo.findById(s.id).orElseThrow(); assertTrue(c.isAprovado()); assertNull(c.getTokenHash()); assertNotNull(c.getRespondidoEm());
        assertTrue(c.isEmailResultadoEnviado());
        String minhas = req(dono, "GET", "/api/conteudos/minhas-sugestoes", null, false, null).body();
        assertTrue(minhas.contains("https://exemplo.com/material")); assertTrue(minhas.contains("APROVADO"));
        String catalogo = req(outro, "GET", "/api/conteudos", null, false, null).body();
        assertFalse(catalogo.contains(s.professor)); assertFalse(catalogo.contains("justificativa"));
        assertTrue(mensagens.stream().anyMatch(x -> x.startsWith("aluno") && x.contains("href='https://exemplo.com/material'")));
        assertEquals(404, req(dono, "PATCH", "/api/conteudos/" + s.id + "/aprovar", Map.of(), true, null).statusCode());
    }
    @Test void recusaFalhaDeEmailEReenvioDoResultado() throws Exception {
        var dono = aluno(); var s = sugerir(dono); var professor = cliente();
        req(professor, "GET", "/api/avaliacoes-conteudo", null, false, s.token);
        when(email.enviarHtml(anyString(), anyString(), anyString(), anyString())).thenReturn(false);
        assertEquals(204, req(professor, "POST", "/api/avaliacoes-conteudo/confirmar", Map.of("token", s.token, "decisao", "RECUSAR", "justificativa", "Use https://exemplo.com/outro"), true, null).statusCode());
        var c = repo.findById(s.id).orElseThrow(); assertFalse(c.isAprovado()); assertEquals(StatusAvaliacao.RECUSADO, c.getStatusAvaliacao()); assertFalse(c.isEmailResultadoEnviado());
        when(email.enviarHtml(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        emailService().enviarResultado(s.id); assertTrue(repo.findById(s.id).orElseThrow().isEmailResultadoEnviado());
    }
    @Autowired ConteudoEmailService emails;
    private ConteudoEmailService emailService() { return emails; }
    @Test void validaEmailELinksSemAlterarDecisao() throws Exception {
        var dono = aluno();
        assertEquals(400, req(dono, "POST", "/api/conteudos", Map.of("titulo", "Vídeo", "emailProfessor", "email-invalido"), true, null).statusCode());
        assertEquals(400, req(dono, "POST", "/api/conteudos", Map.of("titulo", "Vídeo"), true, null).statusCode());
        var s = sugerir(dono); var prof = cliente();
        req(prof, "GET", "/api/avaliacoes-conteudo", null, false, s.token);
        assertEquals(422, req(prof, "POST", "/api/avaliacoes-conteudo/confirmar", Map.of("token", s.token, "decisao", "APROVAR", "justificativa", "javascript:alert(1)"), true, null).statusCode());
        assertEquals(400, req(prof, "POST", "/api/avaliacoes-conteudo/confirmar", Map.of("token", s.token, "decisao", "APROVAR", "justificativa", " "), true, null).statusCode());
        assertEquals(StatusAvaliacao.PENDENTE, repo.findById(s.id).orElseThrow().getStatusAvaliacao());
        assertEquals(422, req(prof, "GET", "/api/avaliacoes-conteudo", null, false, "b".repeat(43)).statusCode());
        assertEquals(400, req(prof, "GET", "/api/avaliacoes-conteudo", null, false, null).statusCode());
    }
    @Test void semSmtpSugestaoFicaSalvaEConvitePodeSerReenviado() throws Exception {
        var dono = aluno(); when(email.enviarHtml(anyString(), anyString(), anyString(), anyString())).thenReturn(false);
        var resposta = req(dono, "POST", "/api/conteudos", Map.of("titulo", "Sem SMTP", "emailProfessor", "prof@example.test"), true, null);
        assertEquals(201, resposta.statusCode()); long id = json.readTree(resposta.body()).get("id").asLong();
        var c = repo.findById(id).orElseThrow(); assertEquals(StatusAvaliacao.PENDENTE, c.getStatusAvaliacao()); assertFalse(c.isEmailConviteEnviado());
        assertEquals(422, req(dono, "POST", "/api/conteudos/" + id + "/reenviar-convite", Map.of(), true, null).statusCode());
        c.setConviteEm(Instant.now().minusSeconds(70)); repo.save(c);
        when(email.enviarHtml(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        assertEquals(204, req(dono, "POST", "/api/conteudos/" + id + "/reenviar-convite", Map.of(), true, null).statusCode());
        assertTrue(repo.findById(id).orElseThrow().isEmailConviteEnviado());
    }
    @Test void tokenExpiradoReenvioEConcorrencia() throws Exception {
        var dono = aluno(); var s = sugerir(dono);
        var c = repo.findById(s.id).orElseThrow(); c.setTokenExpiraEm(Instant.now().minusSeconds(1)); c.setConviteEm(Instant.now().minusSeconds(70)); repo.save(c);
        assertEquals(422, req(cliente(), "GET", "/api/avaliacoes-conteudo", null, false, s.token).statusCode());
        assertEquals(204, req(dono, "POST", "/api/conteudos/" + s.id + "/reenviar-convite", Map.of(), true, null).statusCode());
        assertEquals(422, req(cliente(), "GET", "/api/avaliacoes-conteudo", null, false, s.token).statusCode());
        String novoConvite = mensagens.get(mensagens.size() - 1); var m = Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(novoConvite); assertTrue(m.find()); String novo = m.group(1);
        var inicio = new CountDownLatch(1);
        java.util.function.Supplier<Boolean> confirmar = () -> { try { inicio.await(); service.confirmar(new ConteudoDtos.ConfirmacaoRequest(novo, ConteudoDtos.Decisao.APROVAR, "Confirmado")); return true; } catch (br.com.learningscore.common.RegraNegocioException e) { return false; } catch (InterruptedException e) { throw new RuntimeException(e); } };
        var a = CompletableFuture.supplyAsync(confirmar); var b = CompletableFuture.supplyAsync(confirmar); inicio.countDown();
        assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        assertTrue(repo.findById(s.id).orElseThrow().isAprovado());
    }
}
