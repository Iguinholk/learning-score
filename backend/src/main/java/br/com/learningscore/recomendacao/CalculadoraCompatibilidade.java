package br.com.learningscore.recomendacao;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Critério OBJETIVO e verificável de compatibilidade (sem IA):
 *
 *   compatibilidade(%) = (palavras-chave da DISCIPLINA presentes no CONTEÚDO) / (total de palavras-chave da disciplina) × 100
 *
 * As palavras são separadas por vírgula ou ponto e vírgula, comparadas sem acentos e sem diferenciar maiúsculas.
 * Exemplo: disciplina com 5 palavras e conteúdo que contém 4 delas → 80%.
 */
public final class CalculadoraCompatibilidade {
    public static final double MINIMO_PERCENTUAL = 80.0;

    private CalculadoraCompatibilidade() {}

    public static Set<String> palavras(String texto) {
        Set<String> r = new LinkedHashSet<>();
        if (texto == null) return r;
        Arrays.stream(texto.split("[,;]"))
                .map(CalculadoraCompatibilidade::normalizar)
                .filter(s -> !s.isBlank())
                .forEach(r::add);
        return r;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** @return percentual de 0 a 100 (duas casas); 0 se a disciplina não tem palavras-chave. */
    public static double calcular(String palavrasDisciplina, String palavrasConteudo) {
        Set<String> d = palavras(palavrasDisciplina);
        if (d.isEmpty()) return 0.0;
        Set<String> c = palavras(palavrasConteudo);
        long comuns = d.stream().filter(c::contains).count();
        return Math.round(comuns * 10000.0 / d.size()) / 100.0;
    }
}
