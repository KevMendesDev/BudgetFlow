package br.com.budgetflow.features.movimentacoes.service;

import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRepository;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        Long userId = periodo.getUser().getId();
        List<TransacaoRecorrente> recorrentes = recorrenteRepository.findAllByUserId(userId);
        List<Transacao> geradas = new ArrayList<>();

        for (TransacaoRecorrente recorrente : recorrentes) {
            List<LocalDate> datas = calcularDatas(recorrente, periodo);
            for (LocalDate data : datas) {
                if (!transacaoRepository.existsByTransacaoRecorrenteIdAndDataAndUserId(recorrente.getId(), data, userId)) {
                    Transacao transacao = new Transacao();
                    transacao.setUser(recorrente.getUser());
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

        long indiceInicial = 0;
        long parcelasJaGeradas = transacaoRepository.countByTransacaoRecorrenteId(recorrente.getId());
        if (parcelasJaGeradas > 0) {
            indiceInicial = parcelasJaGeradas;
        }

        Integer totalParcelas = recorrente.getTotalParcelas();
        long limiteOcorrencias = totalParcelas == null ? Long.MAX_VALUE : totalParcelas;

        for (long i = indiceInicial; i < limiteOcorrencias; i++) {
            LocalDate dataOcorrencia = RecorrenciaUtils.calcularDataOcorrencia(recorrenteInicio, recorrente.getFrequencia(), i);

            if (recorrenteFim != null && dataOcorrencia.isAfter(recorrenteFim)) {
                break;
            }

            if (dataOcorrencia.isAfter(periodoFim)) {
                break;
            }

            if (!dataOcorrencia.isBefore(periodoInicio) && !dataOcorrencia.isBefore(recorrenteInicio)) {
                datas.add(dataOcorrencia);
            }
        }

        return datas;
    }
}
