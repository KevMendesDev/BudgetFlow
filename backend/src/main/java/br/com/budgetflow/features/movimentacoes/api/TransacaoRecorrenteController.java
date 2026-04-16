package br.com.budgetflow.features.movimentacoes.api;

import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteRequestDTO;
import br.com.budgetflow.features.movimentacoes.criteria.TransacaoRecorrenteFilterCriteria;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRecorrenteResponseDTO;
import br.com.budgetflow.features.movimentacoes.service.TransacaoRecorrenteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/transacoes-recorrentes")
public class TransacaoRecorrenteController {

    private final TransacaoRecorrenteService transacaoRecorrenteService;

    public TransacaoRecorrenteController(TransacaoRecorrenteService transacaoRecorrenteService) {
        this.transacaoRecorrenteService = transacaoRecorrenteService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<TransacaoRecorrenteResponseDTO> create(@Valid @RequestBody TransacaoRecorrenteRequestDTO requestDTO) {
        TransacaoRecorrenteResponseDTO responseDTO = transacaoRecorrenteService.create(requestDTO);
        return ResponseEntity
                .created(URI.create("/api/transacoes-recorrentes/" + responseDTO.id()))
                .body(responseDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Page<TransacaoRecorrenteResponseDTO>> findAll(
            @Valid TransacaoRecorrenteFilterCriteria criteria,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TransacaoRecorrenteResponseDTO> transacoesRecorrentes = transacaoRecorrenteService.findAll(criteria, pageable);
        return ResponseEntity.ok(transacoesRecorrentes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<TransacaoRecorrenteResponseDTO> findById(@PathVariable Long id) {
        TransacaoRecorrenteResponseDTO transacaoRecorrente = transacaoRecorrenteService.findById(id);
        return ResponseEntity.ok(transacaoRecorrente);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<TransacaoRecorrenteResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TransacaoRecorrenteRequestDTO requestDTO
    ) {
        TransacaoRecorrenteResponseDTO updatedTransacaoRecorrente = transacaoRecorrenteService.update(id, requestDTO);
        return ResponseEntity.ok(updatedTransacaoRecorrente);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transacaoRecorrenteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
