package br.com.budgetflow.features.categorias.service;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.common.exceptions.ConflictException;
import br.com.budgetflow.common.exceptions.EntityHasRelationshipsException;
import br.com.budgetflow.common.exceptions.ResourceNotFoundException;
import br.com.budgetflow.common.service.RelacionamentoChecker;
import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.dto.CategoriaRequestDTO;
import br.com.budgetflow.features.categorias.dto.CategoriaResponseDTO;
import br.com.budgetflow.features.categorias.mapper.CategoriaMapper;
import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.categorias.repository.specification.CategoriaSpecification;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

	private final CategoriaRepository categoriaRepository;
	private final UserService userService;
	private final CategoriaMapper categoriaMapper;
	private final RelacionamentoChecker relacionamentoChecker;

	public CategoriaService(CategoriaRepository categoriaRepository, UserService userService, CategoriaMapper categoriaMapper, RelacionamentoChecker relacionamentoChecker) {
		this.categoriaRepository = categoriaRepository;
		this.userService = userService;
		this.categoriaMapper = categoriaMapper;
		this.relacionamentoChecker = relacionamentoChecker;
	}

	@Transactional
	public CategoriaResponseDTO create(CategoriaRequestDTO categoriaDTO) {
		Long userId = SecurityUtils.currentUserId();
		User user = this.userService.findById(userId);

		String nome = categoriaDTO.nome().trim();
		ClassificacaoCategoria classificacao = normalizeClassificacao(categoriaDTO);
		this.validateDuplicate(nome, user.getId(), categoriaDTO.tipoCategoria(), classificacao, null);

		Categoria categoria = categoriaMapper.toEntity(categoriaDTO);
		categoria.setNome(nome);
		categoria.setClassificacao(classificacao);
		categoria.setUser(user);

		return categoriaMapper.toResponseDTO(categoriaRepository.save(categoria));
	}

	@Transactional(readOnly = true)
	public Page<CategoriaResponseDTO> findAll(
			ClassificacaoCategoria classificacao,
			NaturezaFinanceira tipoCategoria,
			String nomeUsuario,
			String search,
			Pageable pageable
	) {
		Long userId = SecurityUtils.currentUserId();
		Specification<Categoria> specification = Specification
				.where(CategoriaSpecification.hasUserId(userId))
				.and(CategoriaSpecification.hasClassificacao(classificacao))
				.and(CategoriaSpecification.hasTipoCategoria(tipoCategoria))
				.and(CategoriaSpecification.hasNomeUsuario(nomeUsuario))
				.and(CategoriaSpecification.hasSearchTerm(search));

		return categoriaRepository.findAll(specification, pageable)
				.map(categoriaMapper::toResponseDTO);
	}

	@Transactional(readOnly = true)
	public Categoria findById(Long id) {
		Long userId = SecurityUtils.currentUserId();
		return findByIdAndUser(id, userId);
	}

	@Transactional(readOnly = true)
	public Categoria findEntityByIdAndUser(Long id, Long userId) {
		return findByIdAndUser(id, userId);
	}

	@Transactional
	public CategoriaResponseDTO update(Long id, CategoriaRequestDTO categoriaDTO) {
		Long userId = SecurityUtils.currentUserId();
		Categoria categoria = findByIdAndUser(id, userId);
		User user = this.userService.findById(userId);

		String nome = categoriaDTO.nome().trim();
		ClassificacaoCategoria classificacao = normalizeClassificacao(categoriaDTO);
		validateDuplicate(nome, userId, categoriaDTO.tipoCategoria(), classificacao, id);

		categoriaMapper.updateCategoriaFromDto(categoriaDTO, categoria);
		categoria.setNome(nome);
		categoria.setClassificacao(classificacao);
		categoria.setUser(user);

		return categoriaMapper.toResponseDTO(categoriaRepository.save(categoria));
	}

	@Transactional
	public void delete(Long id) {
		Long userId = SecurityUtils.currentUserId();
		Categoria categoria = findByIdAndUser(id, userId);

		if (relacionamentoChecker.categoriaHasRelationships(id, userId)) {
			throw new EntityHasRelationshipsException(
					"A categoria \"" + categoria.getNome() + "\" não pode ser excluída pois está vinculada a uma ou mais transações"
			);
		}

		categoriaRepository.delete(categoria);
	}

	private Categoria findByIdAndUser(Long id, Long userId) {
		return categoriaRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
	}

	private ClassificacaoCategoria normalizeClassificacao(CategoriaRequestDTO categoriaDTO) {
		if (categoriaDTO.tipoCategoria() == NaturezaFinanceira.RECEITA) {
			if (categoriaDTO.classificacao() != null) {
				throw new BusinessRuleException("Categorias de receita não podem ter classificação");
			}
			return null;
		}

		if (categoriaDTO.classificacao() == null) {
			throw new BusinessRuleException("A classificação da categoria é obrigatória para despesas");
		}

		return categoriaDTO.classificacao();
	}

	private void validateDuplicate(
			String nome,
			Long userId,
			NaturezaFinanceira tipoCategoria,
			ClassificacaoCategoria classificacao,
			Long categoriaId
	) {
		String normalizedName = nome.toLowerCase();

		boolean exists;
		if (tipoCategoria == NaturezaFinanceira.RECEITA) {
			exists = categoriaId == null
					? categoriaRepository.existsByNomeIgnoreCaseAndUserIdAndTipoCategoria(normalizedName, userId, tipoCategoria)
					: categoriaRepository.existsByNomeIgnoreCaseAndUserIdAndTipoCategoriaAndIdNot(normalizedName, userId, tipoCategoria, categoriaId);
		} else {
			exists = categoriaId == null
					? categoriaRepository.existsByNomeIgnoreCaseAndUserIdAndTipoCategoriaAndClassificacao(normalizedName, userId, tipoCategoria, classificacao)
					: categoriaRepository.existsByNomeIgnoreCaseAndUserIdAndTipoCategoriaAndClassificacaoAndIdNot(
							normalizedName,
							userId,
							tipoCategoria,
							classificacao,
							categoriaId
					);
		}

		if (exists) {
			throw new ConflictException("Já existe uma categoria com este nome para este usuário");
		}
	}
}
