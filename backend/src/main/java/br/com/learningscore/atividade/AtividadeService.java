package br.com.learningscore.atividade;

import br.com.learningscore.atividade.AtividadeDtos.*;
import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.dadosacademicos.Disciplina;
import br.com.learningscore.dadosacademicos.DisciplinaRepository;
import br.com.learningscore.usuario.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** RF02/RF03: CRUD de atividades com isolamento por estudante e atualização automática do status ATRASADA. */
@Service
@RequiredArgsConstructor
public class AtividadeService {
    public static final List<StatusAtividade> ABERTAS = List.of(StatusAtividade.PENDENTE, StatusAtividade.EM_ANDAMENTO);
    public static final int TAMANHO_MAXIMO_PAGINA = 50;

    private final AtividadeRepository repo;
    private final DisciplinaRepository disciplinas;
    private final UsuarioRepository usuarios;

    @Transactional
    public int marcarAtrasadas(Long uid) {
        return repo.marcarAtrasadas(uid, LocalDateTime.now(), ABERTAS, StatusAtividade.ATRASADA);
    }

    @Transactional
    public Pagina listar(Long uid, StatusAtividade status, Long disciplinaId, int pagina, int tamanho) {
        marcarAtrasadas(uid);
        int tam = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA);   // até 50 por listagem
        Pageable p = PageRequest.of(Math.max(pagina, 0), tam, Sort.by("prazo").ascending());
        Specification<Atividade> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("usuario").get("id"), uid));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (disciplinaId != null) ps.add(cb.equal(root.get("disciplina").get("id"), disciplinaId));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Atividade> r = repo.findAll(spec, p);
        return new Pagina(r.getContent().stream().map(Response::de).toList(),
                r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional
    public Response buscar(Long uid, Long id) {
        marcarAtrasadas(uid);
        return Response.de(obter(uid, id));
    }

    @Transactional
    public Response criar(Long uid, Request r) {
        Atividade a = new Atividade();
        a.setUsuario(usuarios.getReferenceById(uid));
        aplicar(uid, a, r);
        return Response.de(repo.save(a));
    }

    @Transactional
    public Response atualizar(Long uid, Long id, Request r) {
        Atividade a = obter(uid, id);
        aplicar(uid, a, r);
        return Response.de(a);
    }

    @Transactional
    public void excluir(Long uid, Long id) {
        repo.delete(obter(uid, id));
    }

    /** Busca SEMPRE filtrando pelo dono: outro estudante recebe 404. */
    Atividade obter(Long uid, Long id) {
        return repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada"));
    }

    private void aplicar(Long uid, Atividade a, Request r) {
        a.setTitulo(r.titulo().trim());
        a.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        a.setPrazo(r.prazo());
        a.setPrioridade(r.prioridade() == null ? Prioridade.MEDIA : r.prioridade());
        a.setLinkEntrega(r.linkEntrega() == null || r.linkEntrega().isBlank() ? null : r.linkEntrega().trim());
        if (r.disciplinaId() == null) {
            a.setDisciplina(null);
        } else {
            Disciplina d = disciplinas.findByIdAndUsuarioId(r.disciplinaId(), uid)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina não encontrada"));
            a.setDisciplina(d);
        }
        a.setStatus(normalizarStatus(r.status(), r.prazo()));
    }

    /** Concluída permanece; as demais viram ATRASADA se o prazo passou, ou voltam a PENDENTE se o prazo foi ajustado. */
    public static StatusAtividade normalizarStatus(StatusAtividade pedido, LocalDateTime prazo) {
        StatusAtividade s = pedido == null ? StatusAtividade.PENDENTE : pedido;
        if (s == StatusAtividade.CONCLUIDA) return s;
        if (prazo.isBefore(LocalDateTime.now())) return StatusAtividade.ATRASADA;
        return s == StatusAtividade.ATRASADA ? StatusAtividade.PENDENTE : s;
    }
}
