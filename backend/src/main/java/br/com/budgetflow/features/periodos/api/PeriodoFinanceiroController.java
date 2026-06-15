package br.com.budgetflow.features.periodos.api;

import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroRequestDTO;
import br.com.budgetflow.features.periodos.dto.PeriodoFinanceiroResponseDTO;
import br.com.budgetflow.features.periodos.service.PeriodoFinanceiroService;
import jakarta.validation.Valid;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/periodos-financeiros")
public class PeriodoFinanceiroController {
	private final PeriodoFinanceiroService periodoFinanceiroService;

	public PeriodoFinanceiroController(PeriodoFinanceiroService periodoFinanceiroService) {
		this.periodoFinanceiroService = periodoFinanceiroService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('USER')")
	public ResponseEntity<PeriodoFinanceiroResponseDTO> create(@Valid @RequestBody PeriodoFinanceiroRequestDTO periodoDTO) {
		PeriodoFinanceiroResponseDTO response = periodoFinanceiroService.create(periodoDTO);
		return ResponseEntity
				.created(URI.create("/api/periodos-financeiros/" + response.id()))
				.body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('USER')")
	public ResponseEntity<Page<PeriodoFinanceiroResponseDTO>> findAll(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
			@RequestParam(required = false, name = "q") String search,
			Pageable pageable
	) {
		Page<PeriodoFinanceiroResponseDTO> periodos = periodoFinanceiroService.findAll(dataInicio, dataFim, search, pageable);
		return ResponseEntity.ok(periodos);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('USER')")
	public ResponseEntity<PeriodoFinanceiroResponseDTO> findById(@PathVariable Long id) {
		PeriodoFinanceiroResponseDTO periodo = periodoFinanceiroService.findById(id);

		return ResponseEntity.ok(periodo);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('USER')")
	public ResponseEntity<PeriodoFinanceiroResponseDTO> update(
			@PathVariable Long id,
			@Valid @RequestBody PeriodoFinanceiroRequestDTO periodoDTO
	) {
		PeriodoFinanceiroResponseDTO updatedPeriodo = periodoFinanceiroService.update(id, periodoDTO);
		return ResponseEntity.ok(updatedPeriodo);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('USER')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		periodoFinanceiroService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
