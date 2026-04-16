package br.com.budgetflow.features.movimentacoes.service.support;

import br.com.budgetflow.common.enums.Frequencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

public final class RecorrenciaUtils {

    private RecorrenciaUtils() {
    }

    public static LocalDate calcularDataFim(LocalDate dataInicio, Frequencia frequencia, Integer totalParcelas) {
        if (totalParcelas == null) {
            return null;
        }
        if (totalParcelas < 1) {
            throw new IllegalArgumentException("O total de parcelas deve ser maior que zero");
        }
        return calcularDataOcorrencia(dataInicio, frequencia, totalParcelas - 1);
    }

    public static LocalDate calcularDataOcorrencia(LocalDate dataInicio, Frequencia frequencia, long indiceOcorrencia) {
        if (indiceOcorrencia < 0) {
            throw new IllegalArgumentException("O índice da ocorrência não pode ser negativo");
        }

        return switch (frequencia) {
            case DIARIO -> dataInicio.plusDays(indiceOcorrencia);
            case SEMANAL -> dataInicio.plusWeeks(indiceOcorrencia);
            case MENSAL -> adicionarMesesComAjusteFimMes(dataInicio, indiceOcorrencia);
            case ANUAL -> adicionarAnosComAjusteBissexto(dataInicio, indiceOcorrencia);
        };
    }

    public static BigDecimal calcularValorTotal(BigDecimal valorParcela, Integer totalParcelas) {
        if (valorParcela == null || totalParcelas == null) {
            return null;
        }
        return valorParcela.multiply(BigDecimal.valueOf(totalParcelas)).setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate adicionarMesesComAjusteFimMes(LocalDate dataBase, long meses) {
        LocalDate dataAlvo = dataBase.plusMonths(meses);
        int diaOriginal = dataBase.getDayOfMonth();
        int ultimoDiaDoMesAlvo = YearMonth.from(dataAlvo).lengthOfMonth();
        return dataAlvo.withDayOfMonth(Math.min(diaOriginal, ultimoDiaDoMesAlvo));
    }

    private static LocalDate adicionarAnosComAjusteBissexto(LocalDate dataBase, long anos) {
        int anoAlvo = Math.toIntExact(dataBase.getYear() + anos);
        int mes = dataBase.getMonthValue();
        int dia = dataBase.getDayOfMonth();
        int ultimoDiaNoMesAlvo = YearMonth.of(anoAlvo, mes).lengthOfMonth();
        return LocalDate.of(anoAlvo, mes, Math.min(dia, ultimoDiaNoMesAlvo));
    }
}
