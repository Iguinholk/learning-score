package br.com.learningscore.dadosacademicos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AcademicoDtos {
    private AcademicoDtos() {}

    public record DisciplinaRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 120) String professor,
            @Size(max = 500) String palavrasChave,
            @DecimalMin("0") @DecimalMax("100") BigDecimal frequenciaPercentual) {}

    public record NotaRequest(
            @NotBlank @Size(max = 120) String descricao,
            @NotNull @DecimalMin("0") @DecimalMax("10") BigDecimal valor,
            @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero") BigDecimal peso,
            @NotNull LocalDate dataLancamento) {}

    public record NotaResponse(Long id, Long disciplinaId, String descricao, BigDecimal valor, BigDecimal peso, LocalDate dataLancamento) {
        static NotaResponse de(Nota n) {
            return new NotaResponse(n.getId(), n.getDisciplina().getId(), n.getDescricao(), n.getValor(), n.getPeso(), n.getDataLancamento());
        }
    }

    public record DisciplinaResponse(Long id, String nome, String professor, String palavrasChave,
                                     BigDecimal frequenciaPercentual, BigDecimal media, List<NotaResponse> notas) {}

    public record FinanceiroRequest(
            @NotBlank @Size(max = 160) String descricao,
            @NotNull LocalDate competencia,
            @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal valor,
            @NotNull LocalDate vencimento,
            @NotNull StatusFinanceiro status,
            LocalDate dataPagamento) {}

    /** situacao: PAGO, PENDENTE ou ATRASADO (calculada pela data de vencimento). */
    public record FinanceiroResponse(Long id, String descricao, LocalDate competencia, BigDecimal valor,
                                     LocalDate vencimento, String situacao, LocalDate dataPagamento) {}

    public record ResumoFinanceiro(BigDecimal totalPago, BigDecimal totalPendente, BigDecimal totalAtrasado) {}

    public record FinanceiroListagem(List<FinanceiroResponse> registros, ResumoFinanceiro resumo) {}
}
