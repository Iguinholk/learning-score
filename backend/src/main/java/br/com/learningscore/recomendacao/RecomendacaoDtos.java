package br.com.learningscore.recomendacao;

import br.com.learningscore.conteudo.ConteudoDtos;
import java.math.BigDecimal;
import java.util.List;

public final class RecomendacaoDtos {
    private RecomendacaoDtos() {}

    public record Item(Long id, BigDecimal compatibilidade, ConteudoDtos.Response conteudo) {}

    /** completa = true quando há exatamente 3 recomendações; senão a mensagem explica o motivo. */
    public record PorDisciplina(Long disciplinaId, String disciplinaNome, String palavrasChave,
                                List<Item> recomendacoes, boolean completa, String mensagem) {}
}
