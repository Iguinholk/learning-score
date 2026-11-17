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
}
