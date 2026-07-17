package br.com.budgetflow.features.movimentacoes.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.enums.StatusRecorrencia;
import br.com.budgetflow.common.enums.StatusTransacao;
import br.com.budgetflow.features.movimentacoes.domain.Transacao;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.dto.SincronizacaoRecorrentesResponseDTO;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRepository;
import br.com.budgetflow.features.movimentacoes.repository.projection.TransacaoRecorrenteUsageProjection;
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;

@Service
public class GeracaoTransacoesDoPeriodoService {

    private final TransacaoRecorrenteRepository recorrenteRepository;
    private final TransacaoRepository transacaoRepository;
    private final FinalizacaoRecorrenciaService finalizacaoRecorrenciaService;

    public GeracaoTransacoesDoPeriodoService(
            TransacaoRecorrenteRepository recorrenteRepository,
            TransacaoRepository transacaoRepository,
            FinalizacaoRecorrenciaService finalizacaoRecorrenciaService) {
        this.recorrenteRepository = recorrenteRepository;
        this.transacaoRepository = transacaoRepository;
        this.finalizacaoRecorrenciaService = finalizacaoRecorrenciaService;
    }

    @Transactional
    public SincronizacaoRecorrentesResponseDTO gerarParaPeriodo(PeriodoFinanceiro periodo) {
        Long userId = periodo.getUser().getId();
        finalizacaoRecorrenciaService.finalizarExpiradas(userId);
        List<TransacaoRecorrenteUsageProjection> recorrentes = recorrenteRepository.findUsageByUserIdAndStatus(
                userId,
                StatusRecorrencia.ATIVA
        );
        List<Transacao> transacoesPendentes = new ArrayList<>();
        List<String> recorrentesPendentes = new ArrayList<>();
        int ignoradasSemValor = 0;

        for (TransacaoRecorrenteUsageProjection usage : recorrentes) {
            TransacaoRecorrente recorrente = usage.getRecorrente();
            if (recorrente.getValor() == null) {
                ignoradasSemValor++;
                continue;
            }

            ResultadoCalculo resultado = calcularDatas(recorrente, periodo, usage);
            if (resultado.pendenteEmPeriodoAnterior()) {
                recorrentesPendentes.add(recorrente.getDescricao());
                continue;
            }

            for (LocalDate data : resultado.datas()) {
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
                transacao.setStatus(StatusTransacao.PENDENTE);
                transacoesPendentes.add(transacao);
            }
        }

        List<Transacao> transacoesGeradas = transacaoRepository.saveAll(transacoesPendentes);
        String mensagem = "Sincronização concluída: "
                + transacoesGeradas.size() + " transações geradas, "
                + ignoradasSemValor + " recorrentes sem valor e "
                + recorrentesPendentes.size() + " pendentes em períodos anteriores.";

        return new SincronizacaoRecorrentesResponseDTO(
                transacoesGeradas.size(),
                ignoradasSemValor,
                recorrentesPendentes,
                mensagem
        );
    }

    private ResultadoCalculo calcularDatas(
            TransacaoRecorrente recorrente,
            PeriodoFinanceiro periodo,
            TransacaoRecorrenteUsageProjection usage
    ) {
        LocalDate proximaEsperada = usage.getUltimaData() == null
                ? recorrente.getDataInicio()
                : RecorrenciaUtils.calcularProximaDataMinima(usage.getUltimaData(), recorrente.getFrequencia());

        if (proximaEsperada.isBefore(periodo.getDataInicio())) {
            return new ResultadoCalculo(List.of(), true);
        }

        if (proximaEsperada.isAfter(periodo.getDataFim())) {
            return new ResultadoCalculo(List.of(), false);
        }

        List<LocalDate> datas = new ArrayList<>();
        LocalDate dataAtual = proximaEsperada;
        Integer totalParcelas = recorrente.getTotalParcelas();
        long parcelasLancadas = usage.getParcelasLancadas();

        while (!dataAtual.isAfter(periodo.getDataFim())) {
            if (recorrente.getDataFim() != null && dataAtual.isAfter(recorrente.getDataFim())) {
                break;
            }

            if (totalParcelas != null && parcelasLancadas + datas.size() >= totalParcelas) {
                break;
            }

            if (!dataAtual.isBefore(periodo.getDataInicio())) {
                datas.add(dataAtual);
            }

            dataAtual = RecorrenciaUtils.calcularProximaDataMinima(dataAtual, recorrente.getFrequencia());
        }

        return new ResultadoCalculo(datas, false);
    }

    private record ResultadoCalculo(List<LocalDate> datas, boolean pendenteEmPeriodoAnterior) {
    }
}
