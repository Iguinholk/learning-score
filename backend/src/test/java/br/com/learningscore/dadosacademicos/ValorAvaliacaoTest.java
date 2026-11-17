package br.com.learningscore.dadosacademicos;

import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.notificacao.NotificacaoService;
import br.com.learningscore.usuario.UsuarioRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValorAvaliacaoTest {
    private AcademicoDtos.NotaRequest pedido(String nota, String total) {
        return new AcademicoDtos.NotaRequest("Prova", new BigDecimal(nota), new BigDecimal(total), LocalDate.now());
    }

    @Test void validaLimitesDoValorDaProva() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var v = factory.getValidator();
            assertTrue(v.validate(pedido("8", "10")).isEmpty());
            assertFalse(v.validate(pedido("0", "0")).isEmpty());
            assertFalse(v.validate(pedido("8", "11")).isEmpty());
            assertFalse(v.validate(pedido("0", "1.001")).isEmpty());
        }
    }

    @Test void salvaValorERejeitaNotaSuperiorSemAlterarRegistro() {
        NotaRepository repo = mock(NotaRepository.class);
        Nota n = new Nota();
        n.setDisciplina(new Disciplina());
        when(repo.findByIdAndDisciplinaUsuarioId(2L, 1L)).thenReturn(Optional.of(n));
        var service = new DisciplinaService(mock(DisciplinaRepository.class), repo,
                mock(UsuarioRepository.class), mock(NotificacaoService.class));
        var resposta = service.atualizarNota(1L, 2L, pedido("4", "5"));
        assertEquals(new BigDecimal("5"), resposta.valorAvaliacao());
        assertEquals(new BigDecimal("4"), resposta.valor());
        assertThrows(RegraNegocioException.class, () -> service.atualizarNota(1L, 2L, pedido("6", "5")));
        assertEquals(new BigDecimal("4"), n.getValor());
        service.atualizarNota(1L, 2L, pedido("5", "5"));
        assertEquals(new BigDecimal("5"), n.getValor());
    }
}
