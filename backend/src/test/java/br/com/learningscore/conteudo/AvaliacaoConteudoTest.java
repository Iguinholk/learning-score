package br.com.learningscore.conteudo;

import br.com.learningscore.common.*;
import br.com.learningscore.conteudo.ConteudoDtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AvaliacaoConteudoTest {
    private final ConteudoRepository repo = mock(ConteudoRepository.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ConteudoService service = new ConteudoService(repo, eventos);
    private final String token = "a".repeat(43);
    private Conteudo pendente() {
        Conteudo c = new Conteudo(); c.setId(8L); c.setTitulo("Vídeo"); c.setEmailProfessor("prof@example.com");
        c.setTokenHash(ConteudoService.hashToken(token)); c.setTokenExpiraEm(Instant.now().plusSeconds(600));
        return c;
    }
    @Test void consultarNaoDecideNemConsomeToken() {
        Conteudo c = pendente(); when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(c));
        assertEquals("Vídeo", service.consultar(token).titulo());
        assertFalse(c.isAprovado()); assertEquals(StatusAvaliacao.PENDENTE, c.getStatusAvaliacao());
        assertNotNull(c.getTokenHash()); verifyNoInteractions(eventos);
    }
    @Test void confirmaAprovacaoEConsomeToken() {
        Conteudo c = pendente(); when(repo.bloquearPorToken(anyString())).thenReturn(Optional.of(c));
        service.confirmar(new ConfirmacaoRequest(token, Decisao.APROVAR, "Bom vídeo. Veja https://exemplo.com/a"));
        assertTrue(c.isAprovado()); assertEquals(StatusAvaliacao.APROVADO, c.getStatusAvaliacao());
        assertNull(c.getTokenHash()); assertNotNull(c.getRespondidoEm());
        verify(eventos).publishEvent(new ConteudoService.Resultado(8L));
        assertThrows(RegraNegocioException.class, () -> service.confirmar(new ConfirmacaoRequest(token, Decisao.RECUSAR, "Outro")));
    }
    @Test void recusaNaoEntraNoCatalogo() {
        Conteudo c = pendente(); when(repo.bloquearPorToken(anyString())).thenReturn(Optional.of(c));
        service.confirmar(new ConfirmacaoRequest(token, Decisao.RECUSAR, "Prefiro https://exemplo.com/outro"));
        assertFalse(c.isAprovado()); assertEquals(StatusAvaliacao.RECUSADO, c.getStatusAvaliacao());
    }
    @Test void tokenVencidoNaoPermiteConsultarNemConfirmar() {
        Conteudo c = pendente(); c.setTokenExpiraEm(Instant.now().minusSeconds(1));
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(c));
        when(repo.bloquearPorToken(anyString())).thenReturn(Optional.of(c));
        assertThrows(RegraNegocioException.class, () -> service.consultar(token));
        assertThrows(RegraNegocioException.class, () -> service.confirmar(new ConfirmacaoRequest(token, Decisao.APROVAR, "Bom")));
        assertFalse(c.isAprovado()); verifyNoInteractions(eventos);
    }
    @Test void outroEstudanteNaoPodeReenviar() {
        when(repo.bloquearMinhaSugestao(8L, 99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.reenviar(99L, 8L));
        verifyNoInteractions(eventos);
    }
    @Test void minhasSugestoesSempreFiltramPeloUsuario() {
        when(repo.findBySugeridoPorOrderByCriadoEmDesc(99L)).thenReturn(List.of());
        assertTrue(service.minhas(99L).isEmpty());
        verify(repo).findBySugeridoPorOrderByCriadoEmDesc(99L);
    }
    @Test void sugestaoGeraHashSemGuardarTokenAberto() {
        when(repo.save(any())).thenAnswer(i -> { Conteudo c = i.getArgument(0); c.setId(8L); return c; });
        var r = service.sugerir(4L, new Request("Vídeo", "Descrição", "https://youtu.be/abcdefghijk", "java", "prof@example.com"));
        assertEquals(StatusAvaliacao.PENDENTE, r.status());
        var captor = org.mockito.ArgumentCaptor.forClass(ConteudoService.Convite.class);
        verify(eventos).publishEvent(captor.capture());
        String segredo = captor.getValue().token(); assertEquals(43, segredo.length());
        var entidade = org.mockito.ArgumentCaptor.forClass(Conteudo.class); verify(repo).save(entidade.capture());
        assertEquals(ConteudoService.hashToken(segredo), entidade.getValue().getTokenHash());
        assertNotEquals(segredo, entidade.getValue().getTokenHash());
    }
    @Test void linksAceitamHttpERejeitamEsquemasPerigosos() {
        assertEquals(List.of("https://exemplo.com/a", "http://exemplo.com/b"), LinksAvaliacao.extrair("Veja https://exemplo.com/a e http://exemplo.com/b."));
        assertThrows(RegraNegocioException.class, () -> LinksAvaliacao.extrair("javascript:alert(1)"));
        assertThrows(RegraNegocioException.class, () -> LinksAvaliacao.extrair("https://user:senha@exemplo.com"));
        assertThrows(RegraNegocioException.class, () -> LinksAvaliacao.extrair("https://"));
    }
}
