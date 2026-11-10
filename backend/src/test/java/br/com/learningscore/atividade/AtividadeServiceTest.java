package br.com.learningscore.atividade;

import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.dadosacademicos.DisciplinaRepository;
import br.com.learningscore.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {
    @Mock AtividadeRepository repo;
    @Mock DisciplinaRepository disciplinas;
    @Mock UsuarioRepository usuarios;
    @InjectMocks AtividadeService service;

    @Test
    void estudanteNaoAcessaAtividadeDeOutro() {
        // a consulta filtra por (id, usuarioId); para outro estudante nada é encontrado
        when(repo.findByIdAndUsuarioId(10L, 2L)).thenReturn(Optional.empty());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscar(2L, 10L));
    }

    @Test
    void prazoVencidoViraAtrasada() {
        assertEquals(StatusAtividade.ATRASADA,
                AtividadeService.normalizarStatus(StatusAtividade.PENDENTE, LocalDateTime.now().minusDays(1)));
    }

    @Test
    void concluidaNuncaViraAtrasada() {
        assertEquals(StatusAtividade.CONCLUIDA,
                AtividadeService.normalizarStatus(StatusAtividade.CONCLUIDA, LocalDateTime.now().minusDays(1)));
    }

    @Test
    void atrasadaComPrazoNovoVoltaParaPendente() {
        assertEquals(StatusAtividade.PENDENTE,
                AtividadeService.normalizarStatus(StatusAtividade.ATRASADA, LocalDateTime.now().plusDays(2)));
    }
}
