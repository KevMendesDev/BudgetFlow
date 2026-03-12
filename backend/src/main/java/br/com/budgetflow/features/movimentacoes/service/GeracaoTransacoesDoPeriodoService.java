package br.com.budgetflow.features.movimentacoes.service;

import br.com.budgetflow.common.enums.Frequencia;
import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRepository;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeracaoTransacoesDoPeriodoService {

    private final TransacaoRecorrenteRepository recorrenteRepository;
    private final TransacaoRepository transacaoRepository;

    public GeracaoTransacoesDoPeriodoService(
            TransacaoRecorrenteRepository recorrenteRepository,
            TransacaoRepository transacaoRepository) {
        this.recorrenteRepository = recorrenteRepository;
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional
    public List<Transacao> gerarParaPeriodo(PeriodoFinanceiro periodo) {
        List<TransacaoRecorrente> recorrentes = recorrenteRepository.findAll();
        List<Transacao> geradas = new ArrayList<>();

        for (TransacaoRecorrente recorrente : recorrentes) {
            List<LocalDate> datas = calcularDatas(recorrente, periodo);
            for (LocalDate data : datas) {
                if (!transacaoRepository.existsByTransacaoRecorrenteIdAndData(recorrente.getId(), data)) {
                    Transacao transacao = new Transacao();
                    transacao.setUsuario(recorrente.getUsuario());
                    transacao.setCategoria(recorrente.getCategoria());
                    transacao.setPeriodo(periodo);
                    transacao.setTransacaoRecorrente(recorrente);
                    transacao.setDescricao(recorrente.getDescricao());
                    transacao.setValor(recorrente.getValor());
                    transacao.setTipoMovimentacao(recorrente.getTipoMovimentacao());
                    transacao.setTipoPagamento(recorrente.getTipoPagamento());
                    transacao.setData(data);
                    geradas.add(transacaoRepository.save(transacao));
                }
            }
        }
        return geradas;
    }

    private List<LocalDate> calcularDatas(TransacaoRecorrente recorrente, PeriodoFinanceiro periodo) {
        List<LocalDate> datas = new ArrayList<>();
        LocalDate periodoInicio = periodo.getDataInicio();
        LocalDate periodoFim = periodo.getDataFim();
        LocalDate recorrenteInicio = recorrente.getDataInicio();
        LocalDate recorrenteFim = recorrente.getDataFim();

        Frequencia freq = recorrente.getFrequencia();

        switch (freq) {
            case DIARIO -> {
                // Start from the later of periodoInicio and recorrenteInicio to avoid unnecessary iteration
                LocalDate current = recorrenteInicio.isAfter(periodoInicio) ? recorrenteInicio : periodoInicio;
                int parcelas = 0;
                while (!current.isAfter(periodoFim)) {
                    if (dentroDaJanela(current, recorrenteInicio, recorrenteFim)
                            && dentroDoLimiteParcelas(recorrente, parcelas)) {
                        datas.add(current);
                        parcelas++;
                    }
                    current = current.plusDays(1);
                }
            }
            case SEMANAL -> {
                // Align weekly cadence with recorrenteInicio's day of week;
                // find the first occurrence on or after periodoInicio
                LocalDate firstOccurrence = recorrenteInicio;
                if (firstOccurrence.isBefore(periodoInicio)) {
                    long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(firstOccurrence, periodoInicio);
                    long weeksToAdd = (daysDiff + 6) / 7;
                    firstOccurrence = recorrenteInicio.plusWeeks(weeksToAdd);
                }
                LocalDate current = firstOccurrence;
                int parcelas = 0;
                while (!current.isAfter(periodoFim)) {
                    if (dentroDaJanela(current, recorrenteInicio, recorrenteFim)
                            && dentroDoLimiteParcelas(recorrente, parcelas)) {
                        datas.add(current);
                        parcelas++;
                    }
                    current = current.plusWeeks(1);
                }
            }
            case MENSAL -> {
                LocalDate firstDay = periodoInicio;
                if (dentroDaJanela(firstDay, recorrenteInicio, recorrenteFim)
                        && dentroDoLimiteParcelas(recorrente, 0)) {
                    datas.add(firstDay);
                }
            }
            case ANUAL -> {
                MonthDay recorrenteMonthDay = MonthDay.of(recorrenteInicio.getMonth(), recorrenteInicio.getDayOfMonth());
                int ano = periodoInicio.getYear();
                LocalDate candidata;
                try {
                    candidata = recorrenteMonthDay.atYear(ano);
                } catch (Exception e) {
                    // Feb 29 in non-leap year -> Feb 28
                    candidata = LocalDate.of(ano, recorrenteInicio.getMonth(), 28);
                }
                if (!candidata.isBefore(periodoInicio) && !candidata.isAfter(periodoFim)
                        && dentroDaJanela(candidata, recorrenteInicio, recorrenteFim)
                        && dentroDoLimiteParcelas(recorrente, 0)) {
                    datas.add(candidata);
                }
            }
        }
        return datas;
    }

    private boolean dentroDaJanela(LocalDate data, LocalDate inicio, LocalDate fim) {
        if (data.isBefore(inicio)) return false;
        if (fim != null && data.isAfter(fim)) return false;
        return true;
    }

    private boolean dentroDoLimiteParcelas(TransacaoRecorrente recorrente, int parcelasJaGeradas) {
        if (recorrente.getTotalParcelas() == null) return true;
        return parcelasJaGeradas < recorrente.getTotalParcelas();
    }
}
