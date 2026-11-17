package br.com.learningscore.dadosacademicos;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DisciplinaServiceTest {
    private Nota nota(String valor, String pesoLegado) {
        Nota n = new Nota();
        n.setValor(new BigDecimal(valor));
        n.setPeso(new BigDecimal(pesoLegado));
        return n;
    }

    @Test void semNotasNaoPossuiMedia() {
        assertNull(DisciplinaService.calcularMedia(List.of()));
    }

    @Test void mediaSimplesIgnoraPesosAntigos() {
        assertEquals(new BigDecimal("7.00"), DisciplinaService.calcularMedia(
                List.of(nota("4", "1"), nota("10", "9"))));
    }

    @Test void incluiZeroEArredondaParaDuasCasas() {
        assertEquals(new BigDecimal("3.33"), DisciplinaService.calcularMedia(
                List.of(nota("0", "1"), nota("0", "1"), nota("10", "1"))));
    }
    @Test void converteAvaliacoesComValoresDiferentesParaDez() {
        Nota pequena = nota("4", "1"); pequena.setValorAvaliacao(new BigDecimal("5"));
        assertEquals(new BigDecimal("8.00"), DisciplinaService.calcularMedia(List.of(pequena)));
        assertEquals(new BigDecimal("9.00"), DisciplinaService.calcularMedia(List.of(pequena, nota("10", "1"))));
    }

    @Test void arredondaSomenteDepoisDaMedia() {
        Nota a = nota("1", "1"); a.setValorAvaliacao(new BigDecimal("3"));
        Nota b = nota("1", "1"); b.setValorAvaliacao(new BigDecimal("6"));
        assertEquals(new BigDecimal("2.50"), DisciplinaService.calcularMedia(List.of(a, b)));
    }
}
