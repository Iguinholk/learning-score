package br.com.learningscore.recomendacao;

import br.com.learningscore.conteudo.Conteudo;
import br.com.learningscore.conteudo.ConteudoRepository;
import br.com.learningscore.conteudo.ConteudoDtos;
import br.com.learningscore.dadosacademicos.Disciplina;
import br.com.learningscore.dadosacademicos.DisciplinaRepository;
import br.com.learningscore.recomendacao.RecomendacaoDtos.*;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * RF09: até 3 recomendações por disciplina, usando apenas conteúdos APROVADOS por professores
 * e compatibilidade ≥ 80% (ver {@link CalculadoraCompatibilidade}). Nenhuma IA é utilizada.
 */
@Service
@RequiredArgsConstructor
public class RecomendacaoService {
    public static final int QUANTIDADE = 3;

    private final RecomendacaoRepository repo;
    private final DisciplinaRepository disciplinas;
    private final ConteudoRepository conteudos;
    private final UsuarioRepository usuarios;

    @Transactional
    public List<PorDisciplina> listar(Long uid) {
        List<Conteudo> aprovados = conteudos.findByAprovadoTrueOrderByTituloAsc();
        List<PorDisciplina> resultado = new ArrayList<>();
        for (Disciplina d : disciplinas.findByUsuarioIdOrderByNomeAsc(uid)) {
            repo.apagarDaDisciplina(d.getId());
            List<Item> itens = new ArrayList<>();
            if (!CalculadoraCompatibilidade.palavras(d.getPalavrasChave()).isEmpty()) {
                List<Map.Entry<Conteudo, Double>> candidatos = new ArrayList<>();
                for (Conteudo c : aprovados) {
                    double comp = CalculadoraCompatibilidade.calcular(d.getPalavrasChave(), c.getPalavrasChave());
                    if (comp >= CalculadoraCompatibilidade.MINIMO_PERCENTUAL) candidatos.add(Map.entry(c, comp));
                }
                candidatos.sort(Map.Entry.<Conteudo, Double>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().getTitulo()));
                for (Map.Entry<Conteudo, Double> e : candidatos.stream().limit(QUANTIDADE).toList()) {
                    Recomendacao r = new Recomendacao();
                    r.setUsuario(usuarios.getReferenceById(uid));
                    r.setDisciplina(d);
                    r.setConteudo(e.getKey());
                    r.setCompatibilidade(BigDecimal.valueOf(e.getValue()));
                    r = repo.save(r);
                    itens.add(new Item(r.getId(), r.getCompatibilidade(), paraResposta(e.getKey())));
                }
            }
            boolean completa = itens.size() == QUANTIDADE;
            String msg = completa ? null
                    : CalculadoraCompatibilidade.palavras(d.getPalavrasChave()).isEmpty()
                        ? "Cadastre palavras-chave nesta disciplina para receber recomendações."
                        : "Há apenas " + itens.size() + " conteúdo(s) aprovado(s) com compatibilidade de pelo menos 80%.";
            resultado.add(new PorDisciplina(d.getId(), d.getNome(), d.getPalavrasChave(), itens, completa, msg));
        }
        return resultado;
    }

    private ConteudoDtos.Response paraResposta(Conteudo c) {
        return new ConteudoDtos.Response(c.getId(), c.getTitulo(), c.getDescricao(), c.getUrlEmbed(),
                c.getPalavrasChave(), c.isAprovado(), c.getAprovadoPor());
    }
}
