package br.com.budgetflow.features.planejamentos.service;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;

import br.com.budgetflow.common.enums.StatusRecorrencia;
import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.service.FinalizacaoRecorrenciaService;
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
import br.com.budgetflow.features.periodos.domain.PeriodoFinanceiro;
import br.com.budgetflow.features.planejamentos.repository.PlanejamentoRepository;
import br.com.budgetflow.features.planejamentos.service.support.PlanejamentoChaveSincronizacaoUtils;

@Service
public class PlanejamentoRecorrenteVinculoService {

    private final TransacaoRecorrenteRepository recorrenteRepository;
    private final PlanejamentoRepository planejamentoRepository;
    private final FinalizacaoRecorrenciaService finalizacaoRecorrenciaService;

    public PlanejamentoRecorrenteVinculoService(
            TransacaoRecorrenteRepository recorrenteRepository,
            PlanejamentoRepository planejamentoRepository,
            FinalizacaoRecorrenciaService finalizacaoRecorrenciaService
    ) {
        this.recorrenteRepository = recorrenteRepository;
        this.planejamentoRepository = planejamentoRepository;
        this.finalizacaoRecorrenciaService = finalizacaoRecorrenciaService;
    }

    public String resolverChaveSincronizacao(Long recorrenteId, PeriodoFinanceiro periodo, Long userId) {
        finalizacaoRecorrenciaService.finalizarExpiradas(userId);

        TransacaoRecorrente recorrente = recorrenteRepository.findByIdAndUserId(recorrenteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação recorrente não encontrada"));

        if (recorrente.getStatus() != StatusRecorrencia.ATIVA) {
            throw new BusinessRuleException("Apenas recorrências ativas podem ser adicionadas ao planejamento");
        }

        Set<String> chavesExistentes = planejamentoRepository.findChavesSincronizacaoByPeriodoIdAndUserId(
                periodo.getId(),
                userId
        );
        LocalDate dataOcorrencia = findFirstAvailableOccurrence(recorrente, periodo, userId, chavesExistentes);
        if (dataOcorrencia == null) {
            throw new BusinessRuleException(
                    "Todas as ocorrências desta recorrência já foram lançadas neste período"
            );
        }

        return PlanejamentoChaveSincronizacaoUtils.build(userId, recorrente.getId(), dataOcorrencia);
    }

    private LocalDate findFirstAvailableOccurrence(
            TransacaoRecorrente recorrente,
            PeriodoFinanceiro periodo,
            Long userId,
            Set<String> chavesExistentes
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
                if (!chavesExistentes.contains(chave)) {
                    return data;
                }
            }
            indice++;
        }
        return null;
    }
}
