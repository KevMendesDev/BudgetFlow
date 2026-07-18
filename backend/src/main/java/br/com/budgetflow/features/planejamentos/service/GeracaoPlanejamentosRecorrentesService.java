package br.com.budgetflow.features.planejamentos.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.enums.StatusRecorrencia;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.planejamentos.domain.Planejamento;
import br.com.budgetflow.features.planejamentos.dto.SincronizacaoPlanejamentosResponseDTO;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;
import br.com.budgetflow.features.planejamentos.service.support.PlanejamentoChaveSincronizacaoUtils;

@Service
public class GeracaoPlanejamentosRecorrentesService {

    private final TransacaoRecorrenteRepository recorrenteRepository;
    private final PlanejamentoRepository planejamentoRepository;

    public GeracaoPlanejamentosRecorrentesService(
            TransacaoRecorrenteRepository recorrenteRepository,
            PlanejamentoRepository planejamentoRepository
    ) {
        this.recorrenteRepository = recorrenteRepository;
        this.planejamentoRepository = planejamentoRepository;
    }

    @Transactional
    public SincronizacaoPlanejamentosResponseDTO sincronizar(PeriodoFinanceiro periodo, Long userId) {
        List<Planejamento> novos = new ArrayList<>();
        Set<String> chavesSincronizadas = new HashSet<>(
                planejamentoRepository.findChavesSincronizacaoByPeriodoIdAndUserId(periodo.getId(), userId)
        );
        int semValor = 0;

        for (TransacaoRecorrente recorrente : recorrenteRepository.findAllByUserIdAndStatus(
                userId,
                StatusRecorrencia.ATIVA
        )) {
            if (recorrente.getValor() == null) {
                semValor++;
                continue;
            }
            gerarOcorrenciasDoPeriodo(recorrente, periodo, userId, chavesSincronizadas, novos);
        }

        planejamentoRepository.saveAll(novos);
        String mensagem = "Sincronização concluída: " + novos.size()
                + " planejamentos gerados e " + semValor + " recorrências sem valor.";
        return new SincronizacaoPlanejamentosResponseDTO(novos.size(), semValor, mensagem);
    }

    private void gerarOcorrenciasDoPeriodo(
            TransacaoRecorrente recorrente,
            PeriodoFinanceiro periodo,
            Long userId,
            Set<String> chavesSincronizadas,
            List<Planejamento> novos
    ) {
        long indice = 0;
        while (recorrente.getTotalParcelas() == null || indice < recorrente.getTotalParcelas()) {
            LocalDate data = RecorrenciaUtils.calcularDataOcorrencia(
                    recorrente.getDataInicio(),
                    recorrente.getFrequencia(),
                    indice
            );

            if (recorrente.getDataFim() != null && data.isAfter(recorrente.getDataFim())) {
                break;
            }
            if (data.isAfter(periodo.getDataFim())) {
                break;
            }

            if (!data.isBefore(periodo.getDataInicio())) {
                String chave = PlanejamentoChaveSincronizacaoUtils.build(userId, recorrente.getId(), data);
                if (chavesSincronizadas.add(chave)) {
                    Planejamento planejamento = new Planejamento();
                    planejamento.setUser(recorrente.getUser());
                    planejamento.setPeriodo(periodo);
                    planejamento.setCategoria(recorrente.getCategoria());
                    planejamento.setDescricao(recorrente.getDescricao());
                    planejamento.setValor(recorrente.getValor());
                    planejamento.setTipoMovimentacao(recorrente.getTipoMovimentacao());
                    planejamento.setChaveSincronizacao(chave);
                    novos.add(planejamento);
                }
            }
            indice++;
        }
    }
}
