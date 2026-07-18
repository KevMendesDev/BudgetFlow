package br.com.budgetflow.features.movimentacoes.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.enums.StatusRecorrencia;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;

@Service
public class FinalizacaoRecorrenciaService {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    public FinalizacaoRecorrenciaService(TransacaoRecorrenteRepository transacaoRecorrenteRepository) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
    }

    @Transactional
    public void finalizarExpiradas(Long userId) {
        List<TransacaoRecorrente> expiradas = transacaoRecorrenteRepository.findExpiradasNaoFinalizadas(
                userId,
                LocalDate.now(),
                StatusRecorrencia.FINALIZADA
        );
        for (TransacaoRecorrente recorrente : expiradas) {
            recorrente.setStatus(StatusRecorrencia.FINALIZADA);
        }
        if (!expiradas.isEmpty()) {
            transacaoRecorrenteRepository.saveAll(expiradas);
        }
    }
}
