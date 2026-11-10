package br.com.learningscore.dadosacademicos;

import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.dadosacademicos.AcademicoDtos.*;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceiroService {
    private final RegistroFinanceiroRepository repo;
    private final UsuarioRepository usuarios;

    static String situacao(RegistroFinanceiro r) {
        if (r.getStatus() == StatusFinanceiro.PAGO) return "PAGO";
        return r.getVencimento().isBefore(LocalDate.now()) ? "ATRASADO" : "PENDENTE";
    }

    static FinanceiroResponse paraResposta(RegistroFinanceiro r) {
        return new FinanceiroResponse(r.getId(), r.getDescricao(), r.getCompetencia(), r.getValor(),
                r.getVencimento(), situacao(r), r.getDataPagamento());
    }

    @Transactional(readOnly = true)
    public FinanceiroListagem listar(Long uid) {
        List<RegistroFinanceiro> lista = repo.findByUsuarioIdOrderByVencimentoDesc(uid);
        BigDecimal pago = BigDecimal.ZERO, pendente = BigDecimal.ZERO, atrasado = BigDecimal.ZERO;
        for (RegistroFinanceiro r : lista) {
            switch (situacao(r)) {
                case "PAGO" -> pago = pago.add(r.getValor());
                case "ATRASADO" -> atrasado = atrasado.add(r.getValor());
                default -> pendente = pendente.add(r.getValor());
            }
        }
        return new FinanceiroListagem(lista.stream().map(FinanceiroService::paraResposta).toList(),
                new ResumoFinanceiro(pago, pendente, atrasado));
    }

    @Transactional
    public FinanceiroResponse criar(Long uid, FinanceiroRequest req) {
        RegistroFinanceiro r = new RegistroFinanceiro();
        r.setUsuario(usuarios.getReferenceById(uid));
        aplicar(r, req);
        return paraResposta(repo.save(r));
    }

    @Transactional
    public FinanceiroResponse atualizar(Long uid, Long id, FinanceiroRequest req) {
        RegistroFinanceiro r = buscar(uid, id);
        aplicar(r, req);
        return paraResposta(r);
    }

    @Transactional
    public void excluir(Long uid, Long id) {
        repo.delete(buscar(uid, id));
    }

    private RegistroFinanceiro buscar(Long uid, Long id) {
        return repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Registro financeiro não encontrado"));
    }

    private void aplicar(RegistroFinanceiro r, FinanceiroRequest q) {
        if (q.status() == StatusFinanceiro.PAGO && q.dataPagamento() == null) {
            throw new RegraNegocioException("Informe a data de pagamento para registros pagos.");
        }
        r.setDescricao(q.descricao().trim());
        r.setCompetencia(q.competencia());
        r.setValor(q.valor());
        r.setVencimento(q.vencimento());
        r.setStatus(q.status());
        r.setDataPagamento(q.status() == StatusFinanceiro.PAGO ? q.dataPagamento() : null);
    }
}
