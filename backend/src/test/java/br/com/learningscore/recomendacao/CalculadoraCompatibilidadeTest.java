package br.com.learningscore.recomendacao;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraCompatibilidadeTest {
    private static final String DISCIPLINA = "java, classes, herança, polimorfismo, encapsulamento";

    @Test
    void todasAsPalavrasDa100Porcento() {
        assertEquals(100.0, CalculadoraCompatibilidade.calcular(DISCIPLINA, "Java, Classes, Heranca, Polimorfismo, Encapsulamento"));
    }

    @Test
    void quatroDeCincoDa80Porcento() {
        assertEquals(80.0, CalculadoraCompatibilidade.calcular(DISCIPLINA, "java, classes, polimorfismo, encapsulamento, extra"));
    }

    @Test
    void trezDeCincoFicaAbaixoDoMinimo() {
        double c = CalculadoraCompatibilidade.calcular(DISCIPLINA, "java, classes, polimorfismo");
        assertEquals(60.0, c);
        assertTrue(c < CalculadoraCompatibilidade.MINIMO_PERCENTUAL);
    }

    @Test
    void ignoraAcentosMaiusculasEEspacos() {
        assertEquals(100.0, CalculadoraCompatibilidade.calcular("  HERANÇA ;  Java ", "heranca,java"));
    }

    @Test
    void disciplinaSemPalavrasRetornaZero() {
        assertEquals(0.0, CalculadoraCompatibilidade.calcular("", "java"));
        assertEquals(0.0, CalculadoraCompatibilidade.calcular(null, "java"));
    }
}
