package br.com.learningscore.dadosacademicos;

import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.dadosacademicos.AcademicoDtos.*;
import br.com.learningscore.notificacao.NotificacaoService;
import br.com.learningscore.notificacao.TipoNotificacao;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** RF04: disciplinas, notas e frequência. Todas as buscas filtram pelo id do estudante logado. */
@Service
@RequiredArgsConstructor
public class DisciplinaService {
    private final DisciplinaRepository disciplinas;
    private final NotaRepository notas;
    private final UsuarioRepository usuarios;
    private final NotificacaoService notificacoes;

    /** Média simples das avaliações; null se ainda não há notas. */
    public static BigDecimal calcularMedia(List<Nota> lista) {
        if (lista.isEmpty()) return null;
        BigDecimal soma = BigDecimal.ZERO;
        for (Nota n : lista) {
            soma = soma.add(n.getValor());
        }
        return soma.divide(BigDecimal.valueOf(lista.size()), 2, RoundingMode.HALF_UP);
    }

    static DisciplinaResponse paraResposta(Disciplina d) {
        return new DisciplinaResponse(d.getId(), d.getNome(), d.getProfessor(), d.getPalavrasChave(),
                d.getFrequenciaPercentual(), calcularMedia(d.getNotas()),
                d.getNotas().stream().map(NotaResponse::de).toList());
    }

    @Transactional(readOnly = true)
    public List<DisciplinaResponse> listar(Long uid) {
        return disciplinas.findByUsuarioIdOrderByNomeAsc(uid).stream().map(DisciplinaService::paraResposta).toList();
    }

    @Transactional
    public DisciplinaResponse criar(Long uid, DisciplinaRequest r) {
        Disciplina d = new Disciplina();
        d.setUsuario(usuarios.getReferenceById(uid));
        aplicar(d, r);
        return paraResposta(disciplinas.save(d));
    }

    @Transactional
    public DisciplinaResponse atualizar(Long uid, Long id, DisciplinaRequest r) {
        Disciplina d = buscar(uid, id);
        aplicar(d, r);
        return paraResposta(d);
    }

    @Transactional
    public void excluir(Long uid, Long id) {
        disciplinas.delete(buscar(uid, id));
    }

    @Transactional
    public NotaResponse lancarNota(Long uid, Long disciplinaId, NotaRequest r) {
        Disciplina d = buscar(uid, disciplinaId);
        Nota n = new Nota();
        n.setDisciplina(d);
        aplicarNota(n, r);
        n = notas.save(n);
        notificacoes.criarSePermitido(uid, TipoNotificacao.ATUALIZACAO_ACADEMICA,
                "Nova nota em " + d.getNome(), "Foi lançada a nota \"" + n.getDescricao() + "\": " + n.getValor(),
                "nota:" + n.getId());
        return NotaResponse.de(n);
    }

    @Transactional
    public NotaResponse atualizarNota(Long uid, Long notaId, NotaRequest r) {
        Nota n = notas.findByIdAndDisciplinaUsuarioId(notaId, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota não encontrada"));
        aplicarNota(n, r);
        return NotaResponse.de(n);
    }

    @Transactional
    public void excluirNota(Long uid, Long notaId) {
        Nota n = notas.findByIdAndDisciplinaUsuarioId(notaId, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota não encontrada"));
        n.getDisciplina().getNotas().remove(n);
        notas.delete(n);
    }

    private Disciplina buscar(Long uid, Long id) {
        return disciplinas.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada"));
    }

    private void aplicar(Disciplina d, DisciplinaRequest r) {
        d.setNome(r.nome().trim());
        d.setProfessor(r.professor() == null || r.professor().isBlank() ? null : r.professor().trim());
        d.setPalavrasChave(r.palavrasChave() == null ? "" : r.palavrasChave().trim());
        d.setFrequenciaPercentual(r.frequenciaPercentual());
    }

    private void aplicarNota(Nota n, NotaRequest r) {
        if (r.valor().compareTo(r.valorAvaliacao()) > 0) {
            throw new RegraNegocioException("A nota obtida não pode ultrapassar o valor da prova.");
        }
        n.setValorAvaliacao(r.valorAvaliacao());
        n.setDescricao(r.descricao().trim());
        n.setValor(r.valor());
        n.setPeso(BigDecimal.ONE); // Coluna legada preservada por compatibilidade com o banco.
        n.setDataLancamento(r.dataLancamento());
    }
}
