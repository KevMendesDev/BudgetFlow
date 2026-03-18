package br.com.budgetflow.features.categorias.service;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.features.categorias.domain.Categoria;
import br.com.budgetflow.features.categorias.dto.CategoriaRequestDTO;
import br.com.budgetflow.features.categorias.dto.CategoriaResponseDTO;
import br.com.budgetflow.features.categorias.mapper.CategoriaMapper;
import br.com.budgetflow.features.categorias.repository.CategoriaRepository;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.service.UserService;
import br.com.budgetflow.security.SecurityUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

	private final CategoriaRepository categoriaRepository;
	private final UserService userService;
	private final CategoriaMapper categoriaMapper;

	public CategoriaService(CategoriaRepository categoriaRepository, UserService userService, CategoriaMapper categoriaMapper) {
		this.categoriaRepository = categoriaRepository;
		this.userService = userService;
		this.categoriaMapper = categoriaMapper;
	}

	@Transactional
	public CategoriaResponseDTO create(CategoriaRequestDTO categoriaDTO) {
		Long userId = SecurityUtils.currentUserId();
		User user = this.userService.findById(userId);

		String nome = categoriaDTO.nome().trim();
		this.validateDuplicate(nome, user.getId(), categoriaDTO.classificacao(), null);

		Categoria categoria = categoriaMapper.toEntity(categoriaDTO);
		categoria.setNome(nome);
		categoria.setUser(user);

		return categoriaMapper.toResponseDTO(categoriaRepository.save(categoria));
	}

	@Transactional(readOnly = true)
	public Page<CategoriaResponseDTO> findAll(Pageable pageable) {
        Long userId = SecurityUtils.currentUserId();

		return categoriaRepository.findAllByUserId(userId, pageable)
				.map(categoriaMapper::toResponseDTO);
	}

	@Transactional(readOnly = true)
	public CategoriaResponseDTO findById(Long id) {
		Categoria categoria = categoriaRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
		return categoriaMapper.toResponseDTO(categoria);
	}

	@Transactional
	public CategoriaResponseDTO update(Long id, CategoriaRequestDTO categoriaDTO) {
		Categoria categoria = categoriaRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

		Long userId = SecurityUtils.currentUserId();
		User user = this.userService.findById(userId);

		String nome = categoriaDTO.nome().trim();

		validateDuplicate(nome, user.getId(), categoriaDTO.classificacao(), id);

		categoriaMapper.updateCategoriaFromDto(categoriaDTO, categoria);
		categoria.setNome(nome);
		categoria.setUser(user);

		return categoriaMapper.toResponseDTO(categoriaRepository.save(categoria));
	}

	@Transactional
	public void delete(Long id) {
		if (!categoriaRepository.existsById(id)) {
			throw new IllegalArgumentException("Categoria não encontrada");
		}
		categoriaRepository.deleteById(id);
	}

	private void validateDuplicate(String nome, Long userId, ClassificacaoCategoria classificacao, Long categoriaId) {
        String normalizedName = nome.toLowerCase();
		boolean exists = categoriaId == null
				? categoriaRepository.existsByNomeIgnoreCaseAndUserIdAndClassificacao(normalizedName, userId, classificacao)
				: categoriaRepository.existsByNomeIgnoreCaseAndUserIdAndClassificacaoAndIdNot(normalizedName, userId, classificacao, categoriaId);

		if (exists) {
			throw new IllegalArgumentException("Já existe uma categoria com este nome para este usuário e classificação");
		}
	}
}
