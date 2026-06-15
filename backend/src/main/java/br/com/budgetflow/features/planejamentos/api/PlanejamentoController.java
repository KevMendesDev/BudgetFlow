package br.com.budgetflow.features.planejamentos.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.budgetflow.features.planejamentos.dto.PlanejamentoRequestDTO;
import br.com.budgetflow.features.planejamentos.dto.PlanejamentoResponseDTO;
import br.com.budgetflow.features.planejamentos.dto.SincronizacaoPlanejamentosResponseDTO;
import br.com.budgetflow.features.planejamentos.service.PlanejamentoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/planejamentos")
@PreAuthorize("hasAnyRole('USER')")
public class PlanejamentoController {

    private final PlanejamentoService planejamentoService;

    public PlanejamentoController(PlanejamentoService planejamentoService) {
        this.planejamentoService = planejamentoService;
    }

    @GetMapping
    public ResponseEntity<List<PlanejamentoResponseDTO>> findAll(@RequestParam Long periodoId) {
        return ResponseEntity.ok(planejamentoService.findAll(periodoId));
    }

    @PostMapping
    public ResponseEntity<PlanejamentoResponseDTO> create(@Valid @RequestBody PlanejamentoRequestDTO request) {
        PlanejamentoResponseDTO response = planejamentoService.create(request);
        return ResponseEntity.created(URI.create("/api/planejamentos/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanejamentoResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody PlanejamentoRequestDTO request
    ) {
        return ResponseEntity.ok(planejamentoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planejamentoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sincronizar-recorrentes")
    public ResponseEntity<SincronizacaoPlanejamentosResponseDTO> sincronizarRecorrentes(
            @RequestParam Long periodoId
    ) {
        return ResponseEntity.ok(planejamentoService.sincronizarRecorrentes(periodoId));
    }
}
