package br.com.learningscore.dashboard;

import br.com.learningscore.atividade.AtividadeDtos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record Indicadores(long totalAtividades, long concluidas, long atrasadas, long entregasEm7Dias,
                              int taxaConclusaoPercentual, BigDecimal mediaGeral, BigDecimal frequenciaMedia,
                              BigDecimal mensalidadesEmAberto, long notificacoesNaoLidas) {}

    public record MediaDisciplina(String disciplina, BigDecimal media) {}

    /** Gráfico 1 = atividadesPorStatus; gráfico 2 = mediasPorDisciplina. */
    public record Resposta(String saudacao, String nome,
                           List<AtividadeDtos.Response> prioritarias,      // até 5
                           List<AtividadeDtos.Response> proximasEntregas,  // até 5
                           Map<String, Long> atividadesPorStatus,
                           List<MediaDisciplina> mediasPorDisciplina,
                           Indicadores indicadores) {}
}
