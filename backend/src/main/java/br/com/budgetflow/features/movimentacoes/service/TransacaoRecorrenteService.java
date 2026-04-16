package br.com.budgetflow.features.movimentacoes.service;

import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.service.CategoriaService;
import br.com.budgetflow.features.movimentacoes.criteria.TransacaoRecorrenteFilterCriteria;
import br.com.budgetflow.features.movimentacoes.domain.TransacaoRecorrente;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteRequestDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteResponseDTO;
import br.com.budgetflow.features.movimentacoes.mapper.TransacaoRecorrenteMapper;
import br.com.budgetflow.features.movimentacoes.repository.TransacaoRecorrenteRepository;
import br.com.budgetflow.features.movimentacoes.repository.specification.TransacaoRecorrenteSpecification;
import br.com.budgetflow.features.movimentacoes.service.support.RecorrenciaUtils;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TransacaoRecorrenteService {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final UserService userService;
    private final TransacaoRecorrenteMapper transacaoRecorrenteMapper;
    private final CategoriaService categoriaService;

    public TransacaoRecorrenteService(
            TransacaoRecorrenteRepository transacaoRecorrenteRepository,
            UserService userService,
            TransacaoRecorrenteMapper transacaoRecorrenteMapper,
            CategoriaService categoriaService
    ) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
        this.userService = userService;
        this.transacaoRecorrenteMapper = transacaoRecorrenteMapper;
        this.categoriaService = categoriaService;
    }

    @Transactional
    public TransacaoRecorrenteResponseDTO create(TransacaoRecorrenteRequestDTO requestDTO) {
        Long userId = SecurityUtils.currentUserId();
        User user = userService.findById(userId);
        Categoria categoria = categoriaService.findById(requestDTO.categoriaId());

        validateDateRange(requestDTO.dataInicio(), requestDTO.dataFim());

        TransacaoRecorrente transacaoRecorrente = transacaoRecorrenteMapper.toEntity(requestDTO);
        transacaoRecorrente.setUser(user);
        transacaoRecorrente.setCategoria(categoria);
        transacaoRecorrente.setDescricao(requestDTO.descricao().trim());
        transacaoRecorrente.setDataFim(resolveDataFim(requestDTO));

        return transacaoRecorrenteMapper.toResponseDTO(transacaoRecorrenteRepository.save(transacaoRecorrente));
    }

    @Transactional(readOnly = true)
    public Page<TransacaoRecorrenteResponseDTO> findAll(
            TransacaoRecorrenteFilterCriteria criteria,
            Pageable pageable
    ) {
        validateDateRange(criteria.getDataInicio(), criteria.getDataFim());

        Long userId = SecurityUtils.currentUserId();

        Specification<TransacaoRecorrente> specification = TransacaoRecorrenteSpecification
            .createSpecification(criteria, userId, null);

        return transacaoRecorrenteRepository.findAll(specification, pageable)
                .map(transacaoRecorrenteMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public TransacaoRecorrenteResponseDTO findById(Long id) {
        Long userId = SecurityUtils.currentUserId();
        TransacaoRecorrente transacaoRecorrente = findByIdAndUserId(id, userId);
        return transacaoRecorrenteMapper.toResponseDTO(transacaoRecorrente);
    }

    @Transactional
    public TransacaoRecorrenteResponseDTO update(Long id, TransacaoRecorrenteRequestDTO requestDTO) {
        Long userId = SecurityUtils.currentUserId();
        TransacaoRecorrente transacaoRecorrente = findByIdAndUserId(id, userId);
        Categoria categoria = categoriaService.findById(requestDTO.categoriaId());

        validateDateRange(requestDTO.dataInicio(), requestDTO.dataFim());

        transacaoRecorrenteMapper.updateFromDto(requestDTO, transacaoRecorrente);
        transacaoRecorrente.setCategoria(categoria);
        transacaoRecorrente.setDescricao(requestDTO.descricao().trim());
        transacaoRecorrente.setDataFim(resolveDataFim(requestDTO));

        return transacaoRecorrenteMapper.toResponseDTO(transacaoRecorrenteRepository.save(transacaoRecorrente));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityUtils.currentUserId();
        TransacaoRecorrente transacaoRecorrente = findByIdAndUserId(id, userId);
        transacaoRecorrenteRepository.delete(transacaoRecorrente);
    }

    @Transactional(readOnly = true)
    public TransacaoRecorrente findEntityByIdAndUser(Long id, Long userId) {
        return findByIdAndUserId(id, userId);
    }

    private LocalDate resolveDataFim(TransacaoRecorrenteRequestDTO requestDTO) {
        if (requestDTO.totalParcelas() != null) {
            return RecorrenciaUtils.calcularDataFim(requestDTO.dataInicio(), requestDTO.frequencia(), requestDTO.totalParcelas());
        }
        return requestDTO.dataFim();
    }

    private TransacaoRecorrente findByIdAndUserId(Long id, Long userId) {
        return transacaoRecorrenteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Transação recorrente não encontrada"));
    }

    private void validateDateRange(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início");
        }
    }
}
